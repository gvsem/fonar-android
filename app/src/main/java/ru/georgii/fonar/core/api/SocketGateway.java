package ru.georgii.fonar.core.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import ru.georgii.fonar.core.api.callback.FonarCallback;
import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.exception.MalformedSocketMessageException;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.message.Message;
import ru.georgii.fonar.core.server.Server;

public class SocketGateway {

    public Socket socket;
    private final Set<FonarCallback> allSubscribers = new HashSet<>();
    private final Map<Long, Set<FonarCallback>> userSubscribers = new HashMap<>();

    private Server server;
    private UserIdentity identity;

    public SocketGateway(Server server, UserIdentity identity) {
        this.server = server;
        this.identity = identity;
    }

    public static Socket createSocket(SocketGateway socketGateway) throws URISyntaxException, IOException {
        ServerConfigDto configuration = socketGateway.server.getConfiguration();

        IO.Options config = new IO.Options();
        config.query = "authorization=" + socketGateway.identity.generateKey(configuration.salt);
        config.reconnection = true;
        config.reconnectionDelay = 10000;
        config.timeout = -1;
        config.transports = new String[]{WebSocket.NAME};
        config.secure = true;

        String socketUrl = configuration.socketUrl;
        if (configuration.socketUrl.startsWith(":")) {
            URL serverUrl = new URL(socketGateway.server.url);
            socketUrl = "http://" + serverUrl.getHost() + socketUrl;
        }

        Socket socket = IO.socket(socketUrl, config);
        socket.on("connect", socketGateway::onConnected);
        socket.on("connect_error", socketGateway::onConnectionError);
        socket.on("error", socketGateway::onError);
        socket.on("youGotMessage", socketGateway::onNewMessage);
        socket.on("yourMessageSeen", socketGateway::onMessageSeen);
        socket.on("userStartedTyping", socketGateway::onUserStartedTyping);
        socket.on("userStoppedTyping", socketGateway::onUserStoppedTyping);
        socket.on("disconnect", socketGateway::onDisconnected);

        socket.connect();
        return socket;
    }

    private void onUserStartedTyping(Object... args) {
        try {
            Long uid = requireLong(0, args);
            tellEveryoneAboutUser(uid, (fonarCallback -> fonarCallback.typing(uid)));
            System.out.println(uid + " started typing to me");
        } catch (MalformedSocketMessageException e) {
            tellEveryoneAboutError(e);
        }
    }

    private void onUserStoppedTyping(Object... args) {
        try {
            Long uid = requireLong(0, args);
            tellEveryoneAboutUser(uid, (fonarCallback -> fonarCallback.untyping(uid)));
            System.out.println(uid + " stopped typing to me");
        } catch (MalformedSocketMessageException e) {
            tellEveryoneAboutError(e);
        }
    }


    private void onMessageSeen(Object... args) {
        try {
            Long mid = requireLong(0, args);
            Long uid = requireLong(1, args);
            tellEveryoneAboutUser(uid, (fonarCallback -> fonarCallback.messageSeen(mid, uid)));
            System.out.println(uid + " has seen my message " + mid);
        } catch (MalformedSocketMessageException e) {
            tellEveryoneAboutError(e);
        }
    }

    private void onNewMessage(Object... args) {
        try {
            String messageJsonText = requireString(0, args);
            JSONObject messageJson = new JSONObject(messageJsonText);
            Message m = Message.fromJson(messageJson);
            tellEveryoneAboutUser(m.fromUserId, (fonarCallback) -> fonarCallback.messageReceived(m));
            System.out.println(m.fromUserId + " sent me new message " + m.id);
        } catch (MalformedSocketMessageException e) {
            tellEveryoneAboutError(e);
        } catch (JSONException e) {
            tellEveryoneAboutError(new MalformedSocketMessageException(e));
        }
    }

    private void onError(Object... args) {
        tellEveryoneAboutError((Exception) args[0]);
        System.out.println("Error from " + server.url);
    }

