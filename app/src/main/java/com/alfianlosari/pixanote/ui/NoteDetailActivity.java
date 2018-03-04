package com.alfianlosari.pixanote.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alfianlosari.pixanote.R;
import com.alfianlosari.pixanote.model.NotePix;
import com.alfianlosari.pixanote.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Date;

public class NoteDetailActivity extends AppCompatActivity {

    private static final String SCROLL_POSITION = "SCROLL_POSITION";
    private int[] scrollPositions;



    private final String TAG = NoteDetailActivity.class.getSimpleName();
    private TextView mContentTextView;
    private TextView mAuthorTextView;
    private TextView mPublishedAtTextView;
    private FloatingActionButton mFab;
    private ImageView mImageView;
    private ImageView mAvatarImageView;
    private String mPictureId;
    private NestedScrollView mScrollView;
    private ImageButton mShareButton;
    private ImageButton mFavoriteButton;
    private TextView mLikeText;
    private ProgressBar mContentProgressBar;
    private long likeCount;
    private NotePix mNote;
    private boolean isLike = false;
    private boolean isFirstLoad = true;
    ValueEventListener mUserLikesValueEventListener;
    ValueEventListener mLikeCountValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        mScrollView = findViewById(R.id.nested_scroll_view);
        mContentTextView = findViewById(R.id.content);
        mAuthorTextView = findViewById(R.id.author);
        mContentProgressBar = findViewById(R.id.pb_loading);
        mPublishedAtTextView = findViewById(R.id.published_date);
        mImageView = findViewById(R.id.image);
        mAvatarImageView = findViewById(R.id.list_avatar);
        mPictureId = getIntent().getStringExtra(getString(R.string.picture_id_key));
        mShareButton = findViewById(R.id.share_button);
        mFavoriteButton = findViewById(R.id.favorite_button);
        mLikeText = findViewById(R.id.like_text);
        mFab = findViewById(R.id.fab);

        mFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLike) {
                    FirebaseDatabase.getInstance()
                            .getReference("usersLikes/" + FirebaseAuth.getInstance().getUid() + "/" + mPictureId)
                            .removeValue();

                    FirebaseDatabase.getInstance()
                            .getReference("likeCount/" + mPictureId)
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
                            .getReference("usersLikes/" + FirebaseAuth.getInstance().getUid() + "/" + mPictureId)
                            .setValue(true);

                    FirebaseDatabase.getInstance()
                            .getReference("likeCount/" + mPictureId)
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
        });
        Toolbar toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(" ");
    }

    @Override
    protected void onResume() {
        super.onResume();

        DatabaseReference mPicDatabaseRef = FirebaseDatabase.getInstance().getReference("pics/" + mPictureId);
        mPicDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mNote = dataSnapshot.getValue(NotePix.class);

                mFab.setVisibility(View.VISIBLE);
                mContentTextView.setText(mNote.text);
                mAuthorTextView.setText(mNote.name);
                Date publishedDate = new Date(mNote.timestamp);
                mPublishedAtTextView.setText(DateUtils.dateFormat.format(publishedDate));
                Picasso.with(NoteDetailActivity.this)
                        .load(mNote.pictureURL)
                        .fit()
                        .centerCrop()
                        .into(mImageView);

                Picasso.with(NoteDetailActivity.this)
                        .load(mNote.avatar)
                        .fit()
                        .centerCrop()
                        .into(mAvatarImageView);

                if (isFirstLoad) {
                    isFirstLoad = false;
                    mContentProgressBar.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
            }
        });


        DatabaseReference mPicLikeCountDatabaseRef = FirebaseDatabase.getInstance().getReference("likeCount/" + mPictureId);
        mLikeCountValueEventListener =  mPicLikeCountDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                likeCount = (Long) dataSnapshot.getValue();
                mLikeText.setText(getResources().getQuantityString(R.plurals.likes, (int) likeCount, (int) likeCount));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
            }
        });

        DatabaseReference mUserLikeDatabasRef = FirebaseDatabase.getInstance()
                .getReference("usersLikes/" + FirebaseAuth.getInstance().getUid() + "/" + mPictureId);


        mUserLikesValueEventListener = mUserLikeDatabasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    isLike = false;
                } else {
                    isLike = (boolean) dataSnapshot.getValue();
                }
                Resources res = getResources();
                int color = (isLike == true) ?  res.getColor(R.color.colorAccent) : res.getColor(R.color.white);
                mFavoriteButton.setImageTintList(ColorStateList.valueOf(color));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
            }
        });
    }

    public void onShareClicked(View view) {
        String mimeType = "text/plain";
        String title = "Pixanote";

        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setChooserTitle(title)
                .setSubject(title)
                .setType(mimeType)
                .setText(mContentTextView.getText())
                .getIntent();

        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent
                    .createChooser(shareIntent, getString(R.string.action_share)));
        }
    }


    @Override
    protected void onPause() {

        super.onPause();
        if (mUserLikesValueEventListener != null) {
            DatabaseReference mUserLikeDatabasRef = FirebaseDatabase.getInstance()
                    .getReference("usersLikes/" + FirebaseAuth.getInstance().getUid() + "/" + mPictureId);
            mUserLikeDatabasRef.removeEventListener(mUserLikesValueEventListener);
        }

        if (mLikeCountValueEventListener != null) {
            DatabaseReference mPicLikeCountDatabaseRef = FirebaseDatabase.getInstance().getReference("likeCount/" + mPictureId);
            mPicLikeCountDatabaseRef.removeEventListener(mLikeCountValueEventListener);
        }
    }


}
