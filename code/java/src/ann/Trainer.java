package ann;

import board.Board;
import main.AlphaBeta;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

public class Trainer {

    public static final String solvedGamesPath = "data/solvedGames";

    public static void main(String args[]) {
        Trainer.generateAndSave(1000, 5, 5, 5, 5, 0.65);
    }

    public static void generateAndSave(int gamesAmount, int minColumns, int maxColumns, int minRows, int maxRows, double simulationRatio) {

        // Generates games, solves them and stores their results in a file
        // simulationRatio determines roughly what ratio of edges should be filled in in an example (before filling in chains)

        // Load seed
        byte[] original;
        int seed = (new Random()).nextInt();
        try {
            original = Files.readAllBytes(Paths.get(Trainer.solvedGamesPath));
            ByteBuffer buffer = ByteBuffer.allocate(original.length);
            buffer.put(original);
            seed = buffer.getInt(original.length - 4);
            System.out.println("Read seed " + seed);
        } catch (NoSuchFileException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        seed = -953711478;

        Random rand = new Random(seed);
        double totalSolvingTime = 0;

        for(int game = 0; game < gamesAmount; game++) {

            // Determine game parameters
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;
            int gameSeed = rand.nextInt();

            // Switch to next seed
            seed = rand.nextInt();

            // Simulate game
            System.out.println("Simulating game " + game + " with " + columns + " columns, " + rows + " rows, simulationRatio " + simulationRatio + " and seed " + seed);
            Board board = Trainer.simulateRandomGame(columns, rows, simulationRatio, gameSeed);

            // Solve game
            System.out.println("Solving");
            System.out.println(board.edgesString());
            System.out.println("Current player: " + board.currentPlayer);
            System.out.println("Current scores: " + board.scores[0] + ", " + board.scores[1]);
            System.out.println("Current heuristic input: " + Arrays.toString(board.getHeuristicInput()));
            long start = System.nanoTime();
            int res = AlphaBeta.search(board);
            totalSolvingTime += (System.nanoTime() - start)/1000000000.0;
            System.out.println("Result: " + res);

            // Store game
            try {
                try {
                    original = Files.readAllBytes(Paths.get(Trainer.solvedGamesPath));
                } catch (NoSuchFileException e) {
                    original = new byte[4];
                }
                ByteBuffer buffer = ByteBuffer.allocate(original.length + 2*4 + 8 + 2*4);
                buffer.put(original, 0, original.length - 4);
                buffer.putInt(columns);
                buffer.putInt(rows);
                buffer.putDouble(simulationRatio);
                buffer.putInt(gameSeed); // Seed that was used to generate this game
                buffer.putInt(res);
                buffer.putInt(seed); // Seed that will be used to generate next game
                System.out.println("Wrote seed " + seed);

                // We create a separate file and rename it because of atomicity
                Files.write(Paths.get(Trainer.solvedGamesPath + ".tmp"), buffer.array());
                (new File(Trainer.solvedGamesPath + ".tmp")).renameTo(new File(Trainer.solvedGamesPath));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        System.out.println("Finished solving " + gamesAmount + " games with " + minColumns + "-" + maxColumns + " columns, " + minRows + "-" + maxRows + " rows and simulationRatio "
                + simulationRatio);
        System.out.println("Average solve time: " + totalSolvingTime/gamesAmount);

    }

    public static Board simulateRandomGame(int columns, int rows, double simulationRatio, int seed) {

        // Simulates a game on an empty board with the given dimensions starting with the given seed
        // Simulates until at least simulationRatio*totalMoves moves have been played and there are no optimal moves in the current setting

        Random rand = new Random(seed);
        Board board = new Board(columns, rows, false); // We don't record undo during simulation, only solving

        int move = 0;
        int totalMoves = board.movesLeft;
        while(board.movesLeft > 0 && (move < simulationRatio*totalMoves || board.hasOptimalMoves())) {
            int[] selectedEdgeCoords;
            if (board.hasOptimalMoves()) {
                selectedEdgeCoords = board.intToEdge(board.getOptimalMoves()[rand.nextInt(board.getOptimalMoves().length)]);
            } else {
                selectedEdgeCoords = board.getRandomLegalMove(rand);
            }
            board.registerMove(selectedEdgeCoords[0], selectedEdgeCoords[1]);
            move++;
        }

        return board;

    }

}
