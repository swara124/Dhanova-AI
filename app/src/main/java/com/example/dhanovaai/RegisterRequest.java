package com.example.dhanovaai;

public class RegisterRequest {
    public String name;
    public String email;
    public String password;
    public String secret_code;

    public RegisterRequest(String name, String email, String password, String secret_code) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.secret_code = secret_code;
    }
}