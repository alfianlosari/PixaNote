package com.alfianlosari.pixanote.adapter;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alfianlosari.pixanote.R;
import com.alfianlosari.pixanote.model.NotePix;
import com.alfianlosari.pixanote.utils.DateUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by alfianlosari on 02/02/18.
 */

public class NotePicsFirebaseRecyclerViewAdapter extends RecyclerView.Adapter<NotePicsFirebaseRecyclerViewAdapter.NotePixViewHolder> {

    private NotePix[] mNotes = new NotePix[0];
    private HashMap<String, Boolean> mUserLikes = new HashMap();
    private NotePicsAdapterClickItemListener mListener;

    public NotePicsFirebaseRecyclerViewAdapter(NotePicsAdapterClickItemListener listener) {
        mListener = listener;
    }

    public void swapNotes(NotePix[] notes) {
        mNotes = notes;
        notifyDataSetChanged();
    }

    public void swapUserLikes(HashMap<String, Boolean> userLikes) {
        mUserLikes = userLikes;
        notifyDataSetChanged();
    }

    @Override
    public NotePixViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_note_card, parent, false);
        return new NotePixViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final NotePixViewHolder holder, int position) {
        NotePix note = mNotes[position];
        holder.mTextView.setText(note.text);
        holder.mAuthorTextView.setText(note.name);
        holder.mPublishedAtTextView.setText(DateUtils.dateFormat.format(new Date(note.timestamp)));
        holder.mProgressBar.setVisibility(View.VISIBLE);


        Picasso.with(holder.itemView.getContext())
                .load(note.avatar)
                .fit()
                .centerCrop()
                .into(holder.mAvatarImageView);

        Picasso.with(holder.itemView.getContext())
                .load(note.pictureURL)
                .fit()
                .centerCrop()
                .into(holder.mImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (holder.mProgressBar != null) {
                            holder.mProgressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError() {
                        if (holder.mProgressBar != null) {
                            holder.mProgressBar.setVisibility(View.GONE);
                        }

                    }
                });

        holder.itemView.setTag(position);
        Boolean isUserLike = mUserLikes.containsKey(note.picId);

        Resources res = holder.itemView.getResources();
        int color = (isUserLike == true) ?  res.getColor(R.color.colorAccent) : res.getColor(R.color.button_grey);
        holder.mFavoriteButton.setImageTintList(ColorStateList.valueOf(color));

    }

    @Override
    public int getItemCount() {
        return mNotes.length;
    }

    class NotePixViewHolder extends RecyclerView.ViewHolder {

        ImageView mImageView;
        ImageView mAvatarImageView;
        TextView mTextView;
        TextView mAuthorTextView;
        TextView mPublishedAtTextView;
        ImageButton mFavoriteButton;

        ProgressBar mProgressBar;

        public NotePixViewHolder(final View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.card_image);
            mAvatarImageView = itemView.findViewById(R.id.list_avatar);
            mAuthorTextView = itemView.findViewById(R.id.author);
            mPublishedAtTextView = itemView.findViewById(R.id.published);
            mTextView = itemView.findViewById(R.id.card_text);
            mProgressBar = itemView.findViewById(R.id.progressBar);

            Button actionButton = itemView.findViewById(R.id.action_button);
            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = (int) itemView.getTag();
                    NotePix note = mNotes[position];
                    mListener.onNoteClicked(note);
                }
            });

            ImageButton shareButton = itemView.findViewById(R.id.share_button);
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = (int) itemView.getTag();
                    NotePix note = mNotes[position];
                    mListener.onShareClicked(note);
                }
            });

            ImageButton favoriteButton = itemView.findViewById(R.id.favorite_button);
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = (int) itemView.getTag();
                    NotePix note = mNotes[position];
                    mListener.onLikeClicked(note);
                }
            });
            mFavoriteButton = favoriteButton;
        }
    }

    public interface NotePicsAdapterClickItemListener {
        void onNoteClicked(NotePix note);
        void onLikeClicked(NotePix note);
        void onShareClicked(NotePix note);
    }

}
