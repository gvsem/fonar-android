package ru.georgii.fonar.gui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.georgii.fonar.DialogActivity;
import ru.georgii.fonar.R;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.message.Dialog;
import ru.georgii.fonar.core.server.Server;

public class DialogListFragment extends Fragment {

    protected RecyclerView mRecyclerView;
    protected ProgressBar progressBar;
    protected TextView emptyPlaceholderView;
    protected DialogListAdapter mAdapter;
    protected LinearLayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_dialogs, container, false);

        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        progressBar = rootView.findViewById(R.id.progress_bar);
        emptyPlaceholderView = rootView.findViewById(R.id.emptyPlaceholderView);

        progressBar.setVisibility(View.VISIBLE);
        emptyPlaceholderView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.GONE);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        getActivity().runOnUiThread(() -> {
                            Intent intent = new Intent(getActivity(), DialogActivity.class);
                            Bundle b = new Bundle();
                            b.putLong("uId", mAdapter.dialogs.get(position).getUser().id);
                            intent.putExtras(b);
                            startActivity(intent);
                        });
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                    }

                })
        );

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    (new Thread() {
                        public void run() {
                            if (mAdapter != null) {
                                mAdapter.getNextDialogs();
                            }
                        }
                    }).start();
                }
            }
        });

        return rootView;
    }

    public void setServer(Server server, UserIdentity identity) throws IOException {

        if (server == null) {
            return;
        }

        mAdapter = new DialogListAdapter(server, identity);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(mAdapter);
            }
        });

        mAdapter.getNextDialogs();

    }

    public class DialogListAdapter extends RecyclerView.Adapter<DialogListAdapter.ViewHolder> {

        final Server server;
        final UserIdentity identity;
        final List<Dialog> dialogs = new ArrayList<>();
        int offset = 0;
        boolean endReached = false;
        public DialogListAdapter(Server s, UserIdentity identity) throws IOException {
            server = s;
            this.identity = identity;
        }

        public synchronized void getNextDialogs() {
            try {
                UserIdentity identity = UserIdentity.getInstance(getActivity().getApplicationContext());
                List<Dialog> d = server.getRestClient(identity).getDialogs(20L, (long) offset);
                if ((d == null) || (d.size() == 0)) {
                    endReached = true;
                    if (offset == 0) {
                        requireActivity().runOnUiThread(() -> {
                            emptyPlaceholderView.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            mRecyclerView.setVisibility(View.GONE);
                        });
                    }
                    return;
                } else {
                    dialogs.addAll(d);
                    requireActivity().runOnUiThread(() -> {
                        this.notifyItemRangeInserted(offset, d.size());
                    });
                    offset += d.size();
                }

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    emptyPlaceholderView.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), getString(R.string.error_failed_loading_dialogs), Toast.LENGTH_SHORT).show();
                });
            }

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.fragment_dialogs_item, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            viewHolder.setConversation(dialogs.get(position));
        }

        @Override
        public int getItemCount() {
            return dialogs.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView usernameTextView;
            private final TextView messageTextView;
            private final TextView badgeTextView;
            private final TextView dateTextView;
            private final ImageView photoImageView;

            public ViewHolder(View view) {
                super(view);
                usernameTextView = view.findViewById(R.id.usernameTextView);
                messageTextView = view.findViewById(R.id.messageTextView);
                badgeTextView = view.findViewById(R.id.badgeTextView);
                dateTextView = view.findViewById(R.id.nicknameTextView);
                photoImageView = view.findViewById(R.id.photoImageView);

            }

            public void setConversation(Dialog c) {
                if (c == null) {
                    throw new RuntimeException("Conversation is null.");
                }

                if (c.getUser() == null) {
                    usernameTextView.setText(getString(R.string.error_invalid_data));
                    return;
                }

                if (c.getAvatarBytes() != null) {
                    photoImageView.setImageBitmap(BitmapFactory.decodeByteArray(c.getAvatarBytes(), 0, c.getAvatarBytes().length));
                }

                if ((c.getUser().firstname != null) && (c.getUser().lastname != null)) {
                    usernameTextView.setText(c.getUser().firstname + " " + c.getUser().lastname);
                } else if (c.getUser().nickname != null) {
                    usernameTextView.setText(c.getUser().nickname);
                } else {
                    usernameTextView.setText(R.string.error_unknown);
                }

                if (c.getLastMessage() != null) {
                    messageTextView.setText(c.getLastMessage().text);
                    dateTextView.setText(DateUtils.getRelativeTimeSpanString(c.getLastMessage().date.getTime(),
                            System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                } else {
                    messageTextView.setText("");
                }

                if (c.getUnreadMessages() == 0) {
                    badgeTextView.setVisibility(View.INVISIBLE);
                } else {
                    badgeTextView.setVisibility(View.VISIBLE);
                }
                badgeTextView.setText(c.getUnreadMessages().toString());

            }

        }

    }

}
