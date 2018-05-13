package board;

import java.util.ArrayList;

public class Transaction {

    // Stores data to revert the board back one move

    // Main part of move
    public final int x, y;
    public final BoardState state;
    public final int[] optimalMoves;
    public final int currentPlayer;
    public final int[] scores;

    // Box updates
    public int boxesAmount = 0;
    public int[][] boxCoords = new int[2][];
    public ArrayList<SubTransaction> subTransactions = new ArrayList<>();

    public Transaction(int x, int y, BoardState state, int[] optimalMoves, int currentPlayer, int[] scores) {
        this.x = x;
        this.y = y;
        this.state = state;
        this.optimalMoves = optimalMoves; // We don't copy this because Board creates a new array for this every update anyway
        this.scores = new int[]{scores[0], scores[1]};
        this.currentPlayer = currentPlayer;
    }

}
