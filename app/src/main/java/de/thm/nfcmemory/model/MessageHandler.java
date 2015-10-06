package de.thm.nfcmemory.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Nils on 06.10.2015.
 */
public class MessageHandler {
    public static final String TAG = "MessageHandler";
    public static final String MESSAGE_DELIMITER = "|";

    private final ArrayList<String> requestedUniqueKeys = new ArrayList<>();

    public String requestMessage(String key){
        return requestMessage(key, false);
    }
    public String requestMessage(String key, boolean force){
        final Key msgKey = Key.findByName(key);
        if(msgKey == null){
            Log.e(TAG, "Message '" + key + "' does not exist.");
            return null;
        } else if(msgKey.preparedMessage == null){
            Log.e(TAG, "Message '" + key + "' was not prepared.");
            return null;
        } else if(msgKey.unique){
            if(msgKey.counter > 0){
                Log.e(TAG, "Unique message '" + key + "' was already requested.");
                if(force){
                    Log.v(TAG, "Forced to get unique message '" + key + "'.");
                    return msgKey.preparedMessage;
                } else return null;
            }
        }

        msgKey.counter++;
        Log.v(TAG, "Message request counter of '" + key + "' set to " + msgKey.counter);
        return msgKey.preparedMessage;
    }

    public boolean prepareMessage(String key, String preparedMessage){
        final Key msgKey = Key.findByName(key);
        if(msgKey.preparedMessage == null){
            msgKey.preparedMessage = key + ": " + preparedMessage + MESSAGE_DELIMITER;
            return true;
        }

        Log.e(TAG, "Message '" + key + "' was already prepared.");
        return false;
    }

    public String[] split(String message){
        final ArrayList<String> list = new ArrayList<>(Arrays.asList(message.split("\\" + MESSAGE_DELIMITER)));
        final int c = list.size();
        for(int i = 0; i < c; i++){
            String s = list.get(i);
            Log.v(TAG, "Extracted message no. " + (i+1) + ": " + s);
            if(s == null || s == "") list.remove(i);
        }
        return list.toArray(new String[c]);
    }

    public String combine(String messages[]){
        String s = "";
        for(int i = 0; i < messages.length; i++){
            if(messages[i] == null) continue;
            s += messages[i];
            if(!messages[i].endsWith(MESSAGE_DELIMITER))
                s += MESSAGE_DELIMITER;
        }
        return s;
    }

    public void reset(){
        requestedUniqueKeys.clear();
        Key.reset();
    }

    private enum Key{
        Name("name", true),
        FirstTurn("firstTurn", true),
        Field("field", true),
        Game("game", false);

        private final String name;
        private final boolean unique;
        private int counter = 0;
        private String preparedMessage;

        Key(String name, boolean unique){
            this.name = name;
            this.unique = unique;
        }

        private static Key findByName(String name){
            for(Key v : values())
                if (v.name.equals(name)) return v;
            return null;
        }

        private static void reset(){
            for(Key v : values()) {
                v.preparedMessage = null;
                v.counter = 0;
            }
        }
    }
}
