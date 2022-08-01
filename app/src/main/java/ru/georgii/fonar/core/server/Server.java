package ru.georgii.fonar.core.server;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.IOException;
import java.net.URISyntaxException;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.georgii.fonar.core.api.FonarRestClient;
import ru.georgii.fonar.core.api.FonarServerAPI;
import ru.georgii.fonar.core.api.SocketGateway;
import ru.georgii.fonar.core.api.callback.ServerManagerCallback;
import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.identity.UserIdentity;

@Entity
public class Server {

    @ColumnInfo(name = "url")
    public String url;
    @PrimaryKey(autoGenerate = true)
    private Long id;
    @ColumnInfo(name = "cachedName")
    private String cachedName;

    public Server(String url) {
        this.url = url;
    }

    protected Server() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCachedName() {
        return cachedName;
    }

    public void setCachedName(String cachedName) {
        this.cachedName = cachedName;
    }

    public FonarRestClient getRestClient(UserIdentity identity) throws IOException {
        return new FonarRestClient(this, identity);
    }

    public ServerConfigDto getConfiguration() throws IOException {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(this.getUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FonarServerAPI serverApi = retrofit.create(FonarServerAPI.class);

        Call<ServerConfigDto> call = serverApi.serverInformation();
        Response<ServerConfigDto> response = call.execute();
        if (response.code() != 200) {
            throw new IOException("Server unavailable.");
        }

        ServerConfigDto r = response.body();
        if (callback != null) {
            callback.onServerConfigurationRetrieved(this, r);
        }
        return r;

    }

    @Ignore
    ServerManagerCallback callback;

    public void subscribe(ServerManagerCallback c) {
        this.callback = c;
    }

    public SocketGateway getSocketGateway(UserIdentity identity) throws IOException {
        if (callback == null) {
            return null;
        }
        return callback.getSocketGateway(this, identity);
    }

    public void close() {
        if (callback == null) {
            return;
        }
        callback.closeSocketGateway(this);
    }

}
