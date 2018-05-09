public abstract class Agent {

    public final int player;
    public final double timeLimit;
    public final int rows, columns;
    public String gameId;

    public Agent(int player, double timeLimit, int rows, int columns, String gameId) {
        // first player has index 0
        this.player = player;
        this.timeLimit = timeLimit;
        this.rows = rows;
        this.columns = columns;
        this.gameId = gameId;
    }

    // Is called to notify the agent of a new move which was made
    // x, y are in the edge coordinate system (so in a grid of size (2*rows + 1)x(2*columns + 1))
    public abstract void registerAction(int ownScore, int opponentScore, int x, int y);

    // Returns a x,y-tuple in the edge coordinate system (so in a grid of size (2*rows + 1)x(2*columns + 1))
    public abstract int[] getNextMove();

}
