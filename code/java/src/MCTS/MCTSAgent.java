package MCTS;

import board.Board;
import main.Agent;

public class MCTSAgent extends Agent {

    public MCTS mcts;

    public MCTSAgent(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        //this.gs = new MCTS.GameState(rows, columns);
        this.mcts = new MCTS();
        this.mcts.init(new Board(columns, rows, false));
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        //this.gs.playMove(new MCTS.DBMove(x,y));
        this.mcts.registerMove(new DBMove(x,y));
    }

    @Override
    public int[] getNextMove() {
        try {
            //Move m = mcts.getNextMove(this.gs, this.timeLimit);
            Move m = mcts.getNextMove(this.timeLimit);
            DBMove dbm = (DBMove) m;
            return new int[]{dbm.x, dbm.y};
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
