package de.thm.nfcmemory.model;

import java.util.ArrayList;

/**
 * Created by Nils on 15.09.2015.
 */
public class Rules {
    public static final int ALLOW_SWAPPING = 100;
    public static final int AGAIN_ON_SCORE = 101;
    public static final int SECRET_DRAW = 102;

    private ArrayList<Integer> flags = new ArrayList<>();

    public Rules(){

    }

    public Rules addFlag(int flag){
        flags.add(flag);
        return this;
    }

    public Rules removeFlag(int flag){
        flags.remove(flag);
        return this;
    }

    public boolean hasFlag(int flag){
        return flags.contains(flag);
    }

    // Predefined Rules
    public static Rules getStandardRules(){
        return new Rules()
                .addFlag(AGAIN_ON_SCORE);
    }
}
