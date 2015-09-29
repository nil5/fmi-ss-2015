package de.thm.nfcmemory.model;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
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
    private TextView views[];
    private int size;

    public Field(CardSet cardSet){
        this.cardSet = cardSet;
    }

    @TargetApi(16)
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

        this.size = s * 2;
        cards = new Card[this.size];
        views = new TextView[this.size];

        for(i = 0; i < s; i++){
            temp = cardSet.get(i);
            cards[i * 2] = temp;
            cards[i * 2 + 1] = temp;
        }

        Functions.shuffleCards(cards);

        for(i = 0; i < cards.length; i++){
            final BitmapDrawable drawable = new BitmapDrawable(context.getResources(), cards[i].getSrc());
            id = startId + i;

            card = new TextView(context);
            card.setText(Integer.toString(i + 1));
            card.setTextColor(Color.BLACK);
            card.setGravity(Gravity.CENTER);
            card.setId(id);
            if(NFCMemory.Const.API >= 16)
                card.setBackground(drawable);
            else card.setBackgroundDrawable(drawable);

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
            views[i] = card;
        }
    }

    public void setCardColor(int index, String hex){
        if(views == null || index < 0 || index >= views.length) return;
        views[index].setBackgroundColor(Color.parseColor(hex));
    }

    public Card getCard(int index){
        if(views == null || index < 0 || index >= cards.length) return null;
        return cards[index];
    }

    public int getSize(){
        return size;
    }
}
