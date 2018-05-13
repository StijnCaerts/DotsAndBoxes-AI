package board;

public class MoveIterator {

    // Coordinates indicate next coordinate to start searching from
    public Board board;
    public int x;
    public int y;
    public int count;

    public MoveIterator(Board board) {
        this.board = board;
        this.x = 0;
        this.y = 1;
        this.count = 0;
    }

    public boolean hasNext() {
        // Checks if the board still has legal moves left
        return this.count < this.board.movesLeft;
    }

    public int[] getNextMove() {
        // Yields the next legal move
        if (hasNext()) {
            while (true) {
                if (!this.board.edges[this.x][this.y]) {
                    int prevX = this.x;
                    int prevY = this.y;
                    this.count++;
                    nextPos();
                    return new int[]{prevX, prevY};
                }
                nextPos();
            }
        }
        return null;
    }

    private void nextPos() {
        this.y += 2;
        if (this.y >= 2 * board.rows + 1) {
            // Shift to next column
            this.x++;
            this.y = (this.x + 1) % 2;
        }
    }

}
