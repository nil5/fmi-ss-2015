package de.thm.nfcmemory.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Nils on 04.10.2015.
 */
public class InGameMessage {
    public static final int TYPE_RECEIVED = 1;
    public static final int TYPE_SENT = 2;

    private static final ArrayList<InGameMessage> RECEIVED = new ArrayList<>();
    private static final ArrayList<InGameMessage> SENT = new ArrayList<>();
    private static final String IDENTIFIER = "game:";

    private JSONObject content = new JSONObject();

    public InGameMessage(int type){
        if(type == TYPE_RECEIVED) RECEIVED.add(this);
        else if(type == TYPE_SENT) SENT.add(this);
    }

    public void setContent(String json){
        try {
            content = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public JSONObject getContent(){
        return content;
    }

    public String toString(){
        return IDENTIFIER + content.toString();
    }
}
