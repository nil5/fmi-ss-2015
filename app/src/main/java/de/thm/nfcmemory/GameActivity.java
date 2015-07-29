package de.thm.nfcmemory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.ArrayList;

import de.thm.nfcmemory.bluetooth.BluetoothActivity;
import de.thm.nfcmemory.bluetooth.BluetoothConnection;
import de.thm.nfcmemory.bluetooth.listener.BluetoothConnectionStateListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothDiscoveryListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothMessageListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothPowerStateListener;
import de.thm.nfcmemory.model.adapter.BluetoothDeviceListAdapter;


public class GameActivity extends BluetoothActivity {

    private ArrayList<BluetoothDevice> bluetoothDevices;
    private ListView bluetoothDeviceList;
    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter;
    private BluetoothDevice selectedDevice = null;
    private ViewFlipper flipper;
    private boolean flipped = false;
    private int messageSentCounter = 0;
    private int messageReceivedCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        bluetoothDevices = new ArrayList<BluetoothDevice>();
        bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(this, bluetoothDevices);

        bluetoothDeviceList = (ListView) findViewById(R.id.game_bluetooth_devices);
        flipper = (ViewFlipper) findViewById(R.id.game_flipper);

        final Button startGame = (Button) findViewById(R.id.game_start);
        final Button sendMessage = (Button) findViewById(R.id.game_test_message);
        final ImageButton serveGame = (ImageButton) findViewById(R.id.game_serve);

        bluetoothDeviceList.setAdapter(bluetoothDeviceListAdapter);
        bluetoothDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = bluetoothDeviceListAdapter.get(position);
            }
        });

        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedDevice == null) {
                    Toast.makeText(GameActivity.this, "Not connected to other device.", Toast.LENGTH_LONG).show();
                    return;
                } else if (getState() != STATE_CONNECTED) {

                }

                new BluetoothConnection(GameActivity.this, selectedDevice.getAddress()) {
                    @Override
                    public void onConnectionEstablished() {
                        flipper.showNext();
                        flipped = true;
                    }

                    @Override
                    public void onConnectionError(String msg) {
                        Toast.makeText(GameActivity.this, "Error while attempting to connect: " + msg, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onDisconnect() {

                    }
                }.start();
            }
        });

        serveGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serve("NFC Memory", true);
            }
        });

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("Hello");
                messageSentCounter++;
            }
        });

        addBluetoothMessageListener(new BluetoothMessageListener() {
            @Override
            public void onMessageRecieved(Message msg) {
                if(!flipped){
                    flipper.showNext();
                    flipped = true;
                }

                sendMessage.setText("Nachricht senden (empfangen: " + ++messageReceivedCounter + ")");
            }
        });

        if(!bluetoothSupport()){
            Toast.makeText(this, "Can not create game. Your device doesn't support Bluetooth technology.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!bluetoothEnabled()){
            enable(true);
        } else discover();

        addBluetoothDiscoveryListener(new BluetoothDiscoveryListener() {
            @Override
            public void onDiscoveryStart() {

            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                Log.v("GameActivity", "Device found.");
                bluetoothDeviceListAdapter.add(device);
            }

            @Override
            public void onDiscoveryFinished() {

            }
        });

        addBluetoothPowerStateListener(new BluetoothPowerStateListener() {
            @Override
            public void onPowerStateChanged(int state) {
                if (state == BluetoothAdapter.STATE_ON) {
                    discover();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
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
