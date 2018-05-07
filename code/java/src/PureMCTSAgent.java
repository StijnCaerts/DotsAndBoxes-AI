public class PureMCTSAgent extends Agent {

    public PureMCTSAgent(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        //TODO
    }

    @Override
    public int[] getNextMove() {
        //TODO
        return new int[0];
    }

}
