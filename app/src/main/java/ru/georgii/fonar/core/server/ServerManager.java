package ru.georgii.fonar.core.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ru.georgii.fonar.AppDatabase;
import ru.georgii.fonar.core.api.FonarRestClient;
import ru.georgii.fonar.core.api.SocketGateway;
import ru.georgii.fonar.core.api.callback.ServerManagerCallback;
import ru.georgii.fonar.core.api.callback.ServersObserverCallback;
import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.exception.FonarException;
import ru.georgii.fonar.core.exception.NotASupportedFonarServerException;
import ru.georgii.fonar.core.identity.UserIdentity;

public class ServerManager implements ServerManagerCallback {

    final ServerDao serverDao;
    final Set<ServersObserverCallback> subscribers = new HashSet<>();
    private Map<Long, SocketGateway> socketGateways = new HashMap<>();

    private ServerManager(ServerDao serverDao) {
        this.serverDao = serverDao;
    }

    public static ServerManager getServerManager(android.content.Context context) {
        try {
            AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, "fonar-db").build();
            return new ServerManager(db.serverDao());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize fonar db.");
        }
    }

    public void subscribe(ServersObserverCallback c) {
        subscribers.add(c);
    }

    protected Server addServer(Server server, UserIdentity identity) throws UnknownHostException, NotASupportedFonarServerException {
        server.setId(serverDao.insert(server));

        try {

            ServerConfigDto configuration = server.getConfiguration();
            if (!Objects.equals(configuration.api_spec, "FONAR")) {
                throw new NotASupportedFonarServerException("Server api specification '" + configuration.api_spec + "' is not supported");
            }
            server.setCachedName(configuration.server_name);
            server.subscribe(this);

            FonarRestClient.register(server, identity);
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new NotASupportedFonarServerException("Failed to get server configuration", e);
        } catch (FonarException e) {
            throw new NotASupportedFonarServerException(e);
        }

        for (ServersObserverCallback c : subscribers) {
            c.onServerAdded(server);
        }
        return server;
    }

    @Nullable
    public Server getServer(Long id) {
        return serverDao.findById(id);
    }

    public List<Server> getServers() {
        return serverDao.getAll();
    }

    private static Server currentServer;

    protected void removeServer(Server server) {
        server.close();
        for (ServersObserverCallback c : subscribers) {
            c.onServerRemoved(server);
        }
        serverDao.delete(server);
    }

    @NonNull
    public Server requireCurrentServer() throws FonarException {
        if (currentServer != null) {
            return currentServer;
        }
        if (getServers().size() != 0) {
            Server s = getServers().get(0);
            s.subscribe(this);
            currentServer = s;
            return s;
        }
        throw new FonarException("No current server is available");
    }

    public void setCurrentServer(Server server, UserIdentity identity) throws UnknownHostException, NotASupportedFonarServerException {
        List<Server> servers = serverDao.getAll();
        for (Server s : servers) {
            removeServer(s);
        }
        if (currentServer != null) {
            currentServer.close();
        }
        currentServer = null;
        addServer(server, identity);
    }

    @Override
    public void onServerConfigurationRetrieved(Server s, ServerConfigDto c) {
        s.setCachedName(c.server_name);
        serverDao.update(s);
    }


    @Override
    public synchronized SocketGateway getSocketGateway(Server s, UserIdentity identity) {
        if (!socketGateways.containsKey(s.getId())) {
            socketGateways.put(s.getId(), new SocketGateway(s, identity));
        }
        return socketGateways.get(s.getId());
    }

    @Override
    public void closeSocketGateway(Server s) {
        if (socketGateways.containsKey(s.getId())) {
            Objects.requireNonNull(this.socketGateways.get(s.getId())).close();
            this.socketGateways.remove(s.getId());
        }
    }

}
