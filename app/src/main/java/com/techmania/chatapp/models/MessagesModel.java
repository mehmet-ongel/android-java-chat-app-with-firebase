package com.techmania.chatapp.models;

public class MessagesModel {

    String senderId;
    String receiverId;
    String messageId;
    String message;

    public MessagesModel(){

    }

    public MessagesModel(String senderId, String receiverId, String messageId, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageId = messageId;
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }
}
