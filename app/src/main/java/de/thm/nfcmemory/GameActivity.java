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

import org.json.JSONArray;
import org.json.JSONException;

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
import de.thm.nfcmemory.model.MessageHandler;
import de.thm.nfcmemory.model.Player;
import de.thm.nfcmemory.model.Rules;
import de.thm.nfcmemory.model.adapter.BluetoothDeviceListAdapter;
import de.thm.nfcmemory.model.InGameMessage;


public class GameActivity extends BluetoothActivity implements BluetoothDiscoveryListener, BluetoothConnectionStateListener, BluetoothMessageListener, View.OnClickListener, AdapterView.OnItemClickListener, NFCActivity.NFCListener {
    public static final String TAG = "GameActivity";

    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter;
    private BluetoothDevice selectedDevice = null;

    private ViewFlipper flipper;
    private RelativeLayout fieldLayout;
    private ListView bluetoothDeviceList;
    private TextView lobbyStatus;
    private TextView lobbyHost;
    private TextView lobbyClient;
    private Button startGame;
    private Button joinGame;
    private Button hostGame;
    private Button toggleField;
    private Button continueButton;
    private ProgressBar joinGameProgress;
    private ProgressBar hostGameProgress;

    private InGameMessage currentMessage = new InGameMessage(InGameMessage.TYPE_SENT);
    private MessageHandler messageHandler = new MessageHandler();
    private CardView cardView;
    private Field field;
    private Game game;

    private int playerType;
    private String playerName;
    private boolean viewFlipped = false;
    boolean fieldInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        Log.v(TAG, "onCreate");

        // Player name
        playerName = app.getTemp().getPlayerName();
        final CardSet cardSet = app.getTemp().getCardSet();
        if(cardSet == null){
            Toast.makeText(this, "No card set selected. Go to settings and choose a valid card set.", Toast.LENGTH_LONG).show();
            finish();
        }

        // View flipper
        flipper = (ViewFlipper) findViewById(R.id.game_flipper);

        // Lobby view elements
        lobbyStatus = (TextView) findViewById(R.id.game_lobby_status);
        lobbyHost = (TextView) findViewById(R.id.game_versus_host);
        lobbyClient = (TextView) findViewById(R.id.game_versus_client);
        joinGame = (Button) findViewById(R.id.game_join);
        joinGameProgress = (ProgressBar) findViewById(R.id.game_join_progress);
        hostGame = (Button) findViewById(R.id.game_host);
        hostGameProgress = (ProgressBar) findViewById(R.id.game_host_progress);
        startGame = (Button) findViewById(R.id.game_start);
        bluetoothDeviceList = (ListView) findViewById(R.id.game_bluetooth_devices);

        // Game view elements
        continueButton = (Button) findViewById(R.id.game_continue);
        fieldLayout = (RelativeLayout) findViewById(R.id.game_field);

        // Prepare Bluetooth device list
        bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(this, new ArrayList<BluetoothDevice>());
        bluetoothDeviceList.setAdapter(bluetoothDeviceListAdapter);
        bluetoothDeviceList.setOnItemClickListener(this);

        // Lobby functions
        startGame.setOnClickListener(this);
        hostGame.setOnClickListener(this);
        joinGame.setOnClickListener(this);

        // Field init
        field = new Field(cardSet, NFCMemory.Const.getScreen().width);

        // Game init
        game = new Game(Rules.getStandardRules(), field); // TODO: select rules

        // CardView init
        cardView = new CardView(getFragmentManager(), field);
        if(savedInstanceState == null)
            cardView.init(R.id.game_flip_card_left, R.id.game_flip_card_right);

        // Bluetooth
        if(!bluetoothSupport()){
            Toast.makeText(this, "Can not create game. Your device doesn't support Bluetooth technology.", Toast.LENGTH_LONG).show();
            finish();
        }
        addBluetoothDiscoveryListener(this);
        addBluetoothConnectionStateListener(this);
        addBluetoothMessageListener(this);

        // NFC
        addNFCListener(this);

        // Game functions
        continueButton.setEnabled(false);
        continueButton.setOnClickListener(this);

        // Toggle field init
        toggleField = (Button) findViewById(R.id.game_button_show_field);
        toggleField.setOnClickListener(this);

