package com.dzakyhdr.fixholand.Model;

public class BraintreeToken {

    public boolean error;
    private String token;

    public BraintreeToken() {
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

