package de.thm.nfcmemory.util;

import android.graphics.Point;

import java.util.Random;

import de.thm.nfcmemory.model.Card;

/**
 * Created by Nils on 16.08.2015.
 */
public class Functions {
    // Dustenfeld shuffle
    public static void shuffleCards(Card[] ar) {
        final Random r = new Random();
        int i, j;
        Card a;

        for(i = ar.length - 1; i > 0; i--){
            j = r.nextInt(i + 1);

            // swap
            a = ar[j];
            ar[j] = ar[i];
            ar[i] = a;
        }
    }

    public static Point getIdealFieldDimenson(final int count){
        int width = 2, height = count / width;

        while(width < height){
            if(count % ++width == 0)
                height = count / width;
        }

        return new Point(width, height);
    }
}
