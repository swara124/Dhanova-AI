package com.example.dhanovaai;

public class PinRequest {
    public String email;
    public String secret_code;

    public PinRequest(String email, String secret_code) {
        this.email = email;
        this.secret_code = secret_code;
    }
}