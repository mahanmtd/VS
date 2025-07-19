// InventorySystem.java
package de.tuberlin.cit.vs;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class InventorySystem {
    private static Session session;
    private static MessageProducer producer;
    private static int stockSurf = 10, stockDive = 5;

    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        cf.setTrustAllPackages(true);
        Connection con = cf.createConnection();
        session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue inQ = session.createQueue("orders.inventoryIn");
        Queue outQ = session.createQueue("orders.inventoryOut");
        MessageConsumer cons = session.createConsumer(inQ);
        producer = session.createProducer(outQ);

        con.start();
        System.out.println("InventorySystem> stock surf=" + stockSurf + " dive=" + stockDive);

        cons.setMessageListener(msg -> {
            try {
                Order o = (Order)((ObjectMessage)msg).getObject();
                if (o.getNumberOfSurfboards() > stockSurf || o.getNumberOfDivingSuits() > stockDive) {
                    o.setValid(false);
                    o.setValidationResult("INV_FAIL");
                } else {
                    stockSurf -= o.getNumberOfSurfboards();
                    stockDive -= o.getNumberOfDivingSuits();
                }
                ObjectMessage out = session.createObjectMessage(o);
                String oid = msg.getStringProperty("orderId");
                if (oid != null) out.setStringProperty("orderId", oid);
                producer.send(out);
                System.out.println("Processed inventory: " + o +
                    " | now surf=" + stockSurf + " dive=" + stockDive);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });

        System.in.read();
        session.close();
        con.close();
    }
}
