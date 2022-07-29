package ru.georgii.fonar.core.message;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class Dialog implements Parcelable {

    public static final Parcelable.Creator<Dialog> CREATOR = new Parcelable.Creator<Dialog>() {
        public Dialog createFromParcel(Parcel in) {
            return new Dialog(in);
        }

        public Dialog[] newArray(int size) {
            return new Dialog[size];
        }

    };
    Message lastMessage;
    User user;
    boolean lastMessageIsToMe;
    String avatarBytes;
    Long unreadMessages;

    private Dialog() {

    }

    public Dialog(Parcel in) {
        super();
        readFromParcel(in);
    }

    public static Dialog fromJson(JSONObject json) throws JSONException {

        Dialog d = new Dialog();

        d.lastMessage = Message.fromJson(json.getJSONObject("lastMessage"));
        d.lastMessageIsToMe = json.getBoolean("lastMessageIsToMe");
        d.user = User.fromJson(json.getJSONObject("user"));
        d.unreadMessages = json.getLong("unreadMessages");

        return d;
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] getAvatarBytes() {
        if (this.avatarBytes == null) {
            return null;
        }
        return Base64.getDecoder().decode(this.avatarBytes);
    }

    public void readFromParcel(Parcel in) {

        JSONObject json = null;
        try {
            json = new JSONObject(in.readString());

            this.lastMessage = Message.fromJson(json.getJSONObject("lastMessage"));
            this.lastMessageIsToMe = json.getBoolean("lastMessageIsToMe");
            this.user = User.fromJson(json.getJSONObject("user"));
            this.unreadMessages = json.getLong("unreadMessages");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        Gson gson = new Gson();
        dest.writeString(gson.toJson(this));
    }


}
