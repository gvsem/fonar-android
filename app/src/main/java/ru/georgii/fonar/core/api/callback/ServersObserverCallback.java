package ru.georgii.fonar.core.api.callback;

import ru.georgii.fonar.core.server.Server;

public interface ServersObserverCallback {

    default void onServerAdded(Server s) {

    }

    default void onServerRemoved(Server s) {

    }

}
