package de.tuberlin.cit.vs;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class IntegrationAppParserTest {

    private Exchange parse(String line) throws Exception {
        DefaultCamelContext ctx = new DefaultCamelContext();
        Exchange ex = new DefaultExchange(ctx);
        ex.getIn().setBody(line);
        Method m = IntegrationApp.class.getDeclaredMethod("stringToOrder", Exchange.class);
        m.setAccessible(true);
        m.invoke(null, ex);
        return ex;
    }

    @Test
    public void parsesWebOrderLine() throws Exception {
        Exchange ex = parse("c123,John,Doe,2,3");
        Order o = ex.getIn().getBody(Order.class);
        assertNotNull(o);
        assertEquals("c123", o.getCustomerID());
        assertEquals("John", o.getFirstName());
        assertEquals("Doe", o.getLastName());
        assertEquals(2, o.getNumberOfDivingSuits());
        assertEquals(3, o.getNumberOfSurfboards());
        assertNull(ex.getProperty("CamelSkipMessage"));
    }

    @Test
    public void parsesCallCenterLine() throws Exception {
        Exchange ex = parse("John Doe,3,2,c123");
        Order o = ex.getIn().getBody(Order.class);
        assertNotNull(o);
        assertEquals("c123", o.getCustomerID());
        assertEquals("John", o.getFirstName());
        assertEquals("Doe", o.getLastName());
        assertEquals(2, o.getNumberOfDivingSuits());
        assertEquals(3, o.getNumberOfSurfboards());
        assertNull(ex.getProperty("CamelSkipMessage"));
    }

    @Test
    public void rejectsMalformedLine() throws Exception {
        Exchange ex = parse("bad,line");
        assertEquals("bad,line", ex.getIn().getBody());
        assertEquals(Boolean.TRUE, ex.getProperty("CamelSkipMessage"));
    }
}
