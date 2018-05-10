package ann;

import board.Board;
import main.AlphaBeta;
import math.CustomMath;
import math.Vector;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Trainer {

    public static final String solvedGamesPath = "data/solvedGames";
    public static final String annPath = "data/ann/random_ann";
    public static final String annBasePath = "data/ann/ann0-";
    public static final String annPerformancePath = "data/ann_performance.py";

    public static void main(String args[]) {

        //Trainer.generateAndSave(4000, 5, 6, 5, 6, 0.7);
        //Trainer.analyzeExamples(Trainer.loadGames(Trainer.solvedGamesPath));
        //Trainer.saveNewANN(Trainer.ANNPath);
        Trainer.loadAndTrainANN(Trainer.annPath, Trainer.annBasePath, Trainer.annPerformancePath, Trainer.loadGames(Trainer.solvedGamesPath), 0.8);

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
            buffer.rewind();
            seed = buffer.getInt(original.length - 4);
            System.out.println("Read seed " + seed);
        } catch (NoSuchFileException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }

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
            System.out.println("Simulating game " + game + " with " + columns + " columns, " + rows + " rows, simulationRatio " + simulationRatio + " and seed " + gameSeed);
            Board board = Trainer.simulateRandomGame(columns, rows, simulationRatio, gameSeed);

            // Solve game
            /*System.out.println("Solving");
            System.out.println(board.edgesString());
            System.out.println("Current player: " + board.currentPlayer);
            System.out.println("Current scores: " + board.scores[0] + ", " + board.scores[1]);
            System.out.println("Current heuristic input: " + Arrays.toString(board.getHeuristicInput()));*/
            long start = System.nanoTime();
            int res = AlphaBeta.search(board);
            totalSolvingTime += (System.nanoTime() - start)/1000000000.0;
            //System.out.println("Result: " + res);

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
                //System.out.println("Wrote seed " + seed);

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

    public static Example[] loadGames(String path) {

        // Load games from file

        try {

            // Read file
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            buffer.put(bytes);
            buffer.rewind();

            // Load games
            ArrayList<Example> examples = new ArrayList<>();
            Random rand = new Random(897687438);
            for(int i = 0; i < bytes.length - 4; i += 2*4 + 8 + 2*4) {

                // Load game data
                int columns = buffer.getInt();
                int rows = buffer.getInt();
                double simulationRatio = buffer.getDouble();
                int gameSeed = buffer.getInt();
                int res = buffer.getInt();

                // Simulate game
                Board board = Trainer.simulateRandomGame(columns, rows, simulationRatio, gameSeed);

                // Verify if data was loaded correctly
                //System.out.println(AlphaBeta.search(board) == res);

                // Get heuristic input
                Vector input = board.getHeuristicInput();

                // Switch players in half of cases to remove bias
                if (rand.nextInt(2) == 1) {
                    double tmp = input.values[0];
                    input.values[0] = input.values[1];
                    input.values[1] = tmp;
                    res *= -1;
                }

                // Store example
                examples.add(new Example(input, res));

            }

            return examples.toArray(new Example[0]);

        } catch (NoSuchFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static void analyzeExamples(Example[] examples) {

        // Calculates and prints sum of input vectors
        Vector avg = new Vector(new double[examples[0].input.height]);
        double outputAvg = 0;
        int wins = 0;
        int ties = 0;
        int losses = 0;
        for(Example example : examples) {
            avg.add(example.input);
            outputAvg += example.output;
            if (example.output > CustomMath.epsilon) {
                wins++;
            } else if (example.output < CustomMath.epsilon) {
                losses++;
            } else {
                ties++;
            }
        }
        avg.multiply(1.0/examples.length);
        outputAvg /= examples.length;
        System.out.println(Arrays.toString(avg.values));
        System.out.println(outputAvg);
        System.out.println(wins + ", " + ties + ", " + losses);

    }

    public static void saveNewANN(String path) {

        // Creates new random ANN with correct topology and saves it
        // Guideline for hidden size comes from https://stats.stackexchange.com/questions/181/how-to-choose-the-number-of-hidden-layers-and-nodes-in-a-feedforward-neural-netw
        int inputSize = 2 + Board.maxOpenChainSize + Board.maxLoopSize - 3;
        int outputSize = 1;
        ANN ann = new ANN(inputSize, (inputSize + outputSize)/2);
        ann.save(path);

    }

    public static void loadAndTrainANN(String annPath, String annBasePath, String annPerformancePath, Example[] examples, double trainingRatio) {

        // Training size indicates size of training set, rest of examples are used for validation
        ANN ann = ANN.load(annPath);
        int trainingSize = (int) Math.round(trainingRatio*examples.length);
        Example[] trainingSet = Arrays.copyOfRange(examples, 0, trainingSize);
        Example[] validationSet = Arrays.copyOfRange(examples, trainingSize, examples.length);
        ann.train(trainingSet, validationSet, annBasePath, annPerformancePath);

    }

}
