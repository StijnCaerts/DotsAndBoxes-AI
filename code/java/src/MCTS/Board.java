package MCTS;

import java.util.*;

public interface Board {

    public abstract double gameResult();
    public abstract Set<Move> getMoves();
    public abstract void playMove(Move move);
    public abstract Board duplicate();
    public abstract boolean gameDecided();
    public abstract int getNextTurnPlayer();

    public default boolean hasOptimalMoves() {
        return false;
    }

    public default Set<Move> getOptimal() {
        return new HashSet<Move>();
    }

    public default Move getRandomMove() {
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
