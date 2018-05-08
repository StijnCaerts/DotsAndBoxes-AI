import MCTS3.MCTS;
import MCTS3.Move;

public class MCTSAgent2 extends Agent {

    public MCTS mcts;
    GameState2 gs;
    public MCTSAgent2(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.gs = new GameState2(rows, columns);
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
