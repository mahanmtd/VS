package de.tuberlin.cit.vs;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WebOrderSystem {
    public static void main(String[] args) {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection con = null;
        Session session = null;
        try {
            con = cf.createConnection();
            session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            final MessageProducer prod = session.createProducer(session.createQueue("orders.in"));
            con.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("WebOrderSystem> enter orders 'cust,first,last,dive,surf' or 'quit'");
            String line;
            while ((line = in.readLine()) != null && !line.equalsIgnoreCase("quit")) {
                prod.send(session.createTextMessage(line));
                System.out.println("→ sent: " + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (session != null) session.close(); } catch(Exception e){}
            try { if (con != null) con.close(); } catch(Exception e){}
        }
    }
}
