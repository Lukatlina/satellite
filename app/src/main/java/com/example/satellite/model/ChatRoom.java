package com.example.satellite.model;

public class ChatRoom {

    int chat_id;
    int artist_id;
    String artist_image;
    String artist_nickname;
    String last_message;
    String sent_time;

    public ChatRoom(int chat_id, int artist_id, String artist_image, String artist_nickname, String last_message, String sent_time) {
        this.chat_id = chat_id;
        this.artist_id = artist_id;
        this.artist_image = artist_image;
        this.artist_nickname = artist_nickname;
        this.last_message = last_message;
        this.sent_time = sent_time;
    }

    public int getChat_id() {
        return chat_id;
    }

    public void setChat_id(int chat_id) {
        this.chat_id = chat_id;
    }

    public int getArtist_id() {
        return artist_id;
    }

    public void setArtist_id(int artist_id) {
        this.artist_id = artist_id;
    }

    public String getArtist_image() {
        return artist_image;
    }

    public void setArtist_image(String artist_image) {
        this.artist_image = artist_image;
    }

    public String getArtist_nickname() {
        return artist_nickname;
    }

    public void setArtist_nickname(String artists_nickname) {
        this.artist_nickname = artists_nickname;
    }

    public String getLast_message() {
        return last_message;
    }

    public void setLast_message(String last_message) {
        this.last_message = last_message;
    }

    public String getSent_time() {
        return sent_time;
    }

    public void setSent_time(String sent_time) {
        this.sent_time = sent_time;
    }
}
