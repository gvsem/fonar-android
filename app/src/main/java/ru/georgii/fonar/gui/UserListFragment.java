package ru.georgii.fonar.gui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
import ru.georgii.fonar.core.dto.UserDto;
import ru.georgii.fonar.core.identity.UserIdentity;
import ru.georgii.fonar.core.message.User;
import ru.georgii.fonar.core.server.Server;

public class UserListFragment extends Fragment {

    protected RecyclerView mRecyclerView;
    protected ProgressBar progressBar;
    protected UserDtoListAdapter mAdapter;
    protected LinearLayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_dialogs, container, false);
        mRecyclerView = rootView.findViewById(R.id.recyclerView);
        progressBar = rootView.findViewById(R.id.progress_bar);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        getActivity().runOnUiThread(() -> {
                            Intent intent = new Intent(getActivity(), DialogActivity.class);
                            Bundle b = new Bundle();
                            b.putLong("uId", mAdapter.users.get(position).getId());
                            intent.putExtras(b);
                            startActivity(intent);
                        });

                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                        // do whatever
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
                                mAdapter.getNextUsers();
                            }

                        }
                    }).start();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    public void setServer(Server server, UserIdentity identity) throws IOException {
        if (server == null) {
            return;
        }

        mAdapter = new UserDtoListAdapter(server, identity);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(mAdapter);
            }
        });

        mAdapter.getNextUsers();
    }

    public class UserDtoListAdapter extends RecyclerView.Adapter<UserDtoListAdapter.ViewHolder> {

        final Server server;
        final UserIdentity identity;
        final List<User> users = new ArrayList<>();
        int offset = 0;
        boolean endReached = false;
        public UserDtoListAdapter(Server s, UserIdentity identity) throws IOException {
            this.server = s;
            this.identity = identity;
        }

        public synchronized void getNextUsers() {
            if (endReached) {
                return;
            }

            try {
                List<UserDto> d = null;
                d = server.getRestClient(identity).getUsers(20L, (long) offset);

                if ((d == null) || (d.size() == 0)) {
                    endReached = true;
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        mRecyclerView.setVisibility(View.VISIBLE);
                    });
                    return;
                }
                for (UserDto udto : d) {
                    users.add(udto.toUser());
                }
                offset += d.size();

                getActivity().runOnUiThread(() -> {
                    this.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                });

            } catch (IOException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), getString(R.string.error_failed_retrieve_users), Toast.LENGTH_SHORT).show();
                });
            }

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.fragment_users_item, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            viewHolder.setUser(users.get(position));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView usernameTextView;
            private final TextView nicknameTextView;
            private final ImageView photoImageView;
            private final TextView bioTextView;

            public ViewHolder(View view) {
                super(view);
                usernameTextView = (TextView) view.findViewById(R.id.usernameTextView);
                nicknameTextView = (TextView) view.findViewById(R.id.nicknameTextView);
                photoImageView = (ImageView) view.findViewById(R.id.photoImageView);
                bioTextView = (TextView) view.findViewById(R.id.bioTextView);
            }

            public void setUser(User c) {
                if (c == null) {
                    throw new RuntimeException("User is null.");
                }

                if (c.getAvatarBytes() != null) {
                    photoImageView.setImageBitmap(BitmapFactory.decodeByteArray(c.getAvatarBytes(), 0, c.getAvatarBytes().length));
                }

                if ((c.firstname != null) && (c.lastname != null)) {
                    usernameTextView.setText(c.firstname + " " + c.lastname);
                } else {
                    usernameTextView.setText(getString(R.string.error_unknown));
                }

                if (c.getNickname() != null) {
                    nicknameTextView.setText(c.getNickname());
                } else {
                    nicknameTextView.setText("-");
                }

                if (c.getBio() != null) {
                    bioTextView.setText(c.getBio());
                } else {
                    nicknameTextView.setText("");
                }

            }

        }

    }

}
