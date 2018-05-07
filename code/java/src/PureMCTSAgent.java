import MCTS.MCTS;
import MCTS.Move;
import MCTS.FinalSelectionPolicy;

public class PureMCTSAgent extends Agent {

    MCTS mcts;
    GameState gs;
    public PureMCTSAgent(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.mcts = new MCTS();
        this.mcts.setMoveSelectionPolicy(FinalSelectionPolicy.maxChild);
        this.gs = new GameState(rows, columns);
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        this.gs.makeMove(new DBMove(x,y));
    }

    @Override
    public int[] getNextMove() {
        try {
            Move m = mcts.runMCTS_UCT(this.gs, 24000, true);
            return new int[]{m.getX(), m.getY()};
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

}
