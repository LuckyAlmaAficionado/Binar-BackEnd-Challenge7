package com.binar.securityspringboot.model;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationMessage {

    private String recipientToken;
    private String title;
    private String body;
    private String image;
    private Map<String, String> data;

    public NotificationMessage(String recipientToken, String title, String body) {
        this.recipientToken = recipientToken;
        this.title = title;
        this.body = body;
    }
}
