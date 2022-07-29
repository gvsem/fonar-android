package ru.georgii.fonar.core.api;


import java.io.IOException;
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.georgii.fonar.core.dto.MessageDto;
import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.dto.UserDto;
import ru.georgii.fonar.core.exception.FonarServerException;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.message.Dialog;
import ru.georgii.fonar.core.message.Message;
import ru.georgii.fonar.core.server.Server;

public class FonarRestClient {

    final Server server;
    final UserIdentity id;
    ServerConfigDto serverConfig;
    FonarAPI api;

    public FonarRestClient(Server server, UserIdentity id) throws IOException {
        this.server = server;
        this.id = id;

        serverConfig = getServerConfig(server);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.getUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(FonarAPI.class);

    }

    public static FonarRestClient getInstance(Server server, UserIdentity identity) throws IOException {
        return new FonarRestClient(server, identity);
    }

    public static ServerConfigDto getServerConfig(Server server) throws IOException {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.getUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FonarServerAPI serverApi = retrofit.create(FonarServerAPI.class);

        Call<ServerConfigDto> call = serverApi.serverInformation();
        Response<ServerConfigDto> response = call.execute();
        if (response.code() != 200) {
            throw new IOException("Server unavailable.");
        }
        return response.body();

    }

    public static void register(Server server, UserIdentity identity) throws FonarServerException {

        ServerConfigDto configuration;
        try {
            configuration = getServerConfig(server);
        } catch (Exception e) {
            throw new FonarServerException("Failed to retrieve server configuration", e);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.getUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FonarServerAPI serverApi = retrofit.create(FonarServerAPI.class);

        Call<UserDto> call = serverApi.register(identity.generateKey(configuration.salt));
        Response<UserDto> response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new FonarServerException("registration failed", e);
        }
        if (response.code() != 200) {
            throw new FonarServerException("registration failed");
        }


    }

    private String getKey() {
        return id.generateKey(serverConfig.salt);
    }

    public List<Dialog> getDialogs(Long quantity, Long offset) {
        Call<List<Dialog>> call = api.getDialogs(getKey(), quantity, offset);

        try {
            Response<List<Dialog>> r = call.execute();
            return r.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> getMessages(Long uid, Long quantity, Long offset) {
        Call<List<Message>> call = api.getDialog(getKey(), uid, quantity, offset);

        try {
            Response<List<Message>> r = call.execute();
            return r.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Message sendMessage(Long uid, MessageDto m) {
        Call<Message> call = api.postMessage(getKey(), uid, m);

        try {
            Response<Message> r = call.execute();
            if (r.code() != 201) {
                throw new RuntimeException("Message failed to send.");
            }
            return r.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void seenMessage(Long uid, Long messageId) {
        Call<Void> call = api.seenMessage(getKey(), uid, messageId);

        try {
            call.execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public List<UserDto> getUsers(Long quantity, Long offset) {
        Call<List<UserDto>> call = api.getUsers(getKey(), quantity, offset);

        try {
            Response<List<UserDto>> r = call.execute();
            return r.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public UserDto getMe() throws FonarServerException {
        Call<UserDto> call = api.getMe(getKey());

        try {
            Response<UserDto> r = call.execute();
            if (r.body() == null) {
                throw new FonarServerException("Empty user profile.");
            }
            return r.body();
        } catch (IOException e) {
            throw new FonarServerException(e);
        }
    }

    public void setMe(UserDto dto) throws FonarServerException {
        Call<Void> call = api.updateUser(getKey(), dto);
        try {
            Response<Void> r = call.execute();
            if (r.code() != 201) {
                throw new FonarServerException(call.request().url() + " failed with code " + r.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new FonarServerException("Failed updating profile information.", e);
        }
    }

    public UserDto getUser(Long uid) {
        Call<UserDto> call = api.getUserInfo(getKey(), uid);

        try {
            Response<UserDto> r = call.execute();
            return r.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean updatePhoto(MultipartBody.Part image) {
        Call<Void> call = api.updatePhoto(getKey(), image);

        try {
            Response<Void> r = call.execute();
            if (r.code() != 201) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }


}
