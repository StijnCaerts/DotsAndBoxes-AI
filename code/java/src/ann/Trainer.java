package ann;

import board.Board;

import java.util.Random;

public class Trainer {

    public void generateAndSave(int amount, int minColumns, int maxColumns, int minRows, int maxRows, double simulationRatio, int seed) {

        // Generates games, solves them and stores their results in a file

        Random rand = new Random(seed);

        for(int game = 0; game < amount; game++) {

            // Determine game parameters
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;

            System.out.println("Simulating game " + game);

            System.out.println("Solving");

        }

    }

    public Board simulateRandomGame(int columns, int rows, int amount, int seed) {

        // Simulates a game on an empty board with the given dimensions for the given amount of moves starting with the given seed

        Random rand = new Random(seed);
        Board board = new Board(columns, rows, false);

        for(int move = 0; move < amount; move++) {
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
