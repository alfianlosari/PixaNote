package com.alfianlosari.pixanote.service;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.alfianlosari.pixanote.ui.NoteListRemoteViewFactory;

/**
 * Created by alfianlosari on 28/02/18.
 */

public class NoteListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new NoteListRemoteViewFactory(getApplicationContext());
    }
}
