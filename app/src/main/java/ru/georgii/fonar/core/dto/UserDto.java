package ru.georgii.fonar.core.dto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.gson.annotations.Expose;

import java.util.Base64;

import ru.georgii.fonar.core.message.User;

public class UserDto {

    @Nullable
    public String avatarBytes;

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] getAvatarBytes() {
        if (this.avatarBytes == null) {
            return null;
        }
        return Base64.getDecoder().decode(this.avatarBytes);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Bitmap getAvatar() {

        byte[] r = getAvatarBytes();
        if (r == null) {
            r = new byte[0];
        }
        return BitmapFactory.decodeByteArray(r, 0, r.length);
    }

}
