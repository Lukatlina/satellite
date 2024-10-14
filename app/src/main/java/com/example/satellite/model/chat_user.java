package com.example.satellite.model;

public class chat_user {

    public static final int VIEW_TYPE_SENDER = 0;
    public static final int VIEW_TYPE_RECEIVER = 1;
    public static final int VIEW_TYPE_HEADER= 2;

    // 채팅방 ID, 아티스트 여부, 보낸 사람 ID, 메시지, 닉네임, 프로필 사진
    private int usertype;
    private int chat_id;
    private int message_id;
    private String chatroom_name; // 채팅방 id
    private int sender_id; // 보낸 사람
    private int is_artist; //아티스트 여부
    private String image; // 보낸 사람 프로필 사진
    private String nickname; // 보낸 사람 닉네임
    private String message; // 보낸 사람 메시지
    private String sent_time;
    private int fan_message_count;

    public chat_user(int usertype, String sent_time) {
        this.usertype = usertype;
        this.sent_time = sent_time;
    }

    public chat_user(int usertype, int chat_id, int message_id, int is_artist) {
        this.usertype = usertype;
        this.chat_id = chat_id;
        this.message_id = message_id;
        this.is_artist = is_artist;
    }

    public chat_user(int usertype, int sender_id, String nickname, String message, String sent_time) {
        this.usertype = usertype;
        this.sender_id = sender_id;
        this.nickname = nickname;
        this.message = message;
        this.sent_time = sent_time;
    }

    public chat_user(int usertype, int chat_id, String chatroom_name, int sender_id, int is_artist, String image, String nickname, String message, String sent_time) {
        this.usertype = usertype;
        this.chat_id = chat_id;
        this.chatroom_name = chatroom_name;
        this.sender_id = sender_id;
        this.is_artist = is_artist;
        this.image = image;
        this.nickname = nickname;
        this.message = message;
        this.sent_time = sent_time;
    }

    public int getUsertype() {
        return usertype;
    }

    public void setUsertype(int usertype) {
        this.usertype = usertype;
    }

    public int getChat_id() {
        return chat_id;
    }

    public void setChat_id(int chat_id) {
        this.chat_id = chat_id;
    }

    public int getMessage_id() {
        return message_id;
    }

    public void setMessage_id(int message_id) {
        this.message_id = message_id;
    }

    public String getChatroom_name() {
        return chatroom_name;
    }

    public void setChatroom_name(String chatroom_name) {
        this.chatroom_name = chatroom_name;
    }

    public int getSender_id() {
        return sender_id;
    }

    public void setSender_id(int sender_id) {
        this.sender_id = sender_id;
    }

    public int getIs_artist() {
        return is_artist;
    }

    public void setIs_artist(int is_artist) {
        this.is_artist = is_artist;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSent_time() {
        return sent_time;
    }

    public void setSent_time(String sent_time) {
        this.sent_time = sent_time;
    }

    public int getFan_message_count() {
        return fan_message_count;
    }

    public void setFan_message_count(int fan_message_count) {
        this.fan_message_count = fan_message_count;
    }
}