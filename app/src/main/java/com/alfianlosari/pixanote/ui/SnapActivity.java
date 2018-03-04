package com.alfianlosari.pixanote.ui;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alfianlosari.pixanote.R;
import com.alfianlosari.pixanote.ui.camera.CameraSource;
import com.alfianlosari.pixanote.ui.fragments.PreviewFragment;
import com.alfianlosari.pixanote.ui.fragments.SnapFragment;
import com.alfianlosari.pixanote.utils.Exif;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapActivity extends AppCompatActivity {

    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();

    private String generateUniqueKey() {
        return databaseRef.push().getKey();
    }

    View mRootView;
    ProgressBar mProgressBar;
    FloatingActionButton mCameraFab;
    FloatingActionButton mUploadFab;
    public final String PREVIEW_FRAGMENT_TAG = "PREVIEW_FRAGMENT_TAG";


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    public SnapFragment getCurrentSnapFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        SnapFragment snapFragment = null;

        for (Fragment fragment: fragments) {
            if (fragment instanceof SnapFragment) {
                snapFragment = (SnapFragment) fragment;
                break;
            }
        }
        return snapFragment;
    }

    public PreviewFragment getCurrentPreviewFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        PreviewFragment previewFragment = null;

        for (Fragment fragment: fragments) {
            if (fragment instanceof PreviewFragment) {
                previewFragment = (PreviewFragment) fragment;
                break;
            }
        }
        return previewFragment;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snap);

        mRootView = findViewById(R.id.root_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mCameraFab = findViewById(R.id.fab);
        mUploadFab = findViewById(R.id.cloud_fab);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, SnapFragment.newInstance())
                    .commit();
        }
    }

    public void onFabClicked(View view) {

        SnapFragment snapFragment = getCurrentSnapFragment();
        PreviewFragment previewFragment = getCurrentPreviewFragment();

        if (snapFragment != null) {
            final String text = snapFragment.getDetectorProcessor().mDetectedText;
            if (TextUtils.isEmpty(text)) {
                Snackbar.make(mRootView, getString(R.string.no_text_is_available), Snackbar.LENGTH_SHORT).show();
            } else {
                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getString(R.string.processing_image_to_text));
                progressDialog.setCancelable(false);
                progressDialog.show();
                snapFragment.getCameraSource().takePicture(new CameraSource.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                }, new CameraSource.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data) {
                        int orientation = Exif.getOrientation(data);
                        Bitmap  bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        switch(orientation) {
                            case 90:
                                bitmap= rotateImage(bitmap, 90);

                                break;
                            case 180:
                                bitmap= rotateImage(bitmap, 180);

                                break;
                            case 270:
                                bitmap= rotateImage(bitmap, 270);

                                break;
                            case 0:
                                // if orientation is zero we don't need to rotate this

                            default:
                                break;
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
                        byte[] compressed = baos.toByteArray();

                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, PreviewFragment.newInstance(text, compressed))
                                .addToBackStack(PREVIEW_FRAGMENT_TAG)
                                .commit();

                        progressDialog.dismiss();
                        mCameraFab.setVisibility(View.INVISIBLE);
                        mUploadFab.setVisibility(View.VISIBLE);
                    }
                });
            }
        } else if (previewFragment != null && previewFragment.getBitmap() != null && !TextUtils.isEmpty(previewFragment.getNoteText())) {
            final Bitmap bitmap = previewFragment.getBitmap();
            final String text = previewFragment.getNoteText();
            final String noteKey = generateUniqueKey();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                Toast.makeText(this, getString(R.string.user_is_not_signed_in), Toast.LENGTH_SHORT).show();
                return;
            }

            final String uid = user.getUid();
            final String name = user.getDisplayName();
            final String avatar = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] data = baos.toByteArray();

            StorageReference imageRef = storageRef
                    .child("images/" + uid + "/" + noteKey + ".jpg");
            UploadTask uploadTask = imageRef.putBytes(data);

            final ProgressDialog horizontalProgressDialog = new ProgressDialog(this);
            horizontalProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            horizontalProgressDialog.setMessage(getString(R.string.uploading_note));
            horizontalProgressDialog.setCancelable(false);
            horizontalProgressDialog.setMax(100);
            horizontalProgressDialog.show();

            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    long transferred = taskSnapshot.getBytesTransferred();
                    long totalBytes = taskSnapshot.getTotalByteCount();
                    double progress = ((double)transferred / (double)totalBytes) * 100;
                    horizontalProgressDialog.setProgress((int)progress);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    horizontalProgressDialog.dismiss();
                    Snackbar.make(mRootView, getString(R.string.upload_failed) + e.getMessage(), Snackbar.LENGTH_SHORT).show();

                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    String downloadURL = taskSnapshot.getDownloadUrl().toString();
                    Map<String, Object> picsValues = new HashMap<>();
                    DatabaseReference picsRef = databaseRef.child("pics/" + noteKey);

                    picsValues.put("picId", noteKey);
                    picsValues.put("pictureURL", downloadURL);
                    picsValues.put("name", name);
                    picsValues.put("avatar", avatar);
                    picsValues.put("text", text);
                    picsValues.put("timestamp", System.currentTimeMillis());
                    picsValues.put("uid", uid);

                    picsRef.setValue(picsValues);

                    DatabaseReference likeCountRef = FirebaseDatabase.getInstance().getReference("likeCount" + "/" + noteKey);
                    likeCountRef.setValue(0);

                    DatabaseReference userPicRef = databaseRef.child("userPics/" + uid + "/" + noteKey);
                    userPicRef.setValue(picsValues)
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    horizontalProgressDialog.dismiss();
                                    Snackbar.make(mRootView, getString(R.string.failed_to_write) + e.getMessage(), Snackbar.LENGTH_SHORT).show();

                                }
                            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            horizontalProgressDialog.dismiss();
                            finish();
                        }
                    });
                }
            });
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(),   source.getHeight(), matrix,
                true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mCameraFab.setVisibility(View.VISIBLE);
        mUploadFab.setVisibility(View.INVISIBLE);
        getSupportFragmentManager()
                .popBackStackImmediate();
    }
}
