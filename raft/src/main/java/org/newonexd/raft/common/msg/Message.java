package org.newonexd.raft.common.msg;

import com.google.gson.Gson;

import java.io.Serializable;

public class Message implements Serializable {
    public Message(MessageType messageType,MessageBody messageBody) {
        this.messageType = messageType;
        this.messageBody = messageBody;
    }


    private MessageType messageType;

    public MessageType getMessageType() {
        return messageType;
    }

    public MessageBody getMessageBody() {
        return messageBody;
    }

    private MessageBody messageBody;

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
