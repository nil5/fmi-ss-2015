package de.thm.nfcmemory.model;

/**
 * Created by Nils on 10.09.2015.
 */
public class Game {
    public final Player host;
    public final Player client;
    public final Field field;
    public final int playerType;

    private final Rules rules;
    private int turnCounter = 0;
    private int turn;

    public Game(Player host, Player client, int playerType, Rules rules, Field field) {
        this.host = host;
        this.client = client;
        this.rules = rules;
        this.field = field;
        this.playerType = playerType;
        setTurn(-1);
    }

    public void setTurn(int playerType){
        if(playerType == Player.CLIENT || playerType == Player.HOST)
            turn = playerType;
        else setTurn(getRandomPlayerType());
    }

    public Player getTurn(){
        if(turn == Player.CLIENT) return client;
        else if(turn == Player.HOST) return host;
        return null;
    }

    public boolean myTurn(){
        final Player next = getTurn();
        return playerType == Player.HOST && next == host
                || playerType == Player.CLIENT && next == client;
    }

    private Player getRandomPlayer(){
        if(Math.random() < 0.5) return host;
        else return client;
    }

    private int getRandomPlayerType(){
        if(Math.random() < 0.5) return Player.HOST;
        else return Player.CLIENT;
    }

    public Player getPlayerFromType(int type){
        if(type == Player.HOST) return host;
        else if(type == Player.CLIENT) return client;
        else return null;
    }

    public Player getOpponent(){
        if(playerType == Player.HOST) return client;
        else return host;
    }

    public int getOpponentType(){
        return playerType == Player.HOST ? Player.CLIENT : Player.HOST;
    }

    public Rules getRules(){
        return rules;
    }
}
