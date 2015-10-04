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
    private int firstTurn;

    public Game(Player host, Player client, int playerType, Rules rules, Field field) {
        this.host = host;
        this.client = client;
        this.rules = rules;
        this.field = field;
        this.playerType = playerType;
    }

    public void setFirstTurn(Player player){
        if(player == host) firstTurn = Player.HOST;
        else if(player == client) firstTurn = Player.CLIENT;
        else setFirstTurn(getRandomPlayer());
    }

    public Player getNextTurn(){
        if(turnCounter % 2 == 0){
            if(firstTurn == Player.CLIENT) return client;
        } else if(firstTurn == Player.HOST) return client;
        return host;
    }

    public boolean myTurn(){
        final Player next = getNextTurn();
        return playerType == Player.HOST && next == host
                || playerType == Player.CLIENT && next == client;
    }

    private Player getRandomPlayer(){
        if(Math.random() < 0.5) return host;
        else return client;
    }


}
