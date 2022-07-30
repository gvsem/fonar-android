package ru.georgii.fonar;

import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import ru.georgii.fonar.core.api.FonarRestClient;
import ru.georgii.fonar.core.api.callback.FonarCallback;
import ru.georgii.fonar.core.dto.MessageDto;
import ru.georgii.fonar.core.dto.UserDto;
import ru.georgii.fonar.core.exception.FonarServerException;
import ru.georgii.fonar.core.message.Message;
import ru.georgii.fonar.core.server.Server;

public class DialogActivity extends FonarActivity implements FonarCallback {

    Date lastTypingDate = null;
    SocketService service;
    FonarRestClient api;
    UserDto profile;
    private Toolbar toolbar;
    private EditText messageField;
    private Button sendButton;
    private TextView bioTextview;
    private TextView usernameTextview;
    private ImageView photoImageView;
    private RecyclerView messagesView;
    private MessageListAdapter messageAdapter;

    @Override
    public void messageReceived(Message m) {
        if (Objects.equals(m.fromUserId, m.toUserId)) {
            return;
        }
        runOnUiThread(() -> {
            messageAdapter.messages.add(0, m);
            messageAdapter.notifyItemInserted(0);
            messagesView.scrollToPosition(0);
        });
    }

    @Override
    public void messageSeen(Long messageId, Long userId) {
        // todo: trivial. replace with hash structure
        int position = -1;
        for (int i = 0; i < messageAdapter.messages.size(); i++) {
            if (messageAdapter.messages.get(i).id == messageId) {
                messageAdapter.messages.get(i).seen = true;
                position = i;
                break;
            }
        }
        if (position != -1) {
            int finalPosition = position;
            runOnUiThread(() -> {
                messageAdapter.notifyItemChanged(finalPosition);
            });
        }
    }

