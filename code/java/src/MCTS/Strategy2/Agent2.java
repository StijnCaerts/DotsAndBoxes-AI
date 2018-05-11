package MCTS.Strategy2;

import main.Agent;
import MCTS.DBMove;
import MCTS.Move;

public class Agent2 extends Agent {

    private MCTS mcts;
    private GameState gs;

    public Agent2(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.gs = new GameState(rows, columns);
        this.mcts = new MCTS();
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        this.gs.playMove(new DBMove(x,y));
    }

    @Override
    public int[] getNextMove() {
        try {
            Move m = mcts.getNextMove(this.gs, this.timeLimit);
            DBMove dbm = (DBMove) m;
            return new int[]{dbm.x, dbm.y};
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
