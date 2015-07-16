package de.thm.nfcmemory;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class TestActivity extends DefaultActivity {
    public static final String TAG = "TestActivity";
    public static final String MIME_TEXT_PLAIN = "text/plain";

    private EditText editText;
    private Button button;
    private Tag mytag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        NfcAdapter nfcAdapter;
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "NFC is enabled.", Toast.LENGTH_LONG).show();
        }

        editText = (EditText) findViewById(R.id.test_text1);
        button = (Button) findViewById(R.id.test_button1);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(mytag==null){
                        Toast.makeText(TestActivity.this, "Fehler", Toast.LENGTH_LONG ).show();
                    }else{
                        write(editText.getText().toString(), mytag);
                        Toast.makeText(TestActivity.this, "Schreibe...", Toast.LENGTH_LONG ).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(TestActivity.this, "Fehler beim Schreiben", Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(TestActivity.this, "Fehler beim Schreiben" , Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                }
            }
        });

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter writeTagFilters[] = new IntentFilter[] { tagDetected };
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");



       // handleIntent(getIntent());
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

        //create the message in according with the standard
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //handleIntent(intent);
        Log.d(TAG, "Action: " + intent.getAction());
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, "Tag detagted: " + mytag.toString(), Toast.LENGTH_LONG ).show();
        }
    }

    private void handleIntent(Intent intent){
        String action = intent.getAction();
        Log.d(TAG, "Action: " + action + "(" + NfcAdapter.ACTION_NDEF_DISCOVERED + ", " + NfcAdapter.ACTION_TECH_DISCOVERED + ", " + NfcAdapter.ACTION_TAG_DISCOVERED + ")");
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Log.d(TAG, "NDEF discvored!");

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NDEFReaderTask(this).execute(tag);

            } else {
                Toast.makeText(this, "Wrong mime type: " + type, Toast.LENGTH_LONG).show();
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NDEFReaderTask(this).execute(tag);
                    break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}