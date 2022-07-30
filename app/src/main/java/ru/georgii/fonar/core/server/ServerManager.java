package ru.georgii.fonar.core.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Ignore;
import androidx.room.Room;

import java.io.IOException;
import java.net.URISyntaxException;
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
import ru.georgii.fonar.core.exception.FonarServerException;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.message.User;

public class ServerManager implements ServerManagerCallback {

    final ServerDao serverDao;
    final Set<ServersObserverCallback> subscribers = new HashSet<>();

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

    protected Server addServer(Server server, UserIdentity identity) throws FonarServerException {
        server.setId(serverDao.insert(server));

        try {
            ServerConfigDto configuration = server.getConfiguration();
            if (!Objects.equals(configuration.api_spec, "FONAR")) {
                throw new FonarServerException("Server api specification is not supported");
            }
            server.setCachedName(configuration.server_name);
            server.subscribe(this);

            FonarRestClient.register(server, identity);
        } catch (IOException e) {
            throw new FonarServerException(e);
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
    public Server requireCurrentServer() throws FonarServerException {
        if (currentServer != null)  {
            return currentServer;
        }
        if (getServers().size() != 0) {
            Server s = getServers().get(0);
            s.subscribe(this);
            currentServer = s;
            return s;
        }
        throw new FonarServerException("No current server is available");
    }

    public void setCurrentServer(Server server, UserIdentity identity) throws FonarServerException {
        for (Server s : serverDao.getAll()) {
            removeServer(s);
        }
        if (currentServer != null ) {
            try {
                currentServer.getSocketGateway(identity).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentServer = null;
        addServer(server, identity);
    }

    @Override
    public void onServerConfigurationRetrieved(Server s, ServerConfigDto c) {
        s.setCachedName(c.server_name);
        serverDao.update(s);
    }

    private Map<Long, SocketGateway> socketGateways;

    @Override
    public synchronized SocketGateway getSocketGateway(Server s, UserIdentity identity) {

        if (socketGateways == null) {
            socketGateways = new HashMap<>();
        }

        if (!socketGateways.containsKey(s.getId())) {
            try {
                socketGateways.put(s.getId(), SocketGateway.create(s, identity));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return socketGateways.get(s.getId());
    }

    @Override
    public void closeSocketGateway(Server s) {
        if (socketGateways.containsKey(s.getId())) {
            this.socketGateways.get(s.getId()).close();
            this.socketGateways.remove(s.getId());
        }
    }
}
