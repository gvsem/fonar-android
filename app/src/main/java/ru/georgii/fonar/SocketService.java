package ru.georgii.fonar;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.IOException;

import ru.georgii.fonar.core.api.callback.FonarCallback;
import ru.georgii.fonar.core.api.callback.ServersObserverCallback;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.message.Message;
import ru.georgii.fonar.core.server.Server;
import ru.georgii.fonar.core.server.ServerManager;

public class SocketService extends Service implements FonarCallback, ServersObserverCallback {

    final IBinder mBinder = new LocalBinder();
    ServerManager serverManager;

    @Override
    public void onCreate() {
        this.serverManager = ServerManager.getServerManager(getApplicationContext());
        this.serverManager.subscribe(this);

        (new Thread() {
            public void run() {
                for (Server s : serverManager.getServers()) {
                    try {
                        s.getSocketGateway(getUserIdentity()).subscribe(SocketService.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public UserIdentity getUserIdentity() {
        return UserIdentity.getInstance(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        (new Thread() {
            public void run() {
                for (Server s : serverManager.getServers()) {
                    s.close();
                }
            }
        }).start();

    }

    @Override
    public void messageReceived(Message m) {
        // we can send notifications.
    }

    @Override
    public void onServerAdded(Server s) {
        try {
            s.getSocketGateway(getUserIdentity()).subscribe(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public SocketService getServerInstance() {
            return SocketService.this;
        }
    }


}
