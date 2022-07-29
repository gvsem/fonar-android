package ru.georgii.fonar.core.api;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;
import ru.georgii.fonar.core.dto.MessageDto;
import ru.georgii.fonar.core.dto.UserDto;
import ru.georgii.fonar.core.message.Dialog;
import ru.georgii.fonar.core.message.Message;

public interface FonarAPI {

    @GET("v1.0/dialogs")
    Call<List<Dialog>> getDialogs(@Header("Authorization") String key, @Query("quantity") Long quantity, @Query("offset") Long offset);

    @GET("v1.0/dialog")
    Call<List<Message>> getDialog(@Header("Authorization") String key, @Query("userId") Long userId, @Query("quantity") Long quantity, @Query("offset") Long offset);

    @POST("v1.0/message")
    Call<Message> postMessage(@Header("Authorization") String key, @Query("userId") Long userId, @Body MessageDto message);

    @PUT("v1.0/message")
    Call<Void> seenMessage(@Header("Authorization") String key, @Query("userId") Long userId, @Query("messageId") Long messageId);

    @POST("v1.0/user/info")
    Call<Void> updateUser(@Header("Authorization") String key, @Body UserDto info);

    @GET("v1.0/me")
    Call<UserDto> getMe(@Header("Authorization") String key);

    @GET("v1.0/user/info")
    Call<UserDto> getUserInfo(@Header("Authorization") String key, @Query("userId") Long userId);

    @GET("v1.0/users")
    Call<List<UserDto>> getUsers(@Header("Authorization") String key, @Query("quantity") Long quantity, @Query("offset") Long offset);

    @Multipart
    @POST("v1.0/user/photo")
    Call<Void> updatePhoto(@Header("Authorization") String key, @Part MultipartBody.Part image);

    // GET v1.0/user/photo


}
