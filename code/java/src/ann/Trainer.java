package ann;

import board.Board;
import main.AlphaBeta;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class Trainer {

    public static final String solvedGamesPath = "data/solvedGames";

    public void generateAndSave(int gamesAmount, int minColumns, int maxColumns, int minRows, int maxRows, double simulationRatio, int seed) {

        // Generates games, solves them and stores their results in a file
        // simulationRatio determines roughly what ratio of edges should be filled in in an example

        Random rand = new Random(seed);

        for(int game = 0; game < gamesAmount; game++) {

            // Determine game parameters
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;
            int movesAmount = (int) Math.round(simulationRatio*(2*columns*rows + columns + rows));
            int gameSeed = rand.nextInt();

            // Simulate game
            System.out.println("Simulating game " + game + " with " + columns + " columns, " + rows + " rows, " + movesAmount + " moves and seed " + seed);
            Board board = simulateRandomGame(columns, rows, movesAmount, gameSeed);

            // Solve game
            System.out.println("Solving");
            int res = AlphaBeta.search(board);

            // Store game
            try {
                byte[] original = Files.readAllBytes(Paths.get(Trainer.solvedGamesPath));
                ByteBuffer buffer = ByteBuffer.allocate(original.length + 5*4);
                buffer.put(original);
                buffer.putInt(columns);
                buffer.putInt(rows);
                buffer.putInt(movesAmount);
                buffer.putInt(seed);
                buffer.putInt(res);
                Files.write(Paths.get(Trainer.solvedGamesPath + ".tmp"), buffer.array());
            } catch (IOException e) {
                e.printStackTrace();
            }

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
