package de.thm.nfcmemory;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.UUID;

import de.thm.nfcmemory.bluetooth.BluetoothActivity;
import de.thm.nfcmemory.bluetooth.BluetoothConnection;
import de.thm.nfcmemory.bluetooth.BluetoothMessage;
import de.thm.nfcmemory.bluetooth.listener.BluetoothConnectionStateListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothDiscoveryListener;
import de.thm.nfcmemory.bluetooth.listener.BluetoothMessageListener;
import de.thm.nfcmemory.model.CardSet;
import de.thm.nfcmemory.model.CardView;
import de.thm.nfcmemory.model.Field;
import de.thm.nfcmemory.model.Game;
import de.thm.nfcmemory.model.Player;
import de.thm.nfcmemory.model.Rules;
import de.thm.nfcmemory.model.adapter.BluetoothDeviceListAdapter;


public class GameActivity extends BluetoothActivity implements BluetoothDiscoveryListener, BluetoothConnectionStateListener, NFCActivity.NFCListener {
    public static final String TAG = "GameActivity";

    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter;
    private BluetoothDevice selectedDevice = null;

    private ViewFlipper flipper;
    private RelativeLayout fieldLayout;
    private ListView bluetoothDeviceList;
    private TextView lobbyStatus;
    private Button startGame;
    private Button joinGame;
    private Button hostGame;
    private Button toggleField;
    private Button continueButton;
    private ProgressBar joinGameProgress;
    private ProgressBar hostGameProgress;

    private CardView cardView;
    private Field field;
    private Game game;
    private int playerType;
    private String playerName;

    private int messageReceivedCounter = 0;
    private boolean viewFlipped = false;
    boolean fieldVisible = false;
    boolean fieldInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Log.v(TAG, "onCreate");

        // Player name
        playerName = app.getTemp().getPlayer().name;
        final CardSet cardSet = app.getTemp().getCardSet();
        if(cardSet == null){
            Toast.makeText(this, "No card set selected. Go to settings and choose a valid card set.", Toast.LENGTH_LONG).show();
            finish();
        }

        // View flipper
        flipper = (ViewFlipper) findViewById(R.id.game_flipper);

        // Lobby view elements
        lobbyStatus = (TextView) findViewById(R.id.game_lobby_status);
        joinGame = (Button) findViewById(R.id.game_join);
        joinGameProgress = (ProgressBar) findViewById(R.id.game_join_progress);
        hostGame = (Button) findViewById(R.id.game_host);
        hostGameProgress = (ProgressBar) findViewById(R.id.game_host_progress);
        startGame = (Button) findViewById(R.id.game_start);

