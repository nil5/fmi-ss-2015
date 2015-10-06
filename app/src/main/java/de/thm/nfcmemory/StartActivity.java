package de.thm.nfcmemory;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import de.thm.nfcmemory.model.Player;


public class StartActivity extends DefaultActivity {
    public static final String TAG = "StartActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final NFCMemory.Temp temp = NFCMemory.get().getTemp();
        final EditText inputName = (EditText) findViewById(R.id.input_name);
        final Button start = (Button) findViewById(R.id.button_start_game);

        inputName.setText(temp.getPlayerName());
        inputName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                temp.setPlayerName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Start Game!");
                Intent intent = new Intent(StartActivity.this, GameActivity.class);
                startActivity(intent);
            }
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
        } else if(id == R.id.action_mapping) {
            Intent intent = new Intent(this, MappingActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}
