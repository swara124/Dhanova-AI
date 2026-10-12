// Transaction.java
package com.example.dhanovaai;

public class Transaction {
    public int transaction_id;
    public String scope;
    public String type;
    public double amount;
    public String currency;
    public double base_amount;
    public String base_currency;
    public String description;
    public String transaction_date;
    public String added_by_name; // only present for FAMILY scope results
}