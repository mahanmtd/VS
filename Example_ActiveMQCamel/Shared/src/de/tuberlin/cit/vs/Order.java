package de.tuberlin.cit.vs;

import java.io.Serializable;
import java.util.Objects;

/**
 * Shared Order data class for Camel integration pipelines.
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private String customerID;
    private String firstName;
    private String lastName;
    private int numberOfDivingSuits;
    private int numberOfSurfboards;
    private int overallItems;
    private String orderId;
    private boolean valid;
    private String validationResult;

    public Order() {
        // default constructor for JMS and Camel marshalling
    }

    public Order(String customerID, String firstName, String lastName,
                 int numberOfDivingSuits, int numberOfSurfboards, String orderId) {
        this.customerID = customerID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.numberOfDivingSuits = numberOfDivingSuits;
        this.numberOfSurfboards = numberOfSurfboards;
        this.overallItems = numberOfDivingSuits + numberOfSurfboards;
        this.orderId = orderId;
        this.valid = true;             
        this.validationResult = "PENDING";
    }

    // Getters and setters
    public String getCustomerID() {
        return customerID;
    }
    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getNumberOfDivingSuits() {
        return numberOfDivingSuits;
    }
    public void setNumberOfDivingSuits(int numberOfDivingSuits) {
        this.numberOfDivingSuits = numberOfDivingSuits;
        recalcOverall();
    }

    public int getNumberOfSurfboards() {
        return numberOfSurfboards;
    }
    public void setNumberOfSurfboards(int numberOfSurfboards) {
        this.numberOfSurfboards = numberOfSurfboards;
        recalcOverall();
    }

    public int getOverallItems() {
        return overallItems;
    }
    private void recalcOverall() {
        this.overallItems = this.numberOfDivingSuits + this.numberOfSurfboards;
    }

    public String getOrderId() {
        return orderId;
    }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public boolean isValid() {
        return valid;
    }
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getValidationResult() {
        return validationResult;
    }
    public void setValidationResult(String validationResult) {
        this.validationResult = validationResult;
    }

    @Override
    public String toString() {
        return String.format("Order[id=%s, cust=%s, name=%s %s, dive=%d, surf=%d, total=%d, valid=%b, result=%s]",
            orderId, customerID, firstName, lastName,
            numberOfDivingSuits, numberOfSurfboards,
            overallItems, valid, validationResult);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}
