package ru.georgii.fonar.core.message;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

import ru.georgii.fonar.core.dto.ServerConfigDto;

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

    public String getVisibleUsername() {
        if ((firstname != null) && (lastname != null)) {
            return firstname + " " + lastname;
        } else if (firstname != null) {
            return firstname;
        } else if (lastname != null) {
            return lastname;
        } else {
            return "Unknown";
        }
    }

    public String getAddress(String server_name) {
        if (nickname != null) {
            return nickname + " @ " + server_name;
        }
        return server_name;
    }

    public String getAddress(ServerConfigDto serverConfig) {
        if (nickname != null) {
            return nickname + " @ " + serverConfig.server_name;
        }
        return serverConfig.server_name;
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] getAvatarBytes() {
        if (this.avatarBytes == null) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(this.avatarBytes);
    }

    @NonNull
    public Bitmap getBitmap() {
        return BitmapFactory.decodeByteArray(getAvatarBytes(), 0, getAvatarBytes().length);
    }

}
