package com.alfianlosari.pixanote.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.alfianlosari.pixanote.R;
import com.alfianlosari.pixanote.adapter.NotePicsFirebaseRecyclerViewAdapter;
import com.alfianlosari.pixanote.model.NotePix;
import com.alfianlosari.pixanote.service.NoteUpdateService;
import com.alfianlosari.pixanote.ui.MainActivity;
import com.alfianlosari.pixanote.ui.NoteDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class MyNoteListFragment extends Fragment
    implements NotePicsFirebaseRecyclerViewAdapter.NotePicsAdapterClickItemListener {

    private static final String LAYOUT_MANAGER_POSITION = "LAYOUT_MANAGER_POSITION";
    private Parcelable layoutSavedPosition;

    private final String TAG = MyNoteListFragment.class.getSimpleName();
    private ProgressBar mProgressBar;
    private boolean isFirstLoad = true;
    private RecyclerView mRecyclerView;
    private NotePicsFirebaseRecyclerViewAdapter mAdapter;
    private NotePix[] mNotes;
    private HashMap<String, Boolean> mUserLikes = new HashMap();


    DatabaseReference mUserPicturesDatabaseRef = FirebaseDatabase.getInstance()
            .getReference("userPics/" + FirebaseAuth.getInstance().getUid());
    DatabaseReference mUserLikesDatabaseRef = FirebaseDatabase.getInstance()
            .getReference("usersLikes/" + FirebaseAuth.getInstance().getUid());

    private ValueEventListener mUserNotesValueEventListener;
    private ValueEventListener mUserLikesValueEventListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_my_note_list, container, false);

        mAdapter = new NotePicsFirebaseRecyclerViewAdapter(this);
        mProgressBar = rootView.findViewById(R.id.pb_loading);
        mRecyclerView = rootView.findViewById(R.id.my_recycler_view);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mUserNotesValueEventListener = mUserPicturesDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                NotePix[] tempPictures = new NotePix[(int)dataSnapshot.getChildrenCount()];
                int i = 0;
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    NotePix note = snapshot.getValue(NotePix.class);
                    tempPictures[i] = note;
                    i++;
                }


                mNotes = tempPictures;
                mAdapter.swapNotes(mNotes);

                if (isFirstLoad) {
                    isFirstLoad = false;
                    mProgressBar.setVisibility(View.GONE);
                    restoreLayoutManagerPosition();

                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
            }
        });

        mUserLikesValueEventListener = mUserLikesDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Boolean> tempHashMap = new HashMap<>();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    tempHashMap.put(snapshot.getKey(), true);
                }
                mUserLikes = tempHashMap;
                mAdapter.swapUserLikes(mUserLikes);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mUserNotesValueEventListener != null) {
            mUserPicturesDatabaseRef.removeEventListener(mUserNotesValueEventListener);
        }

        if (mUserLikesValueEventListener != null) {
            mUserLikesDatabaseRef.removeEventListener(mUserLikesValueEventListener);
        }

    }

    @Override
    public void onNoteClicked(NotePix note) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        prefEditor.putString(MainActivity.NOTE_TEXT_KEY, note.text);
        prefEditor.putString(MainActivity.NOTE_URL_KEY, note.pictureURL);
        prefEditor.putString(MainActivity.NOTE_ID_KEY, note.picId);
        prefEditor.apply();


        NoteUpdateService.startActionUpdateNoteWidgets(getContext());
        Intent intent = new Intent(getActivity(), NoteDetailActivity.class);
        intent.putExtra(getString(R.string.picture_id_key), note.picId);
        startActivity(intent);
    }


    @Override
    public void onLikeClicked(NotePix note) {
        boolean isLike = mUserLikes.containsKey(note.picId);
        if (isLike) {
            FirebaseDatabase.getInstance()
                    .getReference("usersLikes/" + FirebaseAuth.getInstance().getUid() + "/" + note.picId)
                    .removeValue();

            FirebaseDatabase.getInstance()
                    .getReference("likeCount/" + note.picId)
                    .runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Long count = (Long) mutableData.getValue();
                            if (count == null) {
                                count = Long.valueOf(1);
                            }


                            if (count > 0) {
                                mutableData.setValue(count - 1);
                            }

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                        }
                    });
        } else {
            FirebaseDatabase.getInstance()
                    .getReference("usersLikes/" + FirebaseAuth.getInstance().getUid() + "/" + note.picId)
                    .setValue(true);

            FirebaseDatabase.getInstance()
                    .getReference("likeCount/" + note.picId)
                    .runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Long count = (Long) mutableData.getValue();
                            if (count == null) {
                                count = Long.valueOf(0);
                            }

                            mutableData.setValue(count + 1);
                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                        }
                    });




        }
    }


    @Override
    public void onShareClicked(NotePix note) {
        String mimeType = "text/plain";
        String title = "Pixanote";

        Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                .setChooserTitle(title)
                .setSubject(title)
                .setType(mimeType)
                .setText(note.text)
                .getIntent();

        if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(Intent
                    .createChooser(shareIntent, getString(R.string.action_share)));
        }

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(LAYOUT_MANAGER_POSITION, mRecyclerView.getLayoutManager().onSaveInstanceState());
    }


    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            layoutSavedPosition = savedInstanceState.getParcelable(LAYOUT_MANAGER_POSITION);

        }
        super.onViewStateRestored(savedInstanceState);
    }



    private void restoreLayoutManagerPosition() {
        if (layoutSavedPosition != null) {
            mRecyclerView.getLayoutManager().onRestoreInstanceState(layoutSavedPosition);
        }
    }

}
