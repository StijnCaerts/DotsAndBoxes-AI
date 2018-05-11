package board;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class RandomMoveGenerator {

    // Allows MCTS to quickly generate new random moves
    // Creation costs O(columns) time, getting a new move costs O(columns + rows) time

    public Board board;
    public int movesLeft;
    public int[] movesLeftPerColumn; // Used to quickly iterate and random-access moves
    public HashSet<Integer> generatedMoves;

    public RandomMoveGenerator(Board board) {
        this.board = board;
        this.movesLeft = board.movesLeft;
        this.movesLeftPerColumn = Arrays.copyOf(board.movesLeftPerColumn, board.movesLeftPerColumn.length);
        this.generatedMoves = new HashSet<>();
    }

    public boolean hasMovesLeft() {
        return this.movesLeft > 0;
    }

    public void reverseMove(int move) {
        // Adds moves again to possible set of moves for this generator
        int[] edge = this.board.intToEdge(move);
        this.movesLeft++;
        this.movesLeftPerColumn[edge[0]]++;
        this.generatedMoves.remove(move);
    }

    public int getRandomLegalMoveAsInt(Random rand) {
        return this.board.edgeToInt(getRandomLegalMove(rand));
    }

    public int[] getRandomLegalMove(Random rand) {
        if (this.movesLeft == 0) {
            return null;
        } else {
            int index = rand.nextInt(this.movesLeft);
            for(int x = 0; x < 2*this.board.columns + 1; x++) {
                if (index >= this.movesLeftPerColumn[x]) {
                    // Move is not in this column, move on
                    index -= this.movesLeftPerColumn[x];
                    continue;
                } else {
                    // Move is in this column, iterate through rows
                    for(int y = (x + 1)%2; y < 2*this.board.rows + 1; y += 2) {
                        if (!this.board.edges[x][y] && !this.generatedMoves.contains(this.board.edgeToInt(x, y))) {
                            // Edge is open on board and hasn't been generated as an earlier move yet
                            if (index == 0) {
                                this.movesLeft--;
                                this.movesLeftPerColumn[x]--;
                                this.generatedMoves.add(this.board.edgeToInt(x, y));
                                return new int[] {x, y};
                            }
                            index--;
                        }
                    }
                }
            }
            assert(false);
            return null;
        }
    }

}
