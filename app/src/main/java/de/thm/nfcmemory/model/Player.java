package de.thm.nfcmemory.model;

/**
 * Created by Nils on 10.09.2015.
 */
public class Player {
    public static final int HOST = 100;
    public static final int CLIENT = 101;

    public final String name;

    public Player(String name) {
        this.name = name;
    }
}
