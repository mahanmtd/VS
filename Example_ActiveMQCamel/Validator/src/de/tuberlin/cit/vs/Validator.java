package de.tuberlin.cit.vs;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.IOException;

public class Validator {
    public static void main(String[] args) {
        try {
            // Explicitly specify the broker URL to avoid relying on the
            // implementation's default value which may differ across
            // ActiveMQ versions. Without this the application might fail
            // to connect to the embedded broker started by IntegrationApp.
            ActiveMQConnectionFactory conFactory =
                    new ActiveMQConnectionFactory("tcp://localhost:61616");
            conFactory.setTrustAllPackages(true);
            Connection con = conFactory.createConnection();

            final Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue inQueue = session.createQueue("validationIn");
            Queue outQueue = session.createQueue("validationOut");
            MessageConsumer consumer = session.createConsumer(inQueue);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        Vote vote = (Vote)((ObjectMessage)message).getObject();

                        System.out.println(vote);

                        ObjectMessage answer = session.createObjectMessage(vote);
                        boolean validated = Math.random() > 0.5;
                        answer.setBooleanProperty("validated", validated);

                        MessageProducer producer = session.createProducer(outQueue);
                        producer.send(answer);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            con.start();
            System.in.read();
            con.close();
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
