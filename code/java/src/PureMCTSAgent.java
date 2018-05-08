import MCTS2.MCTS;
import MCTS2.Move;
import MCTS2.FinalSelectionPolicy;

public class PureMCTSAgent extends Agent {

    MCTS mcts;
    GameState gs;
    public PureMCTSAgent(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.mcts = new MCTS();
        this.mcts.enableRootParallelisation(16);
        this.gs = new GameState(rows, columns);
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        this.gs.makeMove(new DBMove(x,y));
    }

    @Override
    public int[] getNextMove() {
        try {
            Move m = mcts.findBestMove(this.gs, 24000, true);
            DBMove dbm = (DBMove) m;
            return new int[]{dbm.x, dbm.y};
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

}
