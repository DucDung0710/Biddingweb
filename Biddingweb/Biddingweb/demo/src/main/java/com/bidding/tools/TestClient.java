package com.bidding.tools;

import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) throws Exception {
        try (Socket s = new Socket("127.0.0.1", 9999);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream())) ) {

            JsonObject req = new JsonObject();
            req.addProperty("action", "REGISTER");
            req.addProperty("fullName", "Auto Test User");
            req.addProperty("email", "autotest@example.com");
            req.addProperty("password", "password123");
            req.addProperty("confirmPassword", "password123");
            req.addProperty("role", "Bidder");

            String json = req.toString();
            System.out.println("Sending: " + json);
            out.println(json);

            String resp = in.readLine();
            System.out.println("Response: " + resp);
        }
    }
}
