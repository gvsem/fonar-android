package ru.georgii.fonar;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ru.georgii.fonar.core.api.callback.FonarCallback;

public abstract class FonarActivity extends AppCompatActivity implements FonarCallback {

    boolean mBounded;
    SocketService mServer;
    Intent mIntent;
    ServiceConnection mConnection;

    @Override
    protected void onStart() {
        super.onStart();
        mIntent = new Intent(this, SocketService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBounded = false;
                mServer = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBounded = true;
                mServer = ((SocketService.LocalBinder) service).getServerInstance();
                FonarActivity.this.onServiceConnected(mServer);
            }
        };


    }

    @Nullable
    public SocketService getFonarService() {
        if (mBounded) {
            return mServer;
        }
        return null;
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    }

    abstract void onServiceConnected(SocketService service);

}
