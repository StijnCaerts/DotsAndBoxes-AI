package main;

import board.Board;

public class AlphaBeta {

    // Performs alpha-beta search on Board
    // Based on https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Pseudocode

    public static int search(Board board) {

        // Returns -1 (player 2 wins), 0 (tie) or 1 (player 1 wins) in best outcome for current player
        boolean temp = board.recordUndo;
        board.recordUndo = true;
        int res = AlphaBeta.alphaBeta(board, Integer.MIN_VALUE, Integer.MAX_VALUE);
        board.recordUndo = temp;
        return res;

    }

    public static int alphaBeta(Board board, double alpha, double beta) {

        // Returns -1 (player 2 wins), 0 (tie) or 1 (player 1 wins) in best outcome for current player

        if (board.scores[0] > board.columns*board.rows/2) {
            return 1;
        }
        if (board.scores[1] > board.columns*board.rows/2) {
            return -1;
        }
        if (board.getLegalMoves().size() == 0) {
            return board.scores[1] - board.scores[2];
        }

        if (board.currentPlayer == 1) {

            // Maximizing player
            int value = Integer.MIN_VALUE;
            if (board.hasOptimalMoves()) {
                for(int edge : board.getOptimalMoves()) {
                    // Check all optimal moves
                    int[] edgeCoords = board.intToEdge(edge);
                    board.registerMove(edgeCoords[0], edgeCoords[1]);
                    value = Math.max(value, AlphaBeta.alphaBeta(board, alpha, beta));
                    board.undo();
                    alpha = Math.max(alpha, value);
                    if (beta <= alpha)
                        break;
                }
            } else {
                for(int edge : board.getLegalMoves()) {
                    // Check all legal moves
                    int[] edgeCoords = board.intToEdge(edge);
                    board.registerMove(edgeCoords[0], edgeCoords[1]);
                    value = Math.max(value, AlphaBeta.alphaBeta(board, alpha, beta));
                    board.undo();
                    alpha = Math.max(alpha, value);
                    if (beta <= alpha)
                        break;
                }
            }
            return value;

        } else {

            // Minimizing player
            int value = Integer.MAX_VALUE;
            if (board.hasOptimalMoves()) {
                for(int edge : board.getOptimalMoves()) {
                    // Check all optimal moves
                    int[] edgeCoords = board.intToEdge(edge);
                    board.registerMove(edgeCoords[0], edgeCoords[1]);
                    value = Math.min(value, AlphaBeta.alphaBeta(board, alpha, beta));
                    board.undo();
                    alpha = Math.min(alpha, value);
                    if (beta <= alpha)
                        break;
                }
            } else {
                for(int edge : board.getLegalMoves()) {
                    // Check all legal moves
                    int[] edgeCoords = board.intToEdge(edge);
                    board.registerMove(edgeCoords[0], edgeCoords[1]);
                    value = Math.min(value, AlphaBeta.alphaBeta(board, alpha, beta));
                    board.undo();
                    beta = Math.min(alpha, value);
                    if (beta <= alpha)
                        break;
                }
            }
            return value;

        }

    }

}
