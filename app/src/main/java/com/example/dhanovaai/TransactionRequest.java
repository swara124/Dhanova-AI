// TransactionRequest.java
package com.example.dhanovaai;

public class TransactionRequest {
    public String scope;       // "FAMILY" or "PERSONAL"
    public String type;        // "INCOME" or "EXPENSE"
    public double amount;
    public String currency;
    public Integer category_id;
    public String description;
    public String payment_method;
    public String transaction_date; // "YYYY-MM-DD"

    public TransactionRequest(String scope, String type, double amount, String currency,
                              String description, String transaction_date) {
        this.scope = scope;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.transaction_date = transaction_date;
    }
}