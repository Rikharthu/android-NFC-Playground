package com.example.nfcplayground.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nfcplayground.R;
import com.example.nfcplayground.Utils;

import java.util.Arrays;

public class NfcWriteActivity extends AppCompatActivity
        implements WriteTask.OnWriteStatusListener {
    public static final String TAG = NfcWriteActivity.class.getSimpleName();

    public static final String KEY_NDEF_MESSAGE = "key_ndef_message";
    public static final int NFC_RESULT_ERROR = 1001;
    public static final String RESULT_KEY_ERROR = "result_key_error";

    private NfcAdapter mNfcAdapter;
    private boolean mIsInWriteMode = false;
    private TextView mErrorTv;
    private ImageView mNfcImage;
    private boolean mIsSuccess = false;
    private NdefMessage mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Dialog);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_write);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mErrorTv = findViewById(R.id.error);
        mNfcImage = findViewById(R.id.nfc_image);

        mMessage = getIntent().getParcelableExtra(KEY_NDEF_MESSAGE);
        if (mMessage == null) {
            Intent result = new Intent("com.example.RESULT_ACTION");
            result.putExtra(RESULT_KEY_ERROR, "no ndef message");
            setResult(NFC_RESULT_ERROR, result);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mIsInWriteMode) {
            registerNfcForegroundDispatch();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            if (mIsInWriteMode) {
                unregisterNfcForegroundDispatch();
            }
            if (mIsSuccess) {
                Intent result = new Intent("com.example.RESULT_ACTION");
                setResult(Activity.RESULT_CANCELED, result);
            }
        }
    }

    private void registerNfcForegroundDispatch() {
        Log.d(TAG, "Registering NFC foreground dispatch");
        IntentFilter discoveryFilter = new IntentFilter();
        discoveryFilter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] tagFilters = new IntentFilter[]{discoveryFilter};
        Intent i = new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        mNfcAdapter.enableForegroundDispatch(this, pi, tagFilters, null);
        mIsInWriteMode = true;
    }

    private void unregisterNfcForegroundDispatch() {
        Log.d(TAG, "Unregistering NFC foreground dispatch");
        mNfcAdapter.disableForegroundDispatch(this);
        mIsInWriteMode = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent " + intent.getAction() + ", " + mIsInWriteMode);
        if (mIsInWriteMode &&
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Log.d(TAG, "Time to write NFC");
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (Arrays.asList(tag.getTechList()).contains(Ndef.class.getName())) {
                // Ndef is supported
                Log.d(TAG, "Ndef is supported");
                new WriteTask(mMessage, tag, this).execute();
            } else {
                Log.d(TAG, "Ndef not supported");
                Utils.vibrate(this);
                Toast.makeText(this, "This tag doesn't support NDEF!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onNfcWriteError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Utils.vibrate(this);
    }

    @Override
    public void onNfcWriteSuccess() {
        mIsSuccess = true;
        Utils.vibrate(this);
        Intent result = new Intent("com.example.RESULT_ACTION");
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed");
    }

    private void setError(String error) {
        if (error == null) {
            mErrorTv.setVisibility(View.INVISIBLE);
            mErrorTv.setText("");
        } else {
            mErrorTv.setVisibility(View.VISIBLE);
            mErrorTv.setText(error);
        }
    }
}
