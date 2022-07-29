package ru.georgii.fonar.core.dto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;

import java.util.Base64;

import ru.georgii.fonar.core.message.User;

public class UserDto {

    @Nullable
    public final String avatarBytes = "";
    public Long id;
    public String nickname;
    public String firstname;
    public String lastname;
    @Nullable
    @Expose(serialize = false)
    public String avatarUrl;
    public String bio;

    public User toUser() {
        User u = new User();
        u.id = id;
        u.nickname = nickname;
        u.firstname = firstname;
        u.lastname = lastname;
        u.avatarUrl = avatarUrl;
        u.bio = bio;
        u.avatarBytes = avatarBytes;
        return u;
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

    public String getAddress(ServerConfigDto serverConfig) {
        if (nickname != null) {
            return nickname + " @ " + serverConfig.server_name;
        }
        return serverConfig.server_name;
    }

    public Bitmap getAvatar() {
        byte[] avatarBytes = Base64.getDecoder().decode(this.avatarBytes);
        return BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
    }

}
