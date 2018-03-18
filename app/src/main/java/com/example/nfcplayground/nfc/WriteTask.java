package com.example.nfcplayground.nfc;

import android.app.Activity;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

public class WriteTask extends AsyncTask<Void, Void, Void> {
    public static final String TAG = WriteTask.class.getSimpleName();

    interface OnWriteStatusListener {
        void onNfcWriteError(String message);

        void onNfcWriteSuccess();
    }

    private Activity mmHost;
    private NdefMessage mmMessage;
    private Tag mmTag;
    private String mmError;
    private OnWriteStatusListener mmListener;

    public WriteTask(NdefMessage mmMessage, Tag mmTag, OnWriteStatusListener listener) {
        this.mmMessage = mmMessage;
        this.mmTag = mmTag;
        this.mmListener = listener;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        int size = mmMessage.toByteArray().length;

        Ndef ndef = Ndef.get(mmTag);

        if (ndef == null) {
            Log.d(TAG, "Tag does not directly support Ndef, trying to format");
            // Tag does not directly support NDEF, try formatting it to NDEF
            NdefFormatable formatable = NdefFormatable.get(mmTag);

            if (formatable != null) {
                try {
                    formatable.connect();
                    formatable.format(mmMessage);
                } catch (IOException e) {
                    mmError = "Ndef formattable IO exception";
                } catch (FormatException e) {
                    mmError = "Could not format to Ndef";
                } finally {
                    try {
                        formatable.close();
                    } catch (IOException e) {
                        mmError = "Could not close Ndef formattable tag";
                    }
                }
            } else {
                // TODO could not write to ndef not supported nor is not formattable
                mmError = "Tag is not Ndef formattable";
            }
        } else {
            try {
                ndef.connect();
                if (!ndef.isWritable()) {
                    mmError = "This Ndef tag is read-only";
                } else if (ndef.getMaxSize() < size) {
                    mmError = "Message is too big for tag";
                } else {
                    ndef.writeNdefMessage(mmMessage);
                }
            } catch (FormatException e) {
                mmError = "NFC Format exception";
            } catch (IOException e) {
                mmError = "NFC IO Exception";
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    mmError = "Could not close Ndef";
                }
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mmError != null) {
            mmListener.onNfcWriteError(mmError);
        } else {
            mmListener.onNfcWriteSuccess();
        }
    }
}
