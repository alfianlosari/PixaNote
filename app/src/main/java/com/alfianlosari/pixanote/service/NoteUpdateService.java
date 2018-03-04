package com.alfianlosari.pixanote.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.alfianlosari.pixanote.R;
import com.alfianlosari.pixanote.ui.NoteWidget;

/**
 * Created by alfianlosari on 28/02/18.
 */

public class NoteUpdateService extends IntentService {

    public static final String ACTION_UPDATE_NOTE_SELECTED = "com.alfianlosari.pixanote.action_note_selected";

    public NoteUpdateService() {
        super("RecipeListIntentService");
    }

    public static void startActionUpdateNoteWidgets(Context context) {
        Intent intent = new Intent(context, NoteUpdateService.class);
        intent.setAction(ACTION_UPDATE_NOTE_SELECTED);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_NOTE_SELECTED.equals(action)) {
                handleUpdateNoteWidget();
            }
        }
    }

    private void handleUpdateNoteWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, NoteWidget.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view);
        NoteWidget.updateNoteWidget(this, appWidgetManager, appWidgetIds);

    }
}
