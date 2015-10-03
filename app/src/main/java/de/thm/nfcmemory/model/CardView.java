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
        Card.LEFT.id = leftLayoutId;
        Card.RIGHT.id = rightLayoutId;

        fragmentManager
                .beginTransaction()
                .add(R.id.game_flip_card_left, new CardBackFragment())
                .add(R.id.game_flip_card_right, new CardBackFragment())
                .commit();
    }

    public Card flipCard(int index){
        if(flipCounter + 1 % 3 == 0) return null;
        flipCounter++;
        Log.d(TAG, "flipCounter: " + flipCounter);
        switch(flipCounter % 3){
            case 1:
                Card.LEFT.flip(index);
                return Card.LEFT;
            case 2:
                Card.RIGHT.flip(index);
                return Card.RIGHT;
            default:
                return null;
        }

    }

    public void reset(){
        flipCounter++;
        Card.LEFT.flip(-1);
        Card.RIGHT.flip(-1);
    }

    public void flipCard(Card card){
        card.flip(card.index);
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
        private int index = -1;
        private CardFrontFragment cardFront;

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
