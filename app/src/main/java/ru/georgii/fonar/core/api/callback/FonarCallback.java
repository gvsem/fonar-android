package ru.georgii.fonar.core.api.callback;

import ru.georgii.fonar.core.message.Message;

public interface FonarCallback {

    default void onSubscribed() {

    }

    default void onError(Exception e) {

    }

    default void onUnsubscribed() {

    }

    default void messageReceived(Message m) {

    }

    default void messageSeen(Long messageId, Long fromUid) {

    }

    default void typing(Long uid) {

    }

    default void untyping(Long uid) {

    }

}
