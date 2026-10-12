package com.example.dhanovaai;

public class AddTransactionRequest {
    public String scope;           // "FAMILY" or "PERSONAL"
    public String type;            // "EXPENSE" or "INCOME"
    public double amount;
    public String category;
    public String title;
    public String date;            // yyyy-MM-dd
    public String payment_method;
    public String note;
}