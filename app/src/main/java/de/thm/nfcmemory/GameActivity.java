package de.thm.nfcmemory;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.ArrayList;

import de.thm.nfcmemory.bluetooth.BluetoothActivity;
import de.thm.nfcmemory.bluetooth.BluetoothConnection;
import de.thm.nfcmemory.bluetooth.BluetoothMessage;
import de.thm.nfcmemory.bluetooth.listener.BluetoothConnectionStateListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothDiscoveryListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothMessageListener;
import de.thm.nfcmemory.model.adapter.BluetoothDeviceListAdapter;


public class GameActivity extends BluetoothActivity implements BluetoothDiscoveryListener, BluetoothConnectionStateListener {
    public static final String TAG = "GameActivity";

    private ListView bluetoothDeviceList;
    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter;
    private BluetoothDevice selectedDevice = null;
    private ViewFlipper flipper;
    private TextView lobbyStatus;
    private Button joinGame;
    private ProgressBar joinGameProgress;
    private Button hostGame;
    private ProgressBar hostGameProgress;
    private Button startGame;
    private boolean flipped = false;
    private int messageReceivedCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        final ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
        bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(this, bluetoothDevices);

        bluetoothDeviceList = (ListView) findViewById(R.id.game_bluetooth_devices);
        flipper = (ViewFlipper) findViewById(R.id.game_flipper);

        lobbyStatus = (TextView) findViewById(R.id.game_lobby_status);
        joinGame = (Button) findViewById(R.id.game_join);
        joinGameProgress = (ProgressBar) findViewById(R.id.game_join_progress);
        hostGame = (Button) findViewById(R.id.game_host);
        hostGameProgress = (ProgressBar) findViewById(R.id.game_host_progress);
        startGame = (Button) findViewById(R.id.game_start);

        final Button sendMessage = (Button) findViewById(R.id.game_test_message);
        final TextView lobbyHost = (TextView) findViewById(R.id.game_versus_host);
        final TextView lobbyClient = (TextView) findViewById(R.id.game_versus_client);

        bluetoothDeviceList.setAdapter(bluetoothDeviceListAdapter);
        bluetoothDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = bluetoothDeviceListAdapter.get(position);

                joinGame.setEnabled(false);
                joinGameProgress.setVisibility(View.VISIBLE);
                bluetoothDeviceList.setEnabled(false);
                new BluetoothConnection(GameActivity.this, selectedDevice.getAddress()) {
                    @Override
                    public void onConnectionEstablished() {

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

        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getState() != STATE_CONNECTED) {
                    Toast.makeText(GameActivity.this, "Not connected to other device.", Toast.LENGTH_LONG).show();
                } else send(BluetoothMessage.START);
            }
        });

        hostGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serve("NFC Memory", true); // TODO: Insert game name
                makeDiscoverable();
                bluetoothDeviceListAdapter.clear();
                joinGame.setEnabled(false);
                hostGame.setEnabled(false);
                hostGameProgress.setVisibility(View.VISIBLE);
                lobbyHost.setText("Player1"); // TODO: Insert player name
                lobbyStatus.setText("Waiting for client...");
            }
        });

        joinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discover();
                bluetoothDeviceListAdapter.clear();
                joinGame.setEnabled(false);
                hostGame.setEnabled(false);
                joinGameProgress.setVisibility(View.VISIBLE);
                lobbyClient.setText("Player2"); // TODO: Insert player name
                lobbyStatus.setText("Searching for nearby hosted games...");
            }
        });

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("Hello");
            }
        });

        addBluetoothMessageListener(new BluetoothMessageListener() {
            @Override
            public void onMessageRecieved(Message msg) {
                String m = (String) msg.obj;
                Log.v(TAG, "Msg: " + m);

                try{
                    byte b = Byte.valueOf(m);
                    // Message is Byte-Message
                    if(b == BluetoothMessage.START && !flipped){
                        flipper.showNext();
                        flipped = true;
                        send(BluetoothMessage.START);
                    }
                } catch(NumberFormatException e){
                    // Message is String-Message
                    // TODO
                }

                sendMessage.setText("Nachricht senden (empfangen: " + ++messageReceivedCounter + ")");
            }
        });

        if(!bluetoothSupport()){
            Toast.makeText(this, "Can not create game. Your device doesn't support Bluetooth technology.", Toast.LENGTH_LONG).show();
        }

        addBluetoothDiscoveryListener(this);
        addBluetoothConnectionStateListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!bluetoothEnabled()){
            enable(true);
        }
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

    @Override
    public void onDiscoveryStart() {
        lobbyStatus.setText("Searching...: 0 hosted games found");
        startGame.setEnabled(false);
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        bluetoothDeviceListAdapter.add(device);
        lobbyStatus.setText("Searching...: " + bluetoothDeviceListAdapter.getCount() + " hosted games found");
    }

    @Override
    public void onDiscoveryFinished() {
        joinGame.setEnabled(true);
        hostGame.setEnabled(true);
        joinGameProgress.setVisibility(View.INVISIBLE);
        if(bluetoothDeviceListAdapter.isEmpty()) {
            lobbyStatus.setText("Sorry, no game was found.");
        } else {
            lobbyStatus.setText("Please select a game from the list below.");
        }
    }

    @Override
    public void onConnectionStateChanged(int oldState, int newState, int stateChangedCount) {
        Log.v(TAG, "old: " + oldState + ", new: " + newState);
        if(newState == STATE_CONNECTED){
            startGame.setEnabled(true);
            hostGame.setEnabled(false);
            joinGame.setEnabled(false);
            hostGameProgress.setVisibility(View.INVISIBLE);
            joinGameProgress.setVisibility(View.INVISIBLE);
        } else if(oldState == STATE_SERVE){
            hostGame.setEnabled(true);
            hostGameProgress.setVisibility(View.INVISIBLE);
            lobbyStatus.setText("No player has joined your game.");
        }
    }
}
