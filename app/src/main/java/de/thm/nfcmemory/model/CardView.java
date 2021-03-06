package de.thm.nfcmemory.model;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;

import de.thm.nfcmemory.NFCMemory;
import de.thm.nfcmemory.R;

/**
 * Created by Nils on 03.10.2015.
 */
public class CardView {
    public static final String TAG = "CardView";
    private static FragmentManager fragmentManager;
    private static Field field;
    private int flipCounter = 0;

    public CardView(FragmentManager fragmentManager, Field field){
        CardView.fragmentManager = fragmentManager;
        CardView.field = field;

        Card.LEFT.cardFront = new CardFrontFragment();
        Card.RIGHT.cardFront = new CardFrontFragment();
    }

    public void init(int leftLayoutId, int rightLayoutId){
        Card.LEFT.init(leftLayoutId);
        Card.RIGHT.init(rightLayoutId);

        fragmentManager
                .beginTransaction()
                .add(R.id.game_flip_card_left, new CardBackFragment())
                .add(R.id.game_flip_card_right, new CardBackFragment())
                .commit();
    }

    public void showCard(Card card){
        if(!card.visible) card.flip(card.index);
    }

    public void hideCard(Card card){
        if(card.visible) card.flip(card.index);
    }

    public Card flipCard(int index, boolean hidden){
        if((flipCounter + 1) % 3 == 0){
            Log.w(TAG, "Could not flip card. Only 2 cards can be flipped per turn.");
            return null;
        }
        flipCounter++;
        Log.d(TAG, "flipCounter: " + flipCounter);
        switch(flipCounter % 3){
            case 1:
                if(!hidden) Card.LEFT.flip(index);
                else{
                    Card.LEFT.index = index;
                    Card.LEFT.cardFront.setImage(field.getCard(index).getSrc());
                }
                return Card.LEFT;
            case 2:
                if(!hidden) Card.RIGHT.flip(index);
                else{
                    Card.RIGHT.index = index;
                    Card.RIGHT.cardFront.setImage(field.getCard(index).getSrc());
                }
                return Card.RIGHT;
            default:
                return null;
        }

    }

    public void reset(){
        flipCounter++;
        if(Card.LEFT.visible) Card.LEFT.flip(-1);
        else Card.LEFT.index = -1;
        if(Card.RIGHT.visible) Card.RIGHT.flip(-1);
        else Card.RIGHT.index = -1;
    }

    public int getIndex(){
        switch(flipCounter % 3){
            case 1:
                return getIndex(Card.LEFT);
            case 2:
                return getIndex(Card.RIGHT);
            default:
                return 0;
        }
    }

    public int getIndex(Card card){
        return card.index;
    }

    public enum Card {
        LEFT, RIGHT;

        private boolean visible;
        private int id;
        private int index;
        private CardFrontFragment cardFront;

        private void init(int id){
            this.id = id;
            index = -1;
            visible = false;
        }

        public de.thm.nfcmemory.model.Card getCard(){
            return field.getCard(index);
        }

        public static boolean isMatch(){
            if(LEFT.index == -1 || RIGHT.index == -1) return false;
            Log.v(TAG, "Check match: " + LEFT.getCard().value + " == " + RIGHT.getCard().value);
            return LEFT.getCard().value == RIGHT.getCard().value;
        }

        private void flip(int index){
            Log.d(TAG, "flip card");
            if (visible) {
                fragmentManager.popBackStack();
                visible = false;
                this.index = index;
                return;
            }

            // Flip to the front.
            visible = true;

            final boolean revealed = this.index == index;
            if(!revealed) cardFront.setImage(field.getCard(index).getSrc());
            this.index = index;

            // Create and commit a new fragment transaction that adds the fragment for the back of
            // the card, uses custom animations, and is part of the fragment manager's back stack.

            fragmentManager
                    .beginTransaction()

                            // Replace the default fragment animations with animator resources representing
                            // rotations when switching to the back of the card, as well as animator
                            // resources representing rotations when flipping back to the front (e.g. when
                            // the system Back button is pressed).
                    .setCustomAnimations(
                            R.animator.animation_card_flip_right_in, R.animator.animation_card_flip_right_out,
                            R.animator.animation_card_flip_left_in, R.animator.animation_card_flip_left_out)

                            // Replace any fragments currently in the container view with a fragment
                            // representing the next page (indicated by the just-incremented currentPage
                            // variable).
                    .replace(id, cardFront)

                            // Add this transaction to the back stack, allowing users to press Back
                            // to get to the front of the card.
                    .addToBackStack(null)

                            // Commit the transaction.
                    .commit();
        }
    }


    /**
     * card front.
     */
    public static class CardFrontFragment extends Fragment {
        private Bitmap bmp;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final File sd = Environment.getExternalStorageDirectory();
            final File file = new File(sd + NFCMemory.Const.SD_FOLDER + "/CardBacks/" + "default.png"); // TODO: Custom card backs

            ImageView v = (ImageView) inflater.inflate(R.layout.card, container, false);

            if(file.exists())
                v.setImageBitmap(BitmapFactory.decodeFile(String.valueOf(file)));

            return v;
        }
    }
}
