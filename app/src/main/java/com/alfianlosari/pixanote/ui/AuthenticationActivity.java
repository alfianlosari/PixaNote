package com.alfianlosari.pixanote.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alfianlosari.pixanote.R;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AuthenticationActivity extends AppCompatActivity {

    private final int RC_SIGN_IN = 123;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        mProgressBar = findViewById(R.id.pb_loading);
        if (!isUserSignedIn()) {
            signIn();
        } else {
            launchMainActivity();
        }
    }

    private void launchMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isUserSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    private void signIn() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build()
        );
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false, true)
                        .setAvailableProviders(providers)
                        .build()
                , RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_OK && user != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            String uid = user.getUid();
            String name = user.getDisplayName();
            String avatar = "";
            if (user.getPhotoUrl() != null) {
                avatar = user.getPhotoUrl().toString();
            }

            HashMap<String, Object> userInfo = new HashMap<>();
            userInfo.put("avatar", avatar);
            userInfo.put("name", name);
            userInfo.put("uid", uid);

            FirebaseDatabase.getInstance().getReference("users/" + uid)
                    .setValue(userInfo)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            mProgressBar.setVisibility(View.GONE);
                            launchMainActivity();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mProgressBar.setVisibility(View.GONE);
                            Toast.makeText(AuthenticationActivity.this, getString(R.string.login_error), Toast.LENGTH_SHORT).show();
                            signOut();
                            signIn();
                        }
                    });

        } else {
            Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_SHORT).show();
            signIn();

        }
    }
}
