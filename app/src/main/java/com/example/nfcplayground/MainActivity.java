package com.example.nfcplayground;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.nfcplayground.nfc.NfcWriteActivity;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_CODE_WRITE_NFC = 1;

    private NfcAdapter mNfcAdapter;
    private boolean mIsInWriteMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        findViewById(R.id.write_btn).setOnClickListener((view) -> onWriteButtonClicked());
    }

    private void onWriteButtonClicked() {
        byte[] urlBytes = buildUrlBytes("museumapp://www.museumapp.com/artifact/5a48e09e57cdf80014fe1be5");

        NdefRecord urlRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_URI,
                new byte[]{},
                urlBytes);

        // More info: https://www.cardsys.dk/download/NFC_Docs/NFC%20Text%20Record%20Type%20Definition%20Technical%20Specification.pdf
        byte[] textBytes = "Hello, world!".getBytes();
        byte[] textMessageBytes = new byte[textBytes.length + 3];
        textMessageBytes[0] = 0x02; // UTF-8 status byte
        // English language code
        textMessageBytes[1] = 'e';
        textMessageBytes[2] = 'n';
        System.arraycopy(textBytes, 0, textMessageBytes, 3, textBytes.length);
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,
                new byte[]{},
                textBytes);
        Log.d(TAG, "Writing message " + new String(textMessageBytes));
        NdefMessage message = new NdefMessage(new NdefRecord[]{urlRecord, textRecord});

        Intent nfcWriteIntent = new Intent(this, NfcWriteActivity.class);
        nfcWriteIntent.putExtra(NfcWriteActivity.KEY_NDEF_MESSAGE, message);
        startActivityForResult(nfcWriteIntent, REQUEST_CODE_WRITE_NFC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODE_WRITE_NFC == requestCode) {
            Log.d(TAG, "result from nfc write, " + resultCode);
            if (Activity.RESULT_OK == resultCode) {
                Toast.makeText(this, "NFC write success", Toast.LENGTH_SHORT).show();
            } else if (NfcWriteActivity.NFC_RESULT_ERROR == resultCode) {
                String error = null;
                if (data != null) {
                    error = data.getStringExtra(NfcWriteActivity.RESULT_KEY_ERROR);
                }
                if (error == null) {
                    error = "NFC write operation failed";
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "NFC write failure", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private byte[] buildUrlBytes(String url) {
        byte prefixByte = 0;
        String subset = url;
        int bestPrefixLength = 0;

        for (int i = 0; i < Constants.PREFIXES.length; i++) {
            String prefix = Constants.PREFIXES[i];

            if (url.startsWith(prefix) && prefix.length() > bestPrefixLength) {
                prefixByte = (byte) (i + 1);
                bestPrefixLength = prefix.length();
                subset = url.substring(bestPrefixLength);
            }
        }

        final byte[] subsetBytes = subset.getBytes();
        final byte[] result = new byte[subsetBytes.length + 1];

        result[0] = prefixByte;
        System.arraycopy(subsetBytes, 0, result, 1, subsetBytes.length);

        return result;
    }
}
