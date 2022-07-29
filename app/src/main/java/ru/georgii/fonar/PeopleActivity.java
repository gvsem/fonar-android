package ru.georgii.fonar;

import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import ru.georgii.fonar.core.server.Server;
import ru.georgii.fonar.gui.UserListFragment;

public class PeopleActivity extends FonarActivity {

    Toolbar toolbar;
    UserListFragment usersView;

    Server server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_people);
        usersView = (UserListFragment) getSupportFragmentManager().findFragmentById(R.id.userListFragment);

        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.logo_text), PorterDuff.Mode.SRC_ATOP);

    }

    @Override
    void onServiceConnected(SocketService mServer) {
        (new Thread() {
            public void run() {
                try {
                    server = mServer.getServerManager().requireCurrentServer();
                    usersView.setServer(server, mServer.getUserIdentity());
                    toolbar.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    finish();
                }
            }
        }).start();
    }

}