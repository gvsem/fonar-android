package ru.georgii.fonar.core.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.dto.UserDto;

public interface FonarServerAPI {

    @GET("/version")
    Call<ServerConfigDto> serverInformation();

    @GET("/register")
    Call<UserDto> register(@Query("key") String key);


}
