package main;

import board.Board;
import board.MoveIterator;

public class AlphaBeta {

    // Performs alpha-beta search on Board
    // Based on https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Pseudocode

    public static int search(Board board) {

        // Returns -1 (player 1 wins), 0 (tie) or 1 (player 0 wins) in best outcome for current player
        boolean temp = board.recordUndo;
        board.recordUndo = true;
        int[] killerMoves = new int[board.movesLeft];
        int res = AlphaBeta.alphaBeta(board, killerMoves, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        board.recordUndo = temp;
        return res;

    }

    public static int alphaBeta(Board board, int[] killerMoves, double alpha, double beta, boolean print) {

        // Returns -1 (player 1 wins), 0 (tie) or 1 (player 0 wins) in best outcome for current player

        if (board.scores[0] > board.columns*board.rows/2) {
            return 1;
        }
        if (board.scores[1] > board.columns*board.rows/2) {
            return -1;
        }
        if (board.movesLeft == 0) {
            return (int) Math.signum(board.scores[0] - board.scores[1]);
        }

        if (board.currentPlayer == 0) {

            // Maximizing player
            int value = Integer.MIN_VALUE;
            if (board.hasOptimalMoves()) {
                for(int edge : board.getOptimalMoves()) {
                    // Check all optimal moves
                    int[] edgeCoords = board.intToEdge(edge);
                    board.registerMove(edgeCoords[0], edgeCoords[1]);
                    value = Math.max(value, AlphaBeta.alphaBeta(board, killerMoves, alpha, beta, false));
                    board.undo();
                    alpha = Math.max(alpha, value);
                    if (beta <= alpha)
                        break;
                }
            } else {
                int counter = 0;

                // Play killer move first
                int killerMove = killerMoves[board.movesLeft - 1];
                if (killerMove != 0) {
                    int[] edgeCoords = board.intToEdge(killerMove);
                    // Check if killer move is legal
                    if (!board.edges[edgeCoords[0]][edgeCoords[1]]) {
                        if (print) {
                            System.out.println("Processing child " + ++counter + "/" + board.movesLeft);
                        }
                        board.registerMove(edgeCoords[0], edgeCoords[1]);
                        value = Math.max(value, AlphaBeta.alphaBeta(board, killerMoves, alpha, beta, false));
                        board.undo();
                        alpha = Math.max(alpha, value);
                        if (beta <= alpha) {
                            return value;
                        }
                    }
                }

                for(MoveIterator it = board.getLegalMoveIterator(); it.hasNext(); ) {
                    // Check all legal moves except for killer move
                    if (print) {
                        System.out.println("Processing child " + ++counter + "/" + board.movesLeft);
                    }
                    int[] edgeCoords = it.getNextMove();
                    if (board.edgeToInt(edgeCoords[0], edgeCoords[1]) == killerMove) {
                        // Already considered, skip
                        continue;
                    }
                    board.registerMove(edgeCoords[0], edgeCoords[1]);
                    value = Math.max(value, AlphaBeta.alphaBeta(board, killerMoves, alpha, beta, false));
                    board.undo();
                    alpha = Math.max(alpha, value);
                    if (beta <= alpha) {
                        killerMoves[board.movesLeft - 1] = board.edgeToInt(edgeCoords[0], edgeCoords[1]);
                        break;
                    }
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
                    value = Math.min(value, AlphaBeta.alphaBeta(board, killerMoves, alpha, beta, false));
                    board.undo();
                    alpha = Math.min(alpha, value);
                    if (beta <= alpha)
                        break;
                }
            } else {

                int counter = 0;

                // Play killer move first
                int killerMove = killerMoves[board.movesLeft - 1];
                if (killerMove != 0) {
                    int[] edgeCoords = board.intToEdge(killerMove);
                    // Check if killer move is legal
                    if (!board.edges[edgeCoords[0]][edgeCoords[1]]) {
                        if (print) {
                            System.out.println("Processing child " + ++counter + "/" + board.movesLeft);
                        }
                        board.registerMove(edgeCoords[0], edgeCoords[1]);
                        value = Math.min(value, AlphaBeta.alphaBeta(board, killerMoves, alpha, beta, false));
                        board.undo();
                        beta = Math.min(alpha, value);
                        if (beta <= alpha) {
                            return value;
                        }
                    }
                }

                for(MoveIterator it = board.getLegalMoveIterator(); it.hasNext(); ) {
                    // Check all legal moves except for killer move
                    if (print) {
                        System.out.println("Processing child " + ++counter + "/" + board.movesLeft);
                    }
                    int[] edgeCoords = it.getNextMove();
                    if (board.edgeToInt(edgeCoords[0], edgeCoords[1]) == killerMove) {
                        // Already considered, skip
                        continue;
                    }
                    board.registerMove(edgeCoords[0], edgeCoords[1]);
                    value = Math.min(value, AlphaBeta.alphaBeta(board, killerMoves, alpha, beta, false));
                    board.undo();
                    beta = Math.min(alpha, value);
                    if (beta <= alpha) {
                        killerMoves[board.movesLeft - 1] = board.edgeToInt(edgeCoords[0], edgeCoords[1]);
                        break;
                    }
                }
            }
            return value;

        }

    }

}
