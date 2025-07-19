// Communication/src/de/tuberlin/cit/vs/ResultSystem.java
package de.tuberlin.cit.vs;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class ResultSystem {
    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        cf.setTrustAllPackages(true);
        Connection con = cf.createConnection();
        Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // subscribe to both valid and invalid result queues
        Queue validQ   = session.createQueue("orders.valid");
        Queue invalidQ = session.createQueue("orders.invalid");
        MessageConsumer consValid   = session.createConsumer(validQ);
        MessageConsumer consInvalid = session.createConsumer(invalidQ);

        con.start();
        System.out.println("ResultSystem> listening for valid and invalid orders...");

        MessageListener listener = msg -> {
            try {
                Order o = (Order)((ObjectMessage)msg).getObject();
                String dest = ((Queue)msg.getJMSDestination()).getQueueName();
                String tag = dest.equals("orders.valid") ? "[VALID]   " : "[INVALID] ";
                System.out.println(tag + o);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        };

        consValid.setMessageListener(listener);
        consInvalid.setMessageListener(listener);

        System.in.read();  // keep running
        session.close();
        con.close();
    }
}
