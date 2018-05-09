package ann;

import board.Board;
import main.AlphaBeta;
import java.util.Random;

public class Trainer {

    public void generateAndSave(int gamesAmount, int minColumns, int maxColumns, int minRows, int maxRows, double simulationRatio, int seed) {

        // Generates games, solves them and stores their results in a file
        // simulationRatio determines roughly what ratio of edges should be filled in in an example

        Random rand = new Random(seed);

        for(int game = 0; game < gamesAmount; game++) {

            // Determine game parameters
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;
            int moves = (int) Math.round(simulationRatio*(2*columns*rows + columns + rows));
            int gameSeed = rand.nextInt();

            // Simulate game
            System.out.println("Simulating game " + game + " with " + columns + " columns, " + rows + " rows, " + moves + " moves and seed " + seed);
            simulateRandomGame(columns, rows, moves, gameSeed);

            System.out.println("Solving");
            AlphaBeta.AlphaBeta();

        }

    }

    public Board simulateRandomGame(int columns, int rows, int movesAmount, int seed) {

        // Simulates a game on an empty board with the given dimensions for the given amount of moves starting with the given seed

        Random rand = new Random(seed);
        Board board = new Board(columns, rows, false);

        for(int move = 0; move < movesAmount; move++) {
            // Play random move
            int i = 0;
            int selectedEdgeIndex = rand.nextInt(board.legalMoves.size());
            for(int edge : board.legalMoves) {
                if (i++ == selectedEdgeIndex) {
                    int[] selectedEdgeCoords = board.intToEdge(edge);
                    board.registerMove(selectedEdgeCoords[0], selectedEdgeCoords[1]);
                    break;
                }
            }
        }

        return board;

    }

}
