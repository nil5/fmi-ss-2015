package de.thm.nfcmemory.model;

import android.graphics.Bitmap;
import android.view.View;

/**
 * Created by Nils on 13.07.2015.
 */
public class Card {
    public final String id;
    public final Bitmap src;
    public final int value;
    public boolean active = true;

    public Card(String id, int value, Bitmap src){
        this.id = id;
        this.src = src;
        this.value = value;
    }

    public Bitmap getSrc(){
        return src;
    }

    public String getId(){
        return id;
    }
}
