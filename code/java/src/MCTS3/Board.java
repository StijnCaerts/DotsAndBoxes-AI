package MCTS3;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

public abstract class Board {
    int next_turn_player;

    public abstract double gameResult();
    public abstract Set<Move> getMoves();
    public abstract void playMove(Move move);
    public abstract Board duplicate();

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
