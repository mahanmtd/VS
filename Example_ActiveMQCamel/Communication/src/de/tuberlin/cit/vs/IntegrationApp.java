// Communication/src/de/tuberlin/cit/vs/IntegrationApp.java
package de.tuberlin.cit.vs;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class IntegrationApp {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationApp.class);

    public static void main(String[] args) throws Exception {
        // 1) Start embedded broker
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:61616");
        broker.start();

        // 2) Create a ConnectionFactory that trusts our Order class
        ActiveMQConnectionFactory cf =
            new ActiveMQConnectionFactory("tcp://localhost:61616");
        cf.setTrustAllPackages(true);

        // 3) Camel context + ActiveMQ component using our CF
        CamelContext ctx = new DefaultCamelContext();
        ActiveMQComponent amq = new ActiveMQComponent();
        amq.setConnectionFactory(cf);
        ctx.addComponent("activemq", amq);

        // 4) Define routes
        ctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                // On exception, skip the message
                onException(Exception.class)
                  .handled(true)
                  .process(exchange -> exchange.setProperty("CamelSkipMessage", true));

                // Route A: pick up CSV files, translate to Order, send to JMS
                from("file:orders?noop=true")
                  .split(body().tokenize("\n"))
                    .process(IntegrationApp::stringToOrder)
                    // allow through unless explicitly marked to skip (true)
                    .filter(ex -> !Boolean.TRUE.equals(ex.getProperty("CamelSkipMessage")))
                  .to("activemq:queue:orders.in");

                // Route B: consume JMS (String or Order), translate if needed, enrich & fan-out
                from("activemq:queue:orders.in")
                  .process(exchange -> {
                      Object body = exchange.getIn().getBody();
                      if (body instanceof String) {
                          stringToOrder(exchange);
                      }
                  })
                  .filter(ex -> ex.getIn().getBody() instanceof Order)
                  .process(IntegrationApp::assignOrderId)
                  .multicast().parallelProcessing()
                    .to("activemq:queue:orders.billingIn", "activemq:queue:orders.inventoryIn")
                  .end();

                // Route C: gather billing + inventory responses and route valid/invalid
                from("activemq:queue:orders.billingOut").to("direct:aggregate");
                from("activemq:queue:orders.inventoryOut").to("direct:aggregate");

                from("direct:aggregate")
                  .aggregate(
                    ExpressionBuilder.headerExpression("orderId"),
                    new CountingAggregation()
                  )
                  .completionSize(2)
                  .choice()
                    .when(simple("${body.valid} == true"))
                      .to("activemq:queue:orders.valid")
                    .otherwise()
                      .to("activemq:queue:orders.invalid");

                // (no in-app logging of results here)
            }
        });

        // 5) Start Camel & wait
        ctx.start();
        LOG.info("IntegrationApp started. Press <enter> to stop");
        System.in.read();
        ctx.stop();
        broker.stop();
    }

    // Updated parser to handle both 5-field web orders and 4-field call-center orders
    private static void stringToOrder(Exchange ex) {
        String line = ex.getIn().getBody(String.class);
        if (line == null || line.trim().isEmpty()) {
            LOG.warn("Skipping empty line");
            ex.setProperty("CamelSkipMessage", true);
            return;
        }
        String[] parts = line.split(",");
        try {
            Order o;
            if (parts.length == 5) {
                // Web orders: custID, first, last, dive, surf
                String cid = parts[0].trim();
                String fn  = parts[1].trim();
                String ln  = parts[2].trim();
                int dive   = Integer.parseInt(parts[3].trim());
                int surf   = Integer.parseInt(parts[4].trim());
                o = new Order(cid, fn, ln, dive, surf, "");
            } else if (parts.length == 4) {
                // Call-center: FullName, surf, dive, custID
                String[] nameParts = parts[0].trim().split(" ", 2);
                String fn = nameParts[0];
                String ln = nameParts.length > 1 ? nameParts[1] : "";
                int surf = Integer.parseInt(parts[1].trim());
                int dive = Integer.parseInt(parts[2].trim());
                String cid = parts[3].trim();
                o = new Order(cid, fn, ln, dive, surf, "");
            } else {
                LOG.warn("Skipping malformed line: {}", line);
                ex.setProperty("CamelSkipMessage", true);
                return;
            }
            ex.getIn().setBody(o);
        } catch (Exception e) {
            LOG.warn("Skipping unparsable line: {}", line, e);
            ex.setProperty("CamelSkipMessage", true);
        }
    }

    private static void assignOrderId(Exchange ex) {
        Order o = ex.getIn().getBody(Order.class);
        String id = UUID.randomUUID().toString();
        o.setOrderId(id);
        ex.getIn().setHeader("orderId", id);
    }

    static class CountingAggregation implements org.apache.camel.processor.aggregate.AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldEx, Exchange newEx) {
            if (oldEx == null) return newEx;
            Order a = oldEx.getIn().getBody(Order.class);
            Order b = newEx.getIn().getBody(Order.class);
            boolean ok = a.isValid() && b.isValid();
            a.setValid(ok);
            a.setValidationResult("COMPLETED");
            oldEx.getIn().setBody(a);
            return oldEx;
        }
    }
}
