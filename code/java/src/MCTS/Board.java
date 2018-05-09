package MCTS;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public abstract class Board {

    public abstract double gameResult();
    public abstract Set<Move> getMoves();
    public abstract void playMove(Move move);
    public abstract Board duplicate();
    public abstract boolean gameDecided();
    public abstract int getNextTurnPlayer();

    public Move getRandomMove() {
        if(!getMoves().isEmpty()) {
            Set<Move> moves = this.getMoves();
            ArrayList<Move> ms = new ArrayList<>(moves);
            int i = new Random().nextInt(ms.size());
            return ms.get(i);
        } else {
            return null;
        }
    }

}
