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
        d.id = json.optLong("id");
        d.date = new Date(json.optString("date"));
        d.text = json.optString("text");
        d.seen = json.optBoolean("seen");
        d.toUserId = json.optLong("toUserId");
        d.fromUserId = json.optLong("fromUserId");
        return d;
    }

}
