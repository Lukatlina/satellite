package com.example.satellite;

public class home_user {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_USER = 1;
    public static final int TYPE_PLANET = 2;
    public static final int TYPE_ARTIST = 3;

    int usertype;
    int id;
    String nickname;
    String message;
    String image;
    String title;


    public home_user(int usertype, String title) {
        this.usertype = usertype;
        this.title = title;
    }

    public home_user(int usertype, int id, String message, String nickname) {
        this.usertype = usertype;
        this.id = id;
        this.message = message;
        this.nickname = nickname;
    }

    public home_user(int usertype, int id,  String message, String nickname, String profile) {
        this.usertype = usertype;
        this.id = id;
        this.image = profile;
        this.message = message;
        this.nickname = nickname;
    }

    public int getUsertype() {
        return usertype;
    }

    public void setUsertype(int usertype) {
        this.usertype = usertype;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

