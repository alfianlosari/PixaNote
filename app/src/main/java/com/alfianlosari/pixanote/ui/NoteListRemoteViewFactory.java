package com.alfianlosari.pixanote.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.alfianlosari.pixanote.R;
import com.squareup.picasso.Picasso;

import java.io.IOException;

/**
 * Created by alfianlosari on 28/02/18.
 */

public class NoteListRemoteViewFactory implements RemoteViewsService.RemoteViewsFactory {

    Context mContext;

    public NoteListRemoteViewFactory(Context context) {
        this.mContext = context;
    }


    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String noteText = pref.getString(MainActivity.NOTE_TEXT_KEY, null);
        if (noteText == null) {
            return 0;
        }
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews views =  new RemoteViews(mContext.getPackageName(), R.layout.item_note_widget);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String noteId = pref.getString(MainActivity.NOTE_ID_KEY, null);
        String noteText = pref.getString(MainActivity.NOTE_TEXT_KEY, null);
        String pictureUrl = pref.getString(MainActivity.NOTE_URL_KEY,null);
        views.setTextViewText(R.id.text, noteText);

        Bundle extras = new Bundle();
        extras.putString(mContext.getString(R.string.picture_id_key), noteId);
        Intent fillIntent = new Intent();
        fillIntent.putExtras(extras);
        views.setOnClickFillInIntent(R.id.text, fillIntent);
        views.setOnClickFillInIntent(R.id.image, fillIntent);


        if (pictureUrl != null) {
            try {
                Bitmap b = Picasso.with(mContext).load(pictureUrl).get();
                views.setImageViewBitmap(R.id.image, b);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
