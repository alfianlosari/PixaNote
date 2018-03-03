package com.alfianlosari.pixanote.ui.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alfianlosari.pixanote.R;
import com.alfianlosari.pixanote.ui.camera.CameraSource;
import com.alfianlosari.pixanote.ui.camera.CameraSourcePreview;
import com.alfianlosari.pixanote.ui.camera.GraphicOverlay;
import com.alfianlosari.pixanote.ui.camera.OcrDetectorProcessor;
import com.alfianlosari.pixanote.ui.camera.OcrGraphic;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class SnapFragment extends Fragment {

    private static final String TAG = "SnapFragment";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private OcrDetectorProcessor mDetectorProcessor;

    public SnapFragment() {}

    public CameraSourcePreview getCameraSourcePreview() {
        return mPreview;
    }

    public CameraSource getCameraSource() {
        return mCameraSource;
    }

    public OcrDetectorProcessor getDetectorProcessor() {
        return mDetectorProcessor;
    }

    public static SnapFragment newInstance() {
        SnapFragment fragment = new SnapFragment();
        return fragment;
    }

    private AppCompatActivity getCompactActivity() {
        return (AppCompatActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_snap, container, false);

        mPreview = rootView.findViewById(R.id.preview);
        mGraphicOverlay = rootView.findViewById(R.id.graphicOverlay);

        boolean autoFocus = true;
        boolean useFlash = false;

        int rc = ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        return rootView;
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getCompactActivity().getApplicationContext();

        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();

        mDetectorProcessor = new OcrDetectorProcessor(mGraphicOverlay);
        textRecognizer.setProcessor(mDetectorProcessor);

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available");

            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = getCompactActivity().registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(context, getCompactActivity().getString(R.string.low_storage_error), Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }

        }

        mCameraSource = new CameraSource.Builder(context, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setRequestedFps(15)
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                .build();
    }


    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(getCompactActivity(), Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(getCompactActivity(), permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(getCompactActivity(), permissions, RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        boolean autoFocus = true;
        boolean useFlash = false;

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // We have permission, so create the camerasource
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                getCompactActivity().finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getCompactActivity());
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getCompactActivity().getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(getCompactActivity(), code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }
}
