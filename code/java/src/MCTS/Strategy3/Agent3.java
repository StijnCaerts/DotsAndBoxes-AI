package MCTS.Strategy3;

import MCTS.DBMove;
import MCTS.Move;
import main.Agent;

public class Agent3 extends Agent {

    private MCTS mcts;

    public Agent3(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.mcts = new MCTS();
        this.mcts.init(new GameState(columns, rows));
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        this.mcts.registerMove(new DBMove(x,y));
    }

    @Override
    public int[] getNextMove() {
        try {
            Move m = mcts.getNextMove(this.timeLimit);
            DBMove dbm = (DBMove) m;
            return new int[]{dbm.x, dbm.y};
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
