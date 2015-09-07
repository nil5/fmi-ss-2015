package de.thm.nfcmemory.model;

import android.graphics.Bitmap;
import android.view.View;

/**
 * Created by Nils on 13.07.2015.
 */
public class Card {
    private String id;
    private Bitmap src;

    public Card(String id, Bitmap src){
        this.id = id;
        this.src = src;
    }

    public Bitmap getSrc(){
        return src;
    }

    public String getId(){
        return id;
    }
}
