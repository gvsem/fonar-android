package ru.georgii.fonar.core.message;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class User {

    public Long id;

    public String nickname;

    public String firstname;

    public String lastname;

    public String avatarUrl;

    @Nullable
    public String avatarBytes;
    public String bio;

    public User() {
    }

    public static User fromJson(JSONObject json) throws JSONException {
        User d = new User();
        d.id = json.getLong("id");
        d.nickname = json.getString("nickname");
        d.firstname = json.getString("firstname");
        d.lastname = json.getString("lastname");
        d.bio = json.getString("bio");
        d.avatarUrl = json.getString("avatarUrl");
        d.avatarBytes = json.getString("avatarBytes");
        return d;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getBio() {
        return bio;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] getAvatarBytes() {
        if (this.avatarBytes == null) {
            return null;
        }
        byte [] r = Base64.getDecoder().decode(this.avatarBytes);
        return r;
    }

}
