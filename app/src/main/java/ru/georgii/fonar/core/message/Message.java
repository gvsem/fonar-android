package ru.georgii.fonar.core.message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Message {

    public Long id;

    public Date date;

    public String text;

    public boolean seen;

    public Long fromUserId;

    public Long toUserId;

    protected Message() {
    }


    public static Message fromJson(JSONObject json) throws JSONException {
        Message d = new Message();
        d.id = json.getLong("id");
        long unix = json.getLong("date");
        d.date = new Date(unix);
        d.text = json.getString("text");
        d.seen = json.getBoolean("seen");
        d.toUserId = json.getLong("toUserId");
        d.fromUserId = json.getLong("fromUserId");
        return d;
    }

}
