package de.thm.nfcmemory.model;

/**
 * Created by Nils on 10.09.2015.
 */
public class Game {
    public final Field field;

    private final Rules rules;

    private Player host = new Player("Host", Player.HOST);
    private Player client = new Player("Client", Player.CLIENT);

    private int playerType;
    private int turn;

    public Game(Rules rules, Field field) {
        this.rules = rules;
        this.field = field;
        setTurn(-1);
    }

    public void setHost(Player host){
        this.host = host;
    }

    public void setClient(Player client){
        this.client = client;
    }

    public void setPlayerType(int playerType){
        this.playerType = playerType;
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
