package ann;

import board.Board;
import main.AlphaBeta;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

public class Trainer {

    public static final String solvedGamesPath = "data/solvedGames";

    public static void main(String args[]) {
        Trainer.generateAndSave(1, 5, 10, 5, 10, 0.5, 68477);
    }

    public static void generateAndSave(int gamesAmount, int minColumns, int maxColumns, int minRows, int maxRows, double simulationRatio, int seed) {

        // Generates games, solves them and stores their results in a file
        // simulationRatio determines roughly what ratio of edges should be filled in in an example (before filling in chains)

        Random rand = new Random(seed);

        for(int game = 0; game < gamesAmount; game++) {

            // Determine game parameters
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;
            int movesAmount = (int) Math.round(simulationRatio*(2*columns*rows + columns + rows));
            int gameSeed = rand.nextInt();

            // Simulate game
            System.out.println("Simulating game " + game + " with " + columns + " columns, " + rows + " rows, " + movesAmount + " moves and seed " + seed);
            Board board = Trainer.simulateRandomGame(columns, rows, movesAmount, gameSeed);

            //TODO: First let players fill up all closed chains etc., wait until hasOptimalMoves is false

            // Solve game
            System.out.println("Solving");
            System.out.println(board.edgesString());
            System.out.println("Current player: " + board.currentPlayer);
            int res = AlphaBeta.search(board);
            System.out.println(board.edgesString());
            System.out.println("Current player: " + board.currentPlayer);
            System.out.println("Result: " + res);

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

                // We create a separate file and rename it because of atomicity
                Files.write(Paths.get(Trainer.solvedGamesPath + ".tmp"), buffer.array());
                (new File(Trainer.solvedGamesPath + ".tmp")).renameTo(new File(Trainer.solvedGamesPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public static Board simulateRandomGame(int columns, int rows, int movesAmount, int seed) {

        // Simulates a game on an empty board with the given dimensions for the given amount of moves starting with the given seed

        Random rand = new Random(seed);
        Board board = new Board(columns, rows, false);

        for(int move = 0; move < movesAmount; move++) {
            // Play random move
            int[] selectedEdgeCoords = board.getRandomLegalMove(rand);
            board.registerMove(selectedEdgeCoords[0], selectedEdgeCoords[1]);
            break;
        }

        return board;

    }

}
