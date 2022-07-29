package ru.georgii.fonar.core.api.callback;

import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.server.Server;

public interface ServerManagerCallback {

    default void onServerConfigurationRetrieved(Server s, ServerConfigDto c) {

    }

}
