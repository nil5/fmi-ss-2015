package de.thm.nfcmemory.model;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import de.thm.nfcmemory.NFCMemory;
import de.thm.nfcmemory.util.Functions;

/**
 * Created by Nils on 16.08.2015.
 */
public class Field {
    private static final String TAG = "Field";
    private static final int PADDING_PERCENTAGE = 2;

    private CardSet cardSet;
    private Card cards[];

    public Field(CardSet cardSet){
        this.cardSet = cardSet;
    }

    public void print(Context context, RelativeLayout container){
        final int startId = 1000;
        final int s = cardSet.size();
        final int w = container.getWidth();
        final Point fieldDimension = Functions.getIdealFieldDimenson(s * 2);
        final int padding = Math.round(PADDING_PERCENTAGE / 100f * w);
        final int size = (w - (fieldDimension.x + 1) * padding) / fieldDimension.x;

        Log.v(TAG, "s: " + s + ", w: " + w + ", fieldDimensions (" + fieldDimension.x + "|" + fieldDimension.y + "), padding: " + padding + ", size: " + size);

        int i, id;
        Card temp;
        TextView card;
        LayoutParams params;

        cards = new Card[s * 2];

        for(i = 0; i < s; i++){
            temp = cardSet.get(i);
            cards[i * 2] = temp;
            cards[i * 2 + 1] = temp;
        }

        Functions.shuffleCards(cards);

        for(i = 0; i < cards.length; i++){
            id = startId + i;

            card = new TextView(context);
            card.setText(Integer.toString(i + 1));
            card.setTextColor(Color.BLACK);
            card.setGravity(Gravity.CENTER);
            card.setId(id);

            params = new LayoutParams(size, size);
            params.setMargins(padding, padding, 0, 0);
            if(id - fieldDimension.x < startId) {
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            } else {
                params.addRule(RelativeLayout.BELOW, id - fieldDimension.x);
            }

            if(i % fieldDimension.x == 0){
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            } else {
                params.addRule(RelativeLayout.RIGHT_OF, id - 1);
            }

            card.setLayoutParams(params);
            card.setBackgroundColor(Color.WHITE);
            container.addView(card);
        }
    }
}
