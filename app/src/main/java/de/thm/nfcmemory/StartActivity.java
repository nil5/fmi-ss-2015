package de.thm.nfcmemory;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import de.thm.nfcmemory.model.Player;


public class StartActivity extends DefaultActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final NFCMemory.Temp temp = NFCMemory.get().getTemp();
        final EditText inputName = (EditText) findViewById(R.id.input_name);

        inputName.setText(temp.getPlayer().name);
        inputName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                temp.setPlayer(new Player(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {  }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if(id == R.id.action_test) {
            Intent intent = new Intent(this, TestActivity.class);
            startActivity(intent);
        } else if(id == R.id.action_spiel) {
            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
        } else if(id == R.id.action_mapping) {
            Intent intent = new Intent(this, MappingActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}
