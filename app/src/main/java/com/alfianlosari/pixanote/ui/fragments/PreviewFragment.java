package com.alfianlosari.pixanote.ui.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.alfianlosari.pixanote.R;

public class PreviewFragment extends Fragment {

    private String mNoteText;
    private TextView mTextView;
    private ImageView mImageView;
    private Bitmap mBitmap;

    private static final String NOTE_TEXT_KEY = "NOTE_TEXT_KEY";
    private static final String NOTE_IMAGE_DATA = "NOTE_IMAGE_DATA";

    public PreviewFragment() {}

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public String getNoteText() {
        return mNoteText;
    }

    public static PreviewFragment newInstance(String noteText, byte[] imageData) {
        Bundle arguments = new Bundle();
        arguments.putString(NOTE_TEXT_KEY, noteText);
        arguments.putByteArray(NOTE_IMAGE_DATA, imageData);
        PreviewFragment fragment = new PreviewFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Bundle arguments = getArguments();
            mNoteText = arguments.getString(NOTE_TEXT_KEY);
            byte[] data = arguments.getByteArray(NOTE_IMAGE_DATA);
            mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_preview, container, false);
        mTextView = rootView.findViewById(R.id.text_view);
        mImageView = rootView.findViewById(R.id.image_view);
        mTextView.setText(mNoteText);
        mImageView.setImageBitmap(mBitmap);
        return rootView;
    }
}
