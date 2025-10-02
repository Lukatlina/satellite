package com.example.satellite.model;

import java.util.ArrayList;

public class ChatResponse {
    private ArrayList<chat_user> messages;
    private int result;

    // Getter Setter
    public ArrayList<chat_user> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<chat_user> messages) {
        this.messages = messages;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}