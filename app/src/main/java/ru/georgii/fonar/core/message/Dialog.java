package ru.georgii.fonar.core.message;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class Dialog {

    Message lastMessage;
    User user;
    boolean lastMessageIsToMe;
    Long unreadMessages;

    private Dialog() {

    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public User getUser() {
        return user;
    }

    public boolean isLastMessageIsToMe() {
        return lastMessageIsToMe;
    }

    public Long getUnreadMessages() {
        return unreadMessages;
    }

}
