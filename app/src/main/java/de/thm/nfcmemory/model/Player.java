package de.thm.nfcmemory.model;

/**
 * Created by Nils on 10.09.2015.
 */
public class Player {
    public static final int HOST = 100;
    public static final int CLIENT = 101;

    public final String name;
    public final int type;
    private int points = 0;

    public Player(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public void addPoints(int points){
        this.points += points;
    }

    public void setPoints(int points){
        this.points = points;
    }

    public int getPoints(){
        return points;
    }
}