    @Override
    public void typing(Long uid) {
        lastTypingDate = new Date();
        runOnUiThread(() -> {
            bioTextview.setText(R.string.typing_tip);
        });
        (new Thread() {
            public synchronized void run() {
                while (lastTypingDate != null) {
                    try {
                        if ((new Date()).getTime() - ((lastTypingDate != null) ? lastTypingDate.getTime() : 0) >= 10000) {
                            untyping(uid);
                            break;
                        }
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void untyping(Long uid) {
        lastTypingDate = null;
        setSubtitle();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        messagesView = findViewById(R.id.messagesView);
        messageField = findViewById(R.id.messageField);
        photoImageView = findViewById(R.id.photoImageView);
        bioTextview = findViewById(R.id.subtitleToolbar);
        usernameTextview = findViewById(R.id.titleToolbar);
        sendButton = findViewById(R.id.sendButton);

        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.logo_text), PorterDuff.Mode.SRC_ATOP);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(false);
        mLayoutManager.setReverseLayout(true);
        messagesView.setLayoutManager(mLayoutManager);

    }

    @Override
    void onServiceConnected(SocketService service) {
        (new Thread() {
            public void run() {
                try {

                    DialogActivity.this.service = service;
                    Server server = service.getServerManager().requireCurrentServer();
                    DialogActivity.this.api = server.getRestClient(service.getUserIdentity());

                    Long userId = getIntent().getExtras().getLong("uId");
                    DialogActivity.this.profile = api.getUser(userId);
                    server.getSocketGateway(service.getUserIdentity()).subscribeForUser(userId, DialogActivity.this);

                    setSubtitle();

                    messageAdapter = new MessageListAdapter(server);
                    messageAdapter.getPreviousMessages();

                    prepareUI();

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(DialogActivity.this, getString(R.string.error_message_not_sent), Toast.LENGTH_SHORT).show();
                        sendButton.setEnabled(true);
                    });
                }
            }
        }).start();
    }

    void setSubtitle() {
        (new Thread() {
            public void run() {
                try {
                    String cachedName = service.getServerManager().requireCurrentServer().getCachedName();
                    runOnUiThread(() -> {
                        bioTextview.setText(profile.getAddress(cachedName));
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void prepareUI() {

        messagesView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(-1)) {
                    (new Thread() {
                        public void run() {
                            messageAdapter.getPreviousMessages();
                        }
                    }).start();
                }
            }
        });

        sendButton.setOnClickListener(this::onSendButtonClicked);

        messageField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                DialogActivity.this.onTypingEventFired(messageField, true);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() == 0) {
                    DialogActivity.this.onTypingEventFired(messageField, false);
                }
            }
        });

        setSubtitle();

        runOnUiThread(() -> {
            usernameTextview.setText(profile.getVisibleUsername());

            if (profile.getAvatarBytes() != null) {
                photoImageView.setImageBitmap(BitmapFactory.decodeByteArray(profile.getAvatarBytes(), 0, profile.getAvatarBytes().length));
            }


            messagesView.setAdapter(messageAdapter);
            sendButton.setEnabled(true);
        });

    }

    void onSendButtonClicked(View v) {
        String message = messageField.getText().toString();
        if (message.length() == 0) {
            return;
        }
        sendButton.setEnabled(false);
        (new Thread() {
            public void run() {
                try {
                    Message m = api.sendMessage(profile.id, new MessageDto(message));
                    runOnUiThread(() -> {
                        messageAdapter.messages.add(0, m);
                        messageAdapter.notifyItemInserted(0);
                        messagesView.scrollToPosition(0);
                        messageField.setText("");
                        sendButton.setEnabled(true);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(DialogActivity.this, getString(R.string.error_message_not_sent), Toast.LENGTH_SHORT).show();
                        sendButton.setEnabled(true);
                    });
                }
            }
        }).start();
    }

    void onTypingEventFired(View v, boolean b) {
        new Thread() {
            public void run() {
                try {
                    if (b) {
                        service.getServerManager().requireCurrentServer().getSocketGateway(service.getUserIdentity()).notifyTypingStart(profile.id);
                    } else {
                        service.getServerManager().requireCurrentServer().getSocketGateway(service.getUserIdentity()).notifyTypingStopped(profile.id);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FonarServerException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder> {

        final Server server;
        final List<Message> messages = new ArrayList<>();
        int offset = 0;
        boolean endReached = false;

        public MessageListAdapter(Server server) throws IOException {
            this.server = server;
        }

        public synchronized void getPreviousMessages() {
            if (endReached) {
                return;
            }

            try {
                List<Message> d = api.getMessages(profile.id, 20L, (long) offset);

                if (d.size() == 0) {
                    endReached = true;
                }

                messages.addAll(d);

                runOnUiThread(() -> {
                    this.notifyItemRangeInserted(offset, d.size());
                });

                offset += d.size();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(DialogActivity.this, getString(R.string.error_failed_loading_messages), Toast.LENGTH_SHORT).show();
                });
            }

        }

        @Override
        public MessageListAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.activity_dialogs_item, viewGroup, false);
            return new MessageListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageListAdapter.ViewHolder viewHolder, final int position) {
            viewHolder.setMessage(messages.get(position));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private final LinearLayout messageView;
            private final TextView messageTextview;
            private final TextView dateTextview;

            Long messageId;

            public ViewHolder(View view) {
                super(view);
                messageView = view.findViewById(R.id.messageView);
                messageTextview = view.findViewById(R.id.messageTextview);
                dateTextview = view.findViewById(R.id.dateTextview);
            }

            public void setMessage(Message c) {
                if (c == null) {
                    throw new RuntimeException("Message is null.");
                }

                messageId = c.id;
                messageTextview.setText(c.text);
                dateTextview.setText(DateUtils.getRelativeTimeSpanString(c.date.getTime(),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

                if (Objects.equals(c.toUserId, profile.id)) {
                    messageView.setGravity(Gravity.END);
                } else {
                    messageView.setGravity(Gravity.START);
                }

                if (!c.seen && (Objects.equals(c.fromUserId, profile.id))) {
                    (new Thread() {
                        public void run() {
                            try {
                                server.getSocketGateway(service.getUserIdentity()).seenMessage(messageId, c.fromUserId);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    c.seen = true;
                }

            }

        }

    }


}