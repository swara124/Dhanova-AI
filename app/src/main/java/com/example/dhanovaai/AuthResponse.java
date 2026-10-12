package com.example.dhanovaai;

public class AuthResponse {
    public String message;
    public String token;
    public User user;

    public static class User {
        public int user_id;
        public String name;
        public String email;
    }
}