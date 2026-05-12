package com.plasmit.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String password = "Admin@123";

        String hash = encoder.encode(password);

        System.out.println(hash);
    }
}