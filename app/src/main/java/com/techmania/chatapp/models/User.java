package com.techmania.chatapp.models;

public class User {

    String userId;
    String userName;
    String userEmail;
    String imageUrl;

    public User(){

    }

    public User(String userId, String userName, String userEmail, String imageUrl) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
