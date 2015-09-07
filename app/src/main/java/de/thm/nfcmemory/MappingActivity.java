package de.thm.nfcmemory;

import android.nfc.NdefMessage;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import java.io.FileNotFoundException;

import de.thm.nfcmemory.model.CardSet;
import de.thm.nfcmemory.model.Field;


public class MappingActivity extends NFCActivity implements NFCActivity.NFCListener {
    private static final String IDENTIFIER = "de.thm.nfcmemory.";
    private RelativeLayout fieldLayout;
    private Field field;
    private int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapping);

        fieldLayout = (RelativeLayout) findViewById(R.id.mapping_field);
        mode(Mode.WRITE);
        addNFCListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        try {
            currentIndex = 0;
            setString(IDENTIFIER + currentIndex);
            field = new Field(new CardSet("nils")); // TODO: Replace with selected card set
            field.print(this, fieldLayout);
            field.setCardColor(currentIndex, "#00FF00");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mapping, menu);
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

    @Override
    public void onTagDetected(NdefMessage[] messages) {

    }

    @Override
    public void onEmptyTag() {

    }

    @Override
    public void onWriteError() {

    }

    @Override
    public void onWriteSuccess() {
        field.setCardColor(currentIndex, "#AAAAAA");
        if(++currentIndex < field.getSize()) {
            field.setCardColor(currentIndex, "#00FF00");
            setString(IDENTIFIER + currentIndex);
        } else mode(Mode.READ);
    }
}
