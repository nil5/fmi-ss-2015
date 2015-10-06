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
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;

import de.thm.nfcmemory.NFCMemory;
import de.thm.nfcmemory.util.Functions;

/**
 * Created by Nils on 16.08.2015.
 */
public class Field {
    private static final String TAG = "Field";
    private static final int PADDING_PERCENTAGE = 2;

    private final ArrayList<Integer> highlighted = new ArrayList<>();
    private final ArrayList<Integer> disabled = new ArrayList<>();

    private CardSet cardSet;
    private Card cards[];
    private TextView views[];
    private int width;
    private int height;
    private int size;

    public Field(CardSet cardSet){
        this(cardSet, 0);
    }

    public Field(CardSet cardSet, int width){
        this.cardSet = cardSet;
        this.width = width;

        size = this.cardSet.size() * 2;
        views = new TextView[this.size];
        cards = Field.getCards(this.cardSet);

        Functions.shuffleCards(cards);
    }

    @TargetApi(16)
    public void print(Context context, RelativeLayout container){
        final int startId = 1000;
        final int w = width == 0 ? container.getWidth() : width;
        final Point fieldDimension = Functions.getIdealFieldDimenson(this.size);
        final int padding = Math.round(PADDING_PERCENTAGE / 100f * w);
        final int size = (w - (fieldDimension.x + 1) * padding) / fieldDimension.x;

        height = fieldDimension.y * size + (fieldDimension.y + 1) * padding;

        Log.v(TAG, "s: " + this.size + ", w: " + w + ", fieldDimensions (" + fieldDimension.x + "|" + fieldDimension.y + "), padding: " + padding + ", size: " + size);

        int id;
        TextView card;
        LayoutParams params;

        for(int i = 0; i < cards.length; i++){
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
            if(id - fieldDimension.x < startId) {
                params.setMargins(padding, padding, 0, padding);
            } else {
                params.setMargins(padding, 0, 0, padding);
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

            if(highlighted.contains(i))
                highlight(i);
            if(disabled.contains(i))
                disable(i);
        }
    }

    public void highlight(int index){
        highlighted.add(index);
        setCardColor(index, "#00FF00");
    }

    public void resetHighlights(){
        for(int i = highlighted.size() - 1; i >= 0; i--){
            final int index = highlighted.get(i);
            highlighted.remove(i);
            if(getCard(index).active) setCardColor(index, "#FFFFFF");
        }
    }

    public void disable(int index){
        if(!disabled.contains(index)) disabled.add(index);
        getCard(index).active = false;
        setCardColor(index, "#AAAAAA");
    }

    public boolean isDisabled(int index){
        return !getCard(index).active;
    }

    public ArrayList<Integer> getDisabled(){
        return disabled;
    }

    public void setCardColor(int index, String hex){
        if(views == null || index < 0 || index >= views.length) return;
        views[index].setBackgroundColor(Color.parseColor(hex));
    }

    public Card getCard(int index){
        if(views == null || index < 0 || index >= cards.length) return null;
        return cards[index];
    }

    public void swap(int index1, int index2){
        if(index1 < 0 || index2 < 0 || index1 >= size || index2 >= size) return;
        Card temp = cards[index1];
        cards[index1] = cards[index2];
        cards[index2] = temp;
        TextView view = views[index1];
        views[index1] = views[index2];
        views[index2] = view;
    }

    public int countRemaining(){
        return size - disabled.size();
    }

    public int getSize(){
        return size;
    }
    public int getHeight(){
        return height;
    }

    @Override
    public String toString() {
        String field = cardSet.name;
        for(int i = 0; i < cards.length; i++){
            field += ";" + i + "-" + cards[i].id;
        }
        return field;
    }

    public void parse(String s){
        String data[] = s.split(";");
        Log.v(TAG, "Splitted data array for field parsing. Length: " + data.length);
        try {
            final CardSet cardset = new CardSet(data[0]);
            final int cardSetSize = cardset.size();
            Log.v(TAG, "Loaded card set. Name: " + data[0] + ", Size: " + cardSetSize);

            if(cardSetSize * 2 != data.length - 1){
                Log.e(TAG, "The requested card set hast a different size than the installed one (" + cardSetSize + " - " + (data.length - 1) + ").");
                return;
            }

            Card cardArr[] = new Card[cardSetSize * 2];
            Log.v(TAG, "Created card array. Size: " + (cardSetSize * 2));

            final TreeMap<Integer, Card> map = new TreeMap<>();
            final Card cards[] = Field.getCards(cardset);
            for(int i = 0; i < cards.length; i++) {
                for (int j = 1; j < data.length; j++){
                    if(map.containsKey(j - 1)) continue;
                    Log.v(TAG, "i: " + i + ", j: " + j + ", Current card: " + cards[i].id + ", Current data: " + data[j]);
                    String subData[] = data[j].split("-", 2);
                    if(subData.length < 2){
                        Log.e(TAG, "Parsing error. Invalid subdata at index " + j + ": " + data[j]);
                        return;
                    } else if(cards[i].id.equals(subData[1])){
                        map.put(j - 1, cards[i]);
                        Log.v(TAG, "Card mapped.");
                    }
                }
            }

            final int mapSize = map.size();
            if(cardArr.length != mapSize){
                Log.e(TAG, "Sorting the field failed (" + cardArr.length + " != " + mapSize + ").");
                return;
            }

            for(int i = 0; i < cardArr.length; i++){
                cardArr[i] = map.get(i);
                Log.v(TAG, cardArr[i].id);
            }

            this.cardSet = cardset;
            this.cards = cardArr;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "The requested card set is not installed on this device.");
            e.printStackTrace();
        }
    }

    private static Card[] getCards(CardSet cardSet){
        final int size = cardSet.size() * 2;
        final Card cards[] = new Card[size];

        Card temp;
        for(int i = 0; i < size; i += 2){
            temp = cardSet.get(i / 2);
            cards[i] = temp;
            cards[i + 1] = temp;
        }

        return cards;
    }
}
