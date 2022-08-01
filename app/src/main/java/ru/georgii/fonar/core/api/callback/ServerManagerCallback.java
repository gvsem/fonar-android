package ru.georgii.fonar.core.api.callback;

import ru.georgii.fonar.core.api.SocketGateway;
import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.server.Server;

public interface ServerManagerCallback {

    default void onServerConfigurationRetrieved(Server s, ServerConfigDto c) {

    }

    default SocketGateway getSocketGateway(Server s, UserIdentity identity) {
        return null;
    }

    default void closeSocketGateway(Server s) {

    }

}
