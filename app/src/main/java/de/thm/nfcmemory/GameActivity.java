package de.thm.nfcmemory;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.UUID;

import de.thm.nfcmemory.bluetooth.BluetoothActivity;
import de.thm.nfcmemory.bluetooth.BluetoothConnection;
import de.thm.nfcmemory.bluetooth.BluetoothMessage;
import de.thm.nfcmemory.bluetooth.listener.BluetoothConnectionStateListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothDiscoveryListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothMessageListener;
import de.thm.nfcmemory.model.Game;
import de.thm.nfcmemory.model.Player;
import de.thm.nfcmemory.model.Rules;
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
    private Game game;
    private boolean flipped = false;
    private int messageReceivedCounter = 0;
    private int playerType;
    private String playerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Player name
        playerName = NFCMemory.get().getTemp().getPlayer().name;

        // View flipper
        flipper = (ViewFlipper) findViewById(R.id.game_flipper);

        // Lobby view elements
        lobbyStatus = (TextView) findViewById(R.id.game_lobby_status);
        joinGame = (Button) findViewById(R.id.game_join);
        joinGameProgress = (ProgressBar) findViewById(R.id.game_join_progress);
        hostGame = (Button) findViewById(R.id.game_host);
        hostGameProgress = (ProgressBar) findViewById(R.id.game_host_progress);
        startGame = (Button) findViewById(R.id.game_start);

        final Button sendMessage = (Button) findViewById(R.id.game_test_message);
        final TextView lobbyHost = (TextView) findViewById(R.id.game_versus_host);
        final TextView lobbyClient = (TextView) findViewById(R.id.game_versus_client);

        // Prepare Bluetooth device list
        final ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
        bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(this, bluetoothDevices);
        bluetoothDeviceList = (ListView) findViewById(R.id.game_bluetooth_devices);
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

        // Lobby functions
        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getState() != STATE_CONNECTED) {
                    Toast.makeText(GameActivity.this, "Not connected to other device.", Toast.LENGTH_LONG).show();
                } else if(game == null){
                    Toast.makeText(GameActivity.this, "Unable to start game. No game initialized.", Toast.LENGTH_LONG).show();
                } else send(BluetoothMessage.START);
            }
        });

        hostGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serve("de.thm.nfcmemory", true);
                makeDiscoverable();
                bluetoothDeviceListAdapter.clear();
                joinGame.setEnabled(false);
                hostGame.setEnabled(false);
                hostGameProgress.setVisibility(View.VISIBLE);
                lobbyHost.setText(playerName);
                playerType = Player.HOST;
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
                lobbyClient.setText(playerName);
                playerType = Player.CLIENT;
                lobbyStatus.setText("Searching for nearby hosted games...");
            }
        });

        // Bluetooth
        if(!bluetoothSupport()){
            Toast.makeText(this, "Can not create game. Your device doesn't support Bluetooth technology.", Toast.LENGTH_LONG).show();
            finish();
        }
        addBluetoothDiscoveryListener(this);
        addBluetoothConnectionStateListener(this);

        // Message handler
        addBluetoothMessageListener(new BluetoothMessageListener() {
            @Override
            public void onMessageRecieved(Message msg) {
                String m = (String) msg.obj;
                Log.v(TAG, "Msg: " + m);

                try {
                    byte b = Byte.valueOf(m);
                    // Message is Byte-Message
                    if (b == BluetoothMessage.START && !flipped) {
                        flipper.showNext();
                        flipped = true;
                        send(BluetoothMessage.START);
                    } else Toast.makeText(GameActivity.this, "Invalid message code: " + b, Toast.LENGTH_LONG).show();
                } catch (NumberFormatException e1) {
                    // Message is String-Message
                    final String s[] = m.split(":", 2);
                    final String key = s[0];
                    final String val;
                    try {
                        val = s[1].trim();
                    } catch (IndexOutOfBoundsException e2){
                        Toast.makeText(GameActivity.this, "Invalid text message. Message must have pattern [key]: [value]", Toast.LENGTH_LONG).show();
                        return;
                    }
                    switch (key) {
                        case "name":
                            final Player player1, player2;
                            if (playerType == Player.HOST){
                                lobbyClient.setText(val);
                                player1 = new Player(playerName);
                                player2 = new Player(val);
                            } else if (playerType == Player.CLIENT){
                                lobbyHost.setText(val);
                                player1 = new Player(val);
                                player2 = new Player(playerName);
                            } else {
                                Toast.makeText(GameActivity.this, "Invalid player type: " + playerType, Toast.LENGTH_LONG).show();
                                return;
                            }
                            game = new Game(player1, player2, Rules.getStandardRules());
                            break;
                        default:
                            Toast.makeText(GameActivity.this, "Invalid message key: " + key, Toast.LENGTH_LONG).show();
                    }
                }

                sendMessage.setText("Nachricht senden (empfangen: " + ++messageReceivedCounter + ")");
            }
        });

        // Game functions
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("Hello:");
            }
        });
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
    protected void onStop() {
        super.onStop();
        disconnect();
    }

    @Override
    public void onBackPressed() {
        // TODO: Really quit?
        super.onBackPressed();
    }

    @Override
    public void onDiscoveryStart() {
        lobbyStatus.setText("Searching...: 0 hosted games found");
        startGame.setEnabled(false);
    }

    @Override
    @TargetApi(15)
    public void onDeviceFound(BluetoothDevice device) {
        bluetoothDeviceListAdapter.add(device);
        if(NFCMemory.Const.API >= 15) {
            final ParcelUuid uuids[] = device.getUuids();
            for(int i = 0; i < uuids.length; i++){
                final UUID uuid = uuids[i].getUuid();
                Log.v(TAG, "UUID: " + uuid.toString());
            }
        }
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
            lobbyStatus.setText("Connected. Let's go!");
            send("name: " + playerName);
        } else if(oldState == STATE_SERVE){
            hostGame.setEnabled(true);
            joinGame.setEnabled(true);
            hostGameProgress.setVisibility(View.INVISIBLE);
            lobbyStatus.setText("No player has joined your game.");
        } else if(oldState == STATE_CONNECTED && newState == STATE_ERROR){
            Toast.makeText(this, "Connection was closed unexpectedly.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * card front.
     */
    public static class CardFrontFragment extends Fragment {
        private Bitmap bmp;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ImageView v = (ImageView) inflater.inflate(R.layout.card, container, false);
            v.setImageBitmap(bmp);
            return v;
        }

        public void setImage(Bitmap bmp){
            this.bmp = bmp;
        }
    }

    /**
     * card back.
     */
    public static class CardBackFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final File sd = Environment.getExternalStorageDirectory();
            final File file = new File(sd + NFCMemory.Const.SD_FOLDER + "/CardBacks/" + "default.png"); // TODO: Custom card backs

            ImageView v = (ImageView) inflater.inflate(R.layout.card, container, false);

            if(file.exists()){
                v.setImageBitmap(BitmapFactory.decodeFile(String.valueOf(file)));
            }
            return v;
        }
    }
}
