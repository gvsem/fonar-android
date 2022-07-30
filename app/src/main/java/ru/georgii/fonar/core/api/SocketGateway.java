package ru.georgii.fonar.core.api;

import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import ru.georgii.fonar.core.api.callback.FonarCallback;
import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.message.Message;
import ru.georgii.fonar.core.server.Server;

public class SocketGateway {

    public Socket socket;
    private final Set<FonarCallback> allSubscribers = new HashSet<>();
    private final Map<Long, Set<FonarCallback>> userSubscribers = new HashMap<>();

    private Server server;
    private UserIdentity identity;
    private boolean isConnected;


    private SocketGateway(Socket socket) {
        this.socket = socket;
    }

    public static Socket createSocket(SocketGateway socketGateway, Server server, UserIdentity identity) throws URISyntaxException, IOException {
        ServerConfigDto configuration = server.getConfiguration();

        IO.Options config = new IO.Options();
        config.query = "authorization=" + identity.generateKey(configuration.salt);
        config.reconnection = true;
        config.reconnectionDelay = 10000;
        config.timeout = -1;
        config.transports = new String[]{WebSocket.NAME};
        config.secure = true;

        String socketUrl = configuration.socketUrl;
        if (configuration.socketUrl.startsWith(":")) {
            URL serverUrl = new URL(server.url);
            socketUrl = "http://" + serverUrl.getHost() + socketUrl;
        }
        Socket socket = IO.socket(socketUrl, config);

        socket.on("connect_error", socketGateway::onConnectionError);
        socket.on("connect", socketGateway::onConnected);
        socket.on("disconnect", args -> {
            System.out.println("Disconnected from " + server.url);
            socketGateway.reconnect();
        });
        socket.on("error", socketGateway::onConnectionError);

        socket.on("message", args -> {
            try {
                socketGateway.onMessageDelivered(Message.fromJson(new JSONObject((String) args[0])));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socket.on("messageSeen", args -> {
            try {
                socketGateway.onMessageSeen(Long.valueOf((Integer) args[0]), Long.valueOf((Integer) args[1]));
                System.out.println(Long.valueOf((Integer) args[0]) + "has seen message " + Long.valueOf((Integer) args[1]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socket.on("userStartedTyping", args -> {
            try {
                socketGateway.onStartedTyping(Long.valueOf((Integer) args[0]));
                System.out.println(Long.valueOf((Integer) args[0]) + "started typing to me");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socket.on("userStoppedTyping", args -> {
            try {
                socketGateway.onStoppedTyping(Long.valueOf((Integer) args[0]));
                System.out.println(Long.valueOf((Integer) args[0]) + "stopped typing to me");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        socket.connect();
        return socket;
    }

    public static SocketGateway create(Server server, UserIdentity identity) throws IOException, URISyntaxException {

        if (identity == null) {
            return null;
        }

        SocketGateway gateway = new SocketGateway(null);
        gateway.socket  = SocketGateway.createSocket(gateway, server, identity);
        gateway.server = server;
        gateway.identity = identity;
        return gateway;
    }

    protected void onConnected(Object... args) {
        isConnected = true;
        System.out.println("Connected to " + server.url);
    }

    protected void onConnectionError(Object... args) {
        isConnected = false;
        ((Exception) args[0]).printStackTrace();
        System.out.println("Error from " + server.url);
    }

    public void subscribe(FonarCallback c) {
        this.allSubscribers.add(c);
    }

    public void unsubscribe(FonarCallback c) {
        this.allSubscribers.remove(c);
    }

    public void subscribeForUser(Long id, FonarCallback c) {
        if (!userSubscribers.containsKey(id)) {
            userSubscribers.put(id, new HashSet<>());
        }
        userSubscribers.get(id).add(c);
    }

    public void unsubscribeForUser(Long id, FonarCallback c) {
        if (userSubscribers.containsKey(id)) {
            userSubscribers.get(id).remove(c);
        }
    }

    public void notifyTypingStart(Long uid) {
        if (!socket.connected()) {
            this.reconnect();
        }
        socket.emit("meStartedTyping", uid);
        System.out.println("meStartedTyping to " + uid);
    }

    public void notifyTypingStopped(Long uid) {
        if (!socket.connected()) {
            this.reconnect();
        }

        socket.emit("meStoppedTyping", uid);
        System.out.println("meStoppedTyping to " + uid);
    }

    public void seenMessage(Long messageId, Long uid) {
        if (!socket.connected()) {
            this.reconnect();
        }
        socket.emit("seenMessage", messageId, uid);
        System.out.println("seenMessage id " + messageId + " of " + uid);
    }

    public void close() {
        //if (socket.connected() && isConnected) {
            socket.disconnect();
            socket.close();

        //}
    }

    protected void onMessageDelivered(Message m) {
        if (m == null) {
            return;
        }

        for (FonarCallback c : allSubscribers) {
            c.messageReceived(m);
        }
        if (userSubscribers.containsKey(m.fromUserId)) {
            for (FonarCallback c : userSubscribers.get(m.fromUserId)) {
                c.messageReceived(m);
            }
        }

        System.out.println("received message from " + m.fromUserId);
    }

    protected void onMessageSeen(Long messageId, Long fromUserId) {
        if (messageId == null) {
            return;
        }

        for (FonarCallback c : allSubscribers) {
            c.messageSeen(messageId, fromUserId);
        }
        if (userSubscribers.containsKey(fromUserId)) {
            for (FonarCallback c : userSubscribers.get(fromUserId)) {
                c.messageSeen(messageId, fromUserId);
            }
        }

        System.out.println("received seen notification for message " + fromUserId + " from " + fromUserId);
    }

    protected void onStartedTyping(Long userId) {
        if (userId == null) {
            return;
        }

        for (FonarCallback c : allSubscribers) {
            c.typing(userId);
        }
        if (userSubscribers.containsKey(userId)) {
            for (FonarCallback c : userSubscribers.get(userId)) {
                c.typing(userId);
            }
        }

    }

    protected void onStoppedTyping(Long userId) {
        if (userId == null) {
            return;
        }

        for (FonarCallback c : allSubscribers) {
            c.untyping(userId);
        }
        if (userSubscribers.containsKey(userId)) {
            for (FonarCallback c : userSubscribers.get(userId)) {
                c.untyping(userId);
            }
        }

        System.out.println("stopped typing " + userId);
    }

    public void reconnect() {
        System.out.println("Attempting to reconnect to " + server.getUrl());
        if (socket != null) {
            socket.disconnect();
            socket.close();
            try {
                socket = SocketGateway.createSocket(this, server, identity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
