// BillingSystem.java
package de.tuberlin.cit.vs;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class BillingSystem {
    private static Session session;
    private static MessageProducer producer;

    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        cf.setTrustAllPackages(true);
        Connection con = cf.createConnection();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue inQ = session.createQueue("orders.billingIn");
        Queue outQ = session.createQueue("orders.billingOut");
        MessageConsumer cons = session.createConsumer(inQ);
        producer = session.createProducer(outQ);

        con.start();
        System.out.println("BillingSystem started, waiting for orders...");

        cons.setMessageListener(msg -> {
            try {
                Order o = (Order)((ObjectMessage)msg).getObject();
                // fail odd‐digit customerIDs
                String cid = o.getCustomerID();
                char last = cid.charAt(cid.length()-1);
                if ((last - '0') % 2 == 1) {
                    o.setValid(false);
                    o.setValidationResult("BILL_FAIL");
                }
                ObjectMessage out = session.createObjectMessage(o);
                // propagate orderId header
                String oid = msg.getStringProperty("orderId");
                if (oid != null) out.setStringProperty("orderId", oid);
                producer.send(out);
                System.out.println("Processed billing: " + o);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });

        System.in.read();
        session.close();
        con.close();
    }
}
