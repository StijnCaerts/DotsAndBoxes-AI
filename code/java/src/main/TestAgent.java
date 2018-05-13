package main;

public class TestAgent extends Agent {

    public boolean[][] edges; // true means edge has been filled in

    public TestAgent(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.edges = new boolean[2 * rows + 1][2 * columns + 1];
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        this.edges[x][y] = true;
    }

    @Override
    public int[] getNextMove() {
        // Simply fills in the next available edge
        for (int x = 0; x < 2 * columns + 1; x++) {
            for (int y = (x + 1) % 2; y < 2 * rows + 1; y += 2) {
                if (!this.edges[x][y])
                    return new int[]{x, y};
            }
        }
        return new int[]{0, 1};
    }

}
