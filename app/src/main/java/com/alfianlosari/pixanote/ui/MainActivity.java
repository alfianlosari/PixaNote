package com.alfianlosari.pixanote.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.alfianlosari.pixanote.R;
import com.alfianlosari.pixanote.adapter.fragments.MainActivityFragmentPagerAdapter;
import com.alfianlosari.pixanote.service.NoteUpdateService;
import com.alfianlosari.pixanote.ui.fragments.LatestNoteListFragment;
import com.alfianlosari.pixanote.ui.fragments.MyNoteListFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    public static String NOTE_TEXT_KEY = "NOTE_TEXT_KEY";
    public static String NOTE_ID_KEY = "NOTE_ID_KEY";
    public static String NOTE_URL_KEY = "NOTE_URL_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        MainActivityFragmentPagerAdapter adapter = new MainActivityFragmentPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new LatestNoteListFragment(), getString(R.string.latest));
        adapter.addFragment(new MyNoteListFragment(), getString(R.string.my_note));
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        switch (itemId) {

            case R.id.action_logout:
                FirebaseAuth.getInstance().signOut();
                SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                prefEditor.clear();
                prefEditor.apply();

                NoteUpdateService.startActionUpdateNoteWidgets(this);
                Intent intent = new Intent(this, AuthenticationActivity.class);
                startActivity(intent);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onCreateClicked(View view) {
        Intent intent = new Intent(this, SnapActivity.class);
        startActivity(intent);
    }

}