        // Card flip listener
        final RelativeLayout leftCardContainer = (RelativeLayout) findViewById(R.id.game_flip_card_left);
        final RelativeLayout rightCardContainer = (RelativeLayout) findViewById(R.id.game_flip_card_right);

        // Prepare messages
        messageHandler.prepareMessage("name", playerName);
        messageHandler.prepareMessage("firstTurn", String.valueOf(game.getTurn().type));
        messageHandler.prepareMessage("field", field.toString());

        // Doesn't work with FragmentManager method popBackStack...
        /*leftCardContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "leftCardClick");
                if(cardView.getIndex(CardView.Card.LEFT) != -1)
                    cardView.flipCard(CardView.Card.LEFT);
            }
        });
        rightCardContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "rightCardClick");
                if(cardView.getIndex(CardView.Card.RIGHT) != -1)
                    cardView.flipCard(CardView.Card.RIGHT);
            }
        });*/
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        disconnect();
        removeNFCListener(this);
        removeBluetoothDiscoveryListener(this);
        removeBluetoothConnectionStateListener(this);
        removeBluetoothMessageListener(this);
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
                .setNeutralButton("Cancel", null).show();
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
            send(messageHandler.requestMessage("name"));
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
                            if(index >= 0 && index < field.getSize() && field.countRemaining() > 0){
                                if(game != null && !game.myTurn()){
                                    Toast.makeText(this, "It's " + game.getTurn().name + "s' turn!", Toast.LENGTH_LONG).show();
                                } else if(field.getCard(index).active && index != cardView.getIndex(CardView.Card.LEFT)
                                        && index != cardView.getIndex(CardView.Card.RIGHT)) {
                                    if(!field.isDisabled(index)) {
                                        final CardView.Card flippedCard = cardView.flipCard(index);
                                        if(flippedCard != null) field.highlight(index);
                                        if(flippedCard == CardView.Card.RIGHT) {
                                            continueButton.setEnabled(true);

                                            final boolean match = CardView.Card.isMatch();
                                            if (match) {
                                                Log.i(TAG, "Match! Value: " + CardView.Card.LEFT.getCard().value);
                                                field.disable(cardView.getIndex(CardView.Card.LEFT));
                                                field.disable(cardView.getIndex(CardView.Card.RIGHT));
                                                currentMessage.getContent().put("disabled", field.getDisabled());

                                                if(field.countRemaining() < 1){
                                                    currentMessage.getContent().put("finished", true);
                                                    Toast.makeText(GameActivity.this, "Game finished", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                            if(!match || !game.getRules().hasFlag(Rules.AGAIN_ON_SCORE)){
                                                game.setTurn(game.getOpponentType());

                                                final String turn = game.getTurn().name;

                                                currentMessage.getContent().put("yourTurn", true);
                                                setTitle(turn + (turn.endsWith("s") ? "" : "s") + "' turn");
                                            }
                                        }
                                    } else Toast.makeText(this, "The card is already out of the game.", Toast.LENGTH_LONG).show();
                                } else Log.w(TAG, "Could not flip card. Card is not active or already revealed.");
                                return;
                            } else Log.e(TAG, "Could not flip card. Index out of field range.");
                        } catch(NumberFormatException e){
                            Log.e(TAG, "Could not process NFC content. Invalid message: '" + message + "'");
                        } catch (JSONException e) {
                            Log.e(TAG, "Could not create message.");
                            Toast.makeText(this, "Message could not be sent.", Toast.LENGTH_LONG).show();
                            return;
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
            final String messages[] = messageHandler.split(m);

            for(int i = 0; i < messages.length; i++) {
                final String s[] = messages[i].split(":", 2);
                final String key = s[0];
                final String val;
                try {
                    val = s[1].trim();
                } catch (IndexOutOfBoundsException e2) {
                    Log.e(TAG, "Invalid text message. Message must have pattern [key]: [value]");
                    return;
                }
                switch (key) {
                    case "name":
                        final Player player1, player2;
                        if (playerType == Player.HOST) {
                            Log.v(TAG, "Player is host, Opponent is client.");
                            lobbyClient.setText(val);
                            player1 = new Player(playerName, Player.HOST);
                            player2 = new Player(val, Player.CLIENT);
                        } else if (playerType == Player.CLIENT) {
                            Log.v(TAG, "Player is client, Opponent is host.");
                            lobbyHost.setText(val);
                            player1 = new Player(val, Player.HOST);
                            player2 = new Player(playerName, Player.CLIENT);
                        } else {
                            Toast.makeText(GameActivity.this, "Invalid player type: " + playerType, Toast.LENGTH_LONG).show();
                            return;
                        }
                        game.setHost(player1);
                        game.setClient(player2);
                        game.setPlayerType(playerType);

                        if (playerType == Player.HOST) {
                            final String turn = game.getTurn().name;

                            send(messageHandler.combine(new String[]{
                                    messageHandler.requestMessage("firstTurn"),
                                    messageHandler.requestMessage("field")
                            }));
                            setTitle(turn + (turn.endsWith("s") ? "" : "s") + "' turn");
                        }
                        break;
                    case "field":
                        field.parse(val);
                        field.print(GameActivity.this, fieldLayout);
                        break;
                    case "firstTurn":
                        game.setTurn(Integer.valueOf(val));
                        setTitle(game.getTurn().name + (game.getTurn().name.endsWith("s") ? "" : "s") + "' turn");
                        break;
                    case "game":
                        Log.d(TAG, val);
                        InGameMessage message = new InGameMessage(InGameMessage.TYPE_RECEIVED);
                        message.setContent(val);
                        if (message.getContent().has("yourTurn")) {
                            game.setTurn(playerType);
                            setTitle(playerName + (playerName.endsWith("s") ? "" : "s") + "' turn");
                            currentMessage = new InGameMessage(InGameMessage.TYPE_SENT);
                            Toast.makeText(GameActivity.this, "It's your turn!", Toast.LENGTH_LONG).show();
                        }
                        if (message.getContent().has("disabled")) {
                            try {
                                JSONArray disabled = new JSONArray(message.getContent().getString("disabled"));
                                for (int j = 0; j < disabled.length(); j++) {
                                    field.disable(disabled.getInt(j));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (message.getContent().has("finished")) {
                            Toast.makeText(GameActivity.this, "Game finished", Toast.LENGTH_LONG).show();
                        }
                        break;
                    default:
                        Toast.makeText(GameActivity.this, "Invalid message key: " + key, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.game_start:
                if (getState() != STATE_CONNECTED) {
                    Toast.makeText(GameActivity.this, "Not connected to other device.", Toast.LENGTH_LONG).show();
                } else if (game == null) {
                    Toast.makeText(GameActivity.this, "Unable to start game. No game initialized.", Toast.LENGTH_LONG).show();
                } else send(BluetoothMessage.START);
                break;
            case R.id.game_host:
                playerType = Player.HOST;

                serve("de.thm.nfcmemory", true);
                makeDiscoverable();

                bluetoothDeviceListAdapter.clear();
                joinGame.setEnabled(false);
                hostGame.setEnabled(false);
                hostGameProgress.setVisibility(View.VISIBLE);
                lobbyHost.setText(playerName);
                lobbyClient.setText("Client");
                lobbyStatus.setText("Waiting for client...");
                break;
            case R.id.game_join:
                playerType = Player.CLIENT;

                discover();

                bluetoothDeviceListAdapter.clear();
                joinGame.setEnabled(false);
                hostGame.setEnabled(false);
                joinGameProgress.setVisibility(View.VISIBLE);
                lobbyHost.setText("Host");
                lobbyClient.setText(playerName);
                lobbyStatus.setText("Searching for nearby hosted games...");
                break;
            case R.id.game_continue:
                cardView.reset();
                continueButton.setEnabled(false);
                field.resetHighlights();
                Log.d(TAG, "Sending message: " + currentMessage.toString());
                send(currentMessage.toString());
                break;
            case R.id.game_button_show_field:
                if (fieldLayout.getVisibility() != View.VISIBLE) {
                    fieldLayout.setVisibility(View.VISIBLE);
                    toggleField.setText("Hide field");
                } else {
                    fieldLayout.setVisibility(View.GONE);
                    toggleField.setText("Show field");
                }
        }
    }

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
}
