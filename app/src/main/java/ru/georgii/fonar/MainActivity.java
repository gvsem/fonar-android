package ru.georgii.fonar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import ru.georgii.fonar.core.dto.ServerConfigDto;
import ru.georgii.fonar.core.dto.UserDto;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.gui.DialogListFragment;

public class MainActivity extends FonarActivity {

    SocketService service;

    Toolbar toolbar;
    ImageView photoImageView;
    TextView titleToolbar;
    TextView subtitleToolbar;
    DialogListFragment dialogsView;

    SharedPreferences prefs;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_people:
                intent = new Intent(this, PeopleActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dialogs);

        dialogsView = (DialogListFragment) getSupportFragmentManager().findFragmentById(R.id.userListFragment);
        toolbar = findViewById(R.id.my_toolbar);
        photoImageView = findViewById(R.id.photoImageView);
        titleToolbar = findViewById(R.id.titleToolbar);
        subtitleToolbar = findViewById(R.id.subtitleToolbar);

        toolbar.inflateMenu(R.menu.main);
        toolbar.setEnabled(false);
        setSupportActionBar(toolbar);

    }

    @Override
    void onServiceConnected(SocketService service) {
        (new Thread() {
            public void run() {

                MainActivity.this.service = service;
                toolbar.setEnabled(true);

                try {

                    ServerConfigDto cachedServerConfig = new ServerConfigDto();
                    cachedServerConfig.server_name = service.getServerManager().requireCurrentServer().getCachedName();
                    updateUserHeader(UserIdentity.getInstance(getApplicationContext()), cachedServerConfig);

                    UserIdentity user = UserIdentity.getInstance(getApplicationContext());

                    UserDto profile = service.getServerManager().requireCurrentServer().getRestClient(user).getMe();
                    ServerConfigDto serverConfig = service.getServerManager().requireCurrentServer().getConfiguration();

                    user.updateFromServer(profile);
                    updateUserHeader(user, serverConfig);

                    dialogsView.setServer(service.getServerManager().requireCurrentServer(), service.getUserIdentity());

                } catch (Exception e) {
                    e.printStackTrace();
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                }

            }
        }).start();
    }

    void updateUserHeader(UserIdentity user, ServerConfigDto serverConfig) {
        if (user == null) {
            return;
        }
        if (serverConfig == null) {
            serverConfig = new ServerConfigDto();
            serverConfig.server_name = "";
        }
        ServerConfigDto finalServerConfig = serverConfig;
        runOnUiThread(() -> {
            titleToolbar.setText(user.getVisibleUsername());
            subtitleToolbar.setText(user.getAddress(finalServerConfig));
            photoImageView.setImageBitmap(user.getAvatar());
        });
    }


}