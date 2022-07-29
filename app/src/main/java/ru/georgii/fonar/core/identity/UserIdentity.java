package ru.georgii.fonar.core.identity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.dto.UserDto;
import ru.georgii.fonar.core.exception.FonarServerException;
import ru.georgii.fonar.core.server.Server;

public class UserIdentity {

    private static UserIdentity instance = null;
    private String nickname;
    private String firstname;
    private String lastname;
    private String bio;
    private String avatarBytes;
    private String guid;
    private UserIdentity() {
    }

    public static UserIdentity getInstance(android.content.Context context) {
        if (instance == null) {
            instance = new UserIdentity();
            SharedPreferences p = context.getSharedPreferences("identity", Context.MODE_PRIVATE);
            instance.firstname = p.getString("firstname", "");
            instance.lastname = p.getString("lastname", "");
            instance.bio = p.getString("bio", "");
            instance.nickname = p.getString("nickname", "");
            instance.avatarBytes = p.getString("my_avatar_bytes", "");
            if (!p.contains("guid")) {
                p.edit().putString("guid", java.util.UUID.randomUUID().toString()).apply();
            }
            instance.guid = p.getString("guid", "");
            assert (!Objects.equals(instance.guid, ""));
        }
        return instance;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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

    public void setAvatarBytes(String avatarBytes) {
        this.avatarBytes = avatarBytes;
    }

    public Bitmap getAvatar() {
        byte[] avatarBytes = Base64.getDecoder().decode(this.avatarBytes);
        return BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.length);
    }

    public String generateKey(String salt) {
        //return "2";

        String key = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest(guid.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            key = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return key;
    }

    public void save(android.content.Context context) {
        SharedPreferences p = context.getSharedPreferences("identity", Context.MODE_PRIVATE);

        p.edit().putString("firstname", firstname)
                .putString("lastname", lastname)
                .putString("bio", bio)
                .putString("nickname", nickname)
                .putString("avatarBytes", avatarBytes)
                .apply();

    }

    public void updateOnServer(Server server) throws IOException, FonarServerException {
        UserDto u = new UserDto();
        u.firstname = firstname;
        u.lastname = lastname;
        u.bio = bio;
        u.nickname = nickname;

        server.getRestClient(this).setMe(u);
    }

    public void updateFromServer(UserDto profile) {
        if (profile == null) {
            return;
        }
        if (profile.firstname != null) {
            firstname = profile.firstname;
        }
        lastname = profile.lastname;
        bio = profile.bio;
        if (profile.nickname != null) {
            nickname = profile.nickname;
        }
        avatarBytes = profile.avatarBytes;
    }

}