        continueButton = (Button) findViewById(R.id.game_continue);
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
                } else if (game == null) {
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


        // Field init
        fieldLayout = (RelativeLayout) findViewById(R.id.game_field);
        field = new Field(cardSet, NFCMemory.Const.getScreen().width);
        final int fieldSize = field.getSize();

        // CardView
        cardView = new CardView(getFragmentManager(), field);
        // CardView init
        if(savedInstanceState == null)
            cardView.init(R.id.game_flip_card_left, R.id.game_flip_card_right);

        // Bluetooth
        if(!bluetoothSupport()){
            Toast.makeText(this, "Can not create game. Your device doesn't support Bluetooth technology.", Toast.LENGTH_LONG).show();
            finish();
        }
        addBluetoothDiscoveryListener(this);
        addBluetoothConnectionStateListener(this);

        // NFC
        addNFCListener(this);

        // Message handler
        addBluetoothMessageListener(new BluetoothMessageListener() {
            @Override
            public void onMessageRecieved(Message msg) {
                String m = (String) msg.obj;
                Log.v(TAG, "Msg: " + m);

                try {
                    byte b = Byte.valueOf(m);
                    // Message is Byte-Message
                    if (b == BluetoothMessage.START && !viewFlipped) {
                        flipper.showNext();
                        viewFlipped = true;
                        send(BluetoothMessage.START);
                    } else
                        Toast.makeText(GameActivity.this, "Invalid message code: " + b, Toast.LENGTH_LONG).show();
                } catch (NumberFormatException e1) {
                    // Message is String-Message
                    final String s[] = m.split(":", 2);
                    final String key = s[0];
                    final String val;
                    try {
                        val = s[1].trim();
                    } catch (IndexOutOfBoundsException e2) {
                        Toast.makeText(GameActivity.this, "Invalid text message. Message must have pattern [key]: [value]", Toast.LENGTH_LONG).show();
                        return;
                    }
                    switch (key) {
                        case "name":
                            final Player player1, player2;
                            if (playerType == Player.HOST) {
                                lobbyClient.setText(val);
                                player1 = new Player(playerName);
                                player2 = new Player(val);
                            } else if (playerType == Player.CLIENT) {
                                lobbyHost.setText(val);
                                player1 = new Player(val);
                                player2 = new Player(playerName);
                            } else {
                                Toast.makeText(GameActivity.this, "Invalid player type: " + playerType, Toast.LENGTH_LONG).show();
                                return;
                            }
                            game = new Game(player1, player2, Rules.getStandardRules(), field);

                            break;
                        default:
                            Toast.makeText(GameActivity.this, "Invalid message key: " + key, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        // Game functions
        continueButton.setEnabled(false);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardView.reset();
            }
        });

        // Card flip listener
        final RelativeLayout leftCardContainer = (RelativeLayout) findViewById(R.id.game_flip_card_left);
        final RelativeLayout rightCardContainer = (RelativeLayout) findViewById(R.id.game_flip_card_right);
        leftCardContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cardView.getIndex(CardView.Card.LEFT) != -1)
                    cardView.flipCard(CardView.Card.LEFT);
            }
        });
        rightCardContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cardView.getIndex(CardView.Card.RIGHT) != -1)
                    cardView.flipCard(CardView.Card.RIGHT);
            }
        });

        // Toggle field init
        toggleField = (Button) findViewById(R.id.game_button_show_field);
        toggleField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fieldLayout.getVisibility() != View.VISIBLE) {
                    fieldLayout.setVisibility(View.VISIBLE);
                    toggleField.setText("Hide field");
                } else {
                    fieldLayout.setVisibility(View.GONE);
                    toggleField.setText("Show field");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");

        if(!bluetoothEnabled()){
            enable(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        removeNFCListener(this);
        removeBluetoothDiscoveryListener(this);
        removeBluetoothConnectionStateListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // TODO: Options menu
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            flipper.showNext();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!fieldInitialized){
            field.print(this, fieldLayout);
            fieldInitialized = true;
            Log.d(TAG, "Field initialized.");
        }
    }

    @Override
    public void onBackPressed() {
        if(viewFlipped) new AlertDialog.Builder(this)
                .setTitle("Quit game")
                .setMessage("Do you really want to leave the game? Progress will be lost and the connection will be closed.")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GameActivity.super.onBackPressed();
                    }
                }).setNegativeButton("No", null)
                .setNeutralButton("Cancel", null);
        else super.onBackPressed();
    }

    @Override
    public void onDiscoveryStart() {
        lobbyStatus.setText("Searching...: 0 devices found");
        startGame.setEnabled(false);
    }

    @Override
    @TargetApi(15)
    public void onDeviceFound(BluetoothDevice device) {
        bluetoothDeviceListAdapter.add(device);
        if(NFCMemory.Const.API >= 15) {
            final ParcelUuid uuids[] = device.getUuids();
            for (ParcelUuid uuid1 : uuids) {
                final UUID uuid = uuid1.getUuid();
                Log.v(TAG, "UUID: " + uuid.toString());
            }
        }
        lobbyStatus.setText("Searching...: " + bluetoothDeviceListAdapter.getCount() + " devices found");
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

    @Override
    public void onTagDetected(NdefMessage[] messages) {
        if(fieldInitialized){
            if(messages.length > 0) {
                final NdefRecord records[] = messages[0].getRecords();
                if(records.length > 0){
                    final String message = new String(records[0].getPayload());
                    final int identifierLength = MappingActivity.IDENTIFIER.length();
                    if(message.length() > identifierLength){
                        try {
                            final int index = Integer.valueOf(message.substring(identifierLength));
                            if(index < 0 || index < field.getSize()){
                                if(cardView.flipCard(index) == CardView.Card.RIGHT)
                                    continueButton.setEnabled(true);
                                return;
                            } else Log.e(TAG, "Could not flip card. Index out of field range.");
                        } catch(NumberFormatException e){
                            Log.e(TAG, "Could not process NFC content. Invalid message: '" + message + "'");
                        }
                    } else Log.e(TAG, "Could not process NFC content. Message to short.");
                } else Log.e(TAG, "Could not process NFC content. No record found.");
            } else Log.e(TAG, "Could not process NFC content. No message found.");
        } else Log.e(TAG, "Could not process NFC content. Game field was not initialized.");
        Toast.makeText(this, "An error occured while reading the tag. Have you already mapped the field?", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onEmptyTag() {
        Toast.makeText(this, "Sorry, but this tag is empty. Have you already mapped the field?", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onWriteError() {

    }

    @Override
    public void onWriteSuccess() {

    }
}
