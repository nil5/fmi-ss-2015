package de.thm.nfcmemory;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by Nils on 16.07.2015.
 */
public class NFCActivity extends DefaultActivity {
    private static final String TAG = "NFCActivity";
    private static final ArrayList<NFCListener> NFC_LISTENERS = new ArrayList<>();

    private IntentFilter tagFilter[];
    private NfcAdapter adapter;
    private PendingIntent pendingIntent;
    private Mode mode = Mode.READ;
    private String writeString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = NfcAdapter.getDefaultAdapter(NFCMemory.get());
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

        tagFilter = new IntentFilter[]{ndefDetected, tagDetected, };
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        adapter.enableForegroundDispatch(this, pendingIntent, tagFilter, null);
        handleIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(adapter != null) adapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent){
        switch(mode){
            case READ:
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
                    Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    if (rawMsgs != null) {
                        NdefMessage msgs[] = new NdefMessage[rawMsgs.length];
                        for(NFCListener lis : NFC_LISTENERS){
                            lis.onTagDetected(msgs);
                        }
                    } else for(NFCListener lis : NFC_LISTENERS){
                        lis.onEmptyTag();
                    }
                } else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                    for(NFCListener lis : NFC_LISTENERS){
                        lis.onEmptyTag();
                    }
                }
                break;
            case WRITE:
                final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                boolean error = false;
                if (tag == null) {
                    Log.v(TAG, "Fehler (Tag ist null)");
                    error = true;
                } else try {
                    write(writeString, tag);
                    Log.v(TAG, "Erfolgreich beschrieben");
                } catch (IOException e) {
                    Log.v(TAG, "Fehler beim Schreiben (IO)");
                    e.printStackTrace();
                    error = true;
                } catch (FormatException e) {
                    Log.v(TAG, "Fehler beim Schreiben (Format)");
                    e.printStackTrace();
                    error = true;
                }
                if(error) for(NFCListener lis : NFC_LISTENERS){
                    lis.onWriteError();
                } else for(NFCListener lis : NFC_LISTENERS){
                    lis.onWriteSuccess();
                }
                break;
        }
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        byte[] textBytes = text.getBytes("US-ASCII");
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + textLength];
        payload[0] = (byte) textLength;

        System.arraycopy(textBytes, 0, payload, 1, textLength);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    public Mode mode(){
        return this.mode;
    }

    public void mode(Mode mode){
        this.mode = mode;
    }

    public void setString(String s){
        this.writeString = s;
    }

    public boolean support(){
        return adapter != null;
    }

    public boolean isEnabled(){
        return adapter.isEnabled();
    }

    @TargetApi(16)
    public void toSettings(){
        final Intent intent;
        if(NFCMemory.Const.API >= 16)
            intent = new Intent(Settings.ACTION_NFC_SETTINGS);
        else intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        NFCMemory.get().startActivity(intent);
    }

    public enum Mode {
        READ, WRITE
    }

    public interface NFCListener {
        void onTagDetected(NdefMessage messages[]);
        void onEmptyTag();
        void onWriteError();
        void onWriteSuccess();
    }

    public void addNFCListener(NFCListener lis){
        NFC_LISTENERS.add(lis);
    }

    public void removeNFCListener(NFCListener lis){
        NFC_LISTENERS.remove(lis);
    }
}
