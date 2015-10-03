package de.thm.nfcmemory;

import android.app.PendingIntent;
import android.content.Context;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.nio.charset.Charset;
import android.nfc.tech.NdefFormatable;

import de.thm.nfcmemory.model.CardSet;
import de.thm.nfcmemory.model.CardView;
import de.thm.nfcmemory.model.Field;


public class TestActivity extends DefaultActivity {
    public static final String TAG = "TestActivity";
    public static final String MIME_TEXT_PLAIN = "text/plain";

    public static final int MODE_READ = 1;
    public static final int MODE_WRITE = 2;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter writeTagFilters[];
    private EditText editText;
    private TextView textView;
    //private Button button;
    private Tag mytag;
    private String techListArray[][];
    private RelativeLayout fieldLayout;
    private RelativeLayout cardContainer;
    private int mode = MODE_READ;
    private boolean cardFlipped = false;
    private CardSet cardSet;
    private Field field;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        try {
            cardSet = new CardSet("nils");
            field = new Field(cardSet);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_test);

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
        textView = (TextView) findViewById(R.id.test_view1);
        Switch modeSwitch = (Switch) findViewById(R.id.test_switch1);

        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mode = MODE_WRITE;
                    Toast.makeText(TestActivity.this, "Write Mode", Toast.LENGTH_SHORT).show();
                } else {
                    mode = MODE_READ;
                    Toast.makeText(TestActivity.this, "Read Mode", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*button = (Button) findViewById(R.id.test_button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (mytag == null) {
                        Toast.makeText(TestActivity.this, "Fehler", Toast.LENGTH_LONG).show();
                    } else {
                        write(editText.getText().toString(), mytag);
                        Toast.makeText(TestActivity.this, "Erfolgreich beschrieben", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(TestActivity.this, "Fehler beim Schreiben", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(TestActivity.this, "Fehler beim Schreiben", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });*/

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

        try {
            ndefDetected.addDataType("*/*"); /* Handles all MIME based dispatches. You should specify only the ones that you need. */
        }
        catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        //ndefDetected.addCategory(Intent.CATEGORY_DEFAULT);
        //tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{ndefDetected, tagDetected, };
        techListArray = new String[][]{new String[]{Ndef.class.getName()}};

        fieldLayout = (RelativeLayout) findViewById(R.id.field);

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.test_animation, new CardView.CardBackFragment())
                    .commit();
        }

        cardContainer = (RelativeLayout) findViewById(R.id.test_animation);
        cardContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipCard((int)(Math.random() * field.getSize()));
                Log.v(TAG, "Flip card!");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
        handleIntent(getIntent());

    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause");

        if(nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        field.print(this, fieldLayout);
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

        //create the message in according with the standard
        byte[] textBytes = text.getBytes();
        int textLength = textBytes.length;

        byte[] payload = new byte[textLength];

        // copy langbytes and textbytes into payload
        System.arraycopy(textBytes, 0, payload, 0, textLength);

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
      handleIntent(intent);
    }

    private void handleIntent(Intent intent){
        //read
        Log.v(TAG, "Action: " + intent.getAction());
        switch(mode){
            case MODE_READ:
                if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
                    Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    if (rawMsgs != null) {
                        NdefMessage msgs[] = new NdefMessage[rawMsgs.length];
                        textView.setText("");
                        for (int i = 0; i < rawMsgs.length; i++) {
                            msgs[i] = (NdefMessage) rawMsgs[i];
                            NdefRecord records[] = msgs[i].getRecords();

                            textView.append(new String(records[0].getPayload()));
                            Log.d(TAG, "Msg: '" + new String(records[0].getPayload()) + "'");
                        }
                    } else Toast.makeText(this, "Der NDEF-Tag ist leer.", Toast.LENGTH_LONG).show();
                } else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                    Toast.makeText(this, "Der Tag ist nicht NDEF-formatiert. Beschreiben um zu formatieren.", Toast.LENGTH_LONG).show();
                }
                break;
            case MODE_WRITE:
                mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (mytag == null) {
                    Toast.makeText(TestActivity.this, "Fehler (Tag ist null)", Toast.LENGTH_LONG).show();
                } else try {
                    write(editText.getText().toString(), mytag);
                    Toast.makeText(TestActivity.this, "Erfolgreich beschrieben", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Toast.makeText(TestActivity.this, "Fehler beim Schreiben (IO)", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(TestActivity.this, "Fehler beim Schreiben (Format)", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                break;
        }
        /*if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                NdefMessage msgs[] = new NdefMessage[rawMsgs.length];
                textView.setText("");
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    NdefRecord records[] = msgs[i].getRecords();

                    textView.append(new String(records[0].getPayload()));
                }
            }
        } else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            textView.setText(mytag.toString());
            Toast.makeText(this, "OK: " + mytag.toString(), Toast.LENGTH_LONG ).show();
        }

        /*

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            // validate that this tag can be written....
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if(supportedTechs(detectedTag.getTechList())) {
                // check if tag is writable (to the extent that we can
                if(writableTag(detectedTag)) {
                    //writeTag here
                    WriteResponse wr = writeTag(getTagAsNdef(), detectedTag);
                    String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();
                    Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,"This tag is not writable",Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this,"This tag type is not supported",Toast.LENGTH_SHORT).show();
            }
        }
        /*

        //if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
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

       */
    }

    /*
    public WriteResponse writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        String mess = "";

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    return new WriteResponse(0,"Tag is read-only");

                }
                if (ndef.getMaxSize() < size) {
                    mess = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.";
                    return new WriteResponse(0,mess);
                }

                ndef.writeNdefMessage(message);
                if(writeProtect)  ndef.makeReadOnly();
                mess = "Wrote message to pre-formatted tag.";
                return new WriteResponse(1,mess);
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        mess = "Formatted tag and wrote message";
                        return new WriteResponse(1,mess);
                    } catch (IOException e) {
                        mess = "Failed to format tag.";
                        return new WriteResponse(0,mess);
                    }
                } else {
                    mess = "Tag doesn't support NDEF.";
                    return new WriteResponse(0,mess);
                }
            }
        } catch (Exception e) {
            mess = "Failed to write tag";
            return new WriteResponse(0,mess);
        }
    }

    private class WriteResponse {
        int status;
        String message;
        WriteResponse(int Status, String Message) {
            this.status = Status;
            this.message = Message;
        }
        public int getStatus() {
            return status;
        }
        public String getMessage() {
            return message;
        }
    }

    public static boolean supportedTechs(String[] techs) {
        boolean ultralight=false;
        boolean nfcA=false;
        boolean ndef=false;
        for(String tech:techs) {
            if(tech.equals("android.nfc.tech.MifareUltralight")) {
                ultralight=true;
            }else if(tech.equals("android.nfc.tech.NfcA")) {
                nfcA=true;
            } else if(tech.equals("android.nfc.tech.Ndef") || tech.equals("android.nfc.tech.NdefFormatable")) {
                ndef=true;

            }
        }
        if(ultralight && nfcA && ndef) {
            return true;
        } else {
            return false;
        }
    }

    private boolean writableTag(Tag tag) {

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(this,"Tag is read-only.",Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return false;
                }
                ndef.close();
                return true;
            }
        } catch (Exception e) {
            Toast.makeText(this,"Failed to read tag",Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    private NdefMessage getTagAsNdef() {
        boolean addAAR = false;
        String uniqueId = "tapwise.com";
        byte[] uriField = uniqueId.getBytes(Charset.forName("US-ASCII"));
        byte[] payload = new byte[uriField.length + 1];              //add 1 for the URI Prefix
        payload[0] = 0x01;

        System.arraycopy(uriField, 0, payload, 1, uriField.length);  //appends URI to payload
        NdefRecord rtdUriRecord = new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload);


        if(addAAR) {
            // note:  returns AAR for different app (nfcreadtag)
            return new NdefMessage(new NdefRecord[] {
                    rtdUriRecord, NdefRecord.createApplicationRecord("com.tapwise.nfcreadtag")
            });
        } else {
            return new NdefMessage(new NdefRecord[] {
                    rtdUriRecord});
        }
    }
    */

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

    private void flipCard(int index) {
        if (cardFlipped) {
            getFragmentManager().popBackStack();
            cardFlipped = false;
            return;
        }

        // Flip to the back.

        cardFlipped = true;

        // Create and commit a new fragment transaction that adds the fragment for the back of
        // the card, uses custom animations, and is part of the fragment manager's back stack.

        final CardView.CardFrontFragment fragment = new CardView.CardFrontFragment();
        fragment.setImage(field.getCard(index).getSrc());

        getFragmentManager()
                .beginTransaction()

                        // Replace the default fragment animations with animator resources representing
                        // rotations when switching to the back of the card, as well as animator
                        // resources representing rotations when flipping back to the front (e.g. when
                        // the system Back button is pressed).
                .setCustomAnimations(
                        R.animator.animation_card_flip_right_in, R.animator.animation_card_flip_right_out,
                        R.animator.animation_card_flip_left_in, R.animator.animation_card_flip_left_out)

                        // Replace any fragments currently in the container view with a fragment
                        // representing the next page (indicated by the just-incremented currentPage
                        // variable).
                .replace(R.id.test_animation, fragment)

                        // Add this transaction to the back stack, allowing users to press Back
                        // to get to the front of the card.
                .addToBackStack(null)

                        // Commit the transaction.
                .commit();
    }
}