    private void onDisconnected(Object... objects) {
        (new Thread(() -> {
            try {
                Thread.sleep(5000);
                connectIfSomeoneListens();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        })).start();
    }

    protected void onConnected(Object... args) {
        System.out.println("Connected to " + server.url);
    }

    protected void onConnectionError(Object... args) {
        tellEveryoneAboutError((Exception) args[0]);
        System.out.println("Connection error from " + server.url);
    }

    public void subscribe(FonarCallback c) {
        this.allSubscribers.add(c);
        c.onSubscribed();
        connectIfSomeoneListens();
    }

    public void unsubscribe(FonarCallback c) {
        this.allSubscribers.remove(c);
        c.onUnsubscribed();
        disconnectIfNobodyListens();
    }

    public void subscribeForUser(Long id, FonarCallback c) {
        if (!userSubscribers.containsKey(id)) {
            userSubscribers.put(id, new HashSet<>());
        }
        Objects.requireNonNull(userSubscribers.get(id)).add(c);
        c.onSubscribed();
        connectIfSomeoneListens();
    }

    public void unsubscribeForUser(Long id, FonarCallback c) {
        if (userSubscribers.containsKey(id)) {
            Objects.requireNonNull(userSubscribers.get(id)).remove(c);
            c.onUnsubscribed();
            disconnectIfNobodyListens();
        }
    }

    public void notifyTypingStart(Long uid) {
        connectIfNotAlready();
        socket.emit("meStartedTyping", uid);
        System.out.println("meStartedTyping to " + uid);
    }

    public void notifyTypingStopped(Long uid) {
        connectIfNotAlready();
        socket.emit("meStoppedTyping", uid);
        System.out.println("meStoppedTyping to " + uid);
    }

    public void seenMessage(Long messageId, Long uid) {
        connectIfNotAlready();
        socket.emit("meSeenMessage", messageId, uid);
        System.out.println("seenMessage id " + messageId + " of " + uid);
    }

    public void close() {
        allSubscribers.forEach(FonarCallback::onUnsubscribed);
        userSubscribers.values().forEach((subs) -> subs.forEach(FonarCallback::onUnsubscribed));
        allSubscribers.clear();
        userSubscribers.clear();
        disconnectIfNobodyListens();
    }

    public void reconnectIfSomeoneListens() {
        disconnectIfNotAlready();
        connectIfSomeoneListens();
    }

    private synchronized void connectIfNotAlready() {
        if (socket == null) {
            try {
                socket = createSocket(this);
            } catch (Exception e) {
                tellEveryoneAboutError(e);
            }
        }
    }

    private synchronized void disconnectIfNotAlready() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
            socket = null;
        }
    }

    private void connectIfSomeoneListens() {
        if (allSubscribers.isEmpty() && userSubscribers.isEmpty()) {
            return;
        }
        connectIfNotAlready();
    }

    private void disconnectIfNobodyListens() {
        if (allSubscribers.isEmpty() && userSubscribers.isEmpty()) {
            disconnectIfNotAlready();
        }
    }

    private void tellEveryoneAboutError(Exception e) {
        tellEveryone((callback) -> callback.onError(e));
    }

    private void tellEveryone(Consumer<FonarCallback> f) {
        allSubscribers.forEach(f);
        for (Set<FonarCallback> s : userSubscribers.values()) {
            s.forEach(f);
        }
    }

    private void tellEveryoneAboutUser(Long uid, Consumer<FonarCallback> f) {
        allSubscribers.forEach(f);
        if (userSubscribers.containsKey(uid)) {
            Objects.requireNonNull(userSubscribers.get(uid)).forEach(f);
        }
    }

    private static long requireLong(int index, Object... args) throws MalformedSocketMessageException {
        if (index >= args.length) {
            throw new MalformedSocketMessageException("Argument with index " + index + " not presented.");
        }
        if (args[index] instanceof Integer) {
            return Long.valueOf((Integer) args[index]);
        } else if (args[index] instanceof Long) {
            return (Long) args[index];
        } else {
            throw new MalformedSocketMessageException("Argument with index " + index + " is not Integer/Long");
        }
    }

    private static String requireString(int index, Object... args) throws MalformedSocketMessageException {
        if (index >= args.length) {
            throw new MalformedSocketMessageException("Argument with index " + index + " not presented.");
        }
        if (args[index] instanceof String) {
            return (String) args[index];
        } else {
            throw new MalformedSocketMessageException("Argument with index " + index + " is not String");
        }
    }

}
