package ann;

import MCTS2.MCTSAgent;
import board.Board;
import board.BoardState;
import main.Agent;
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
    public static final String annPath = "data/random_ann";
    public static final String annBasePath = "data/ann/ann0-";
    public static final String annPerformancePath = "data/ann_performance.py";

    public static void main(String args[]) {

        //Trainer.generateAndSave(250, 5, 6, 6, 6);
        //Trainer.analyzeExamples(Trainer.loadGames(Trainer.solvedGamesPath));
        //Trainer.saveNewANN(Trainer.annPath);
        Trainer.loadAndTrainANN(Trainer.annPath, Trainer.annBasePath, Trainer.annPerformancePath, Trainer.loadGames(Trainer.solvedGamesPath), 0.8);

    }

    public static void generateAndSave(int gamesAmount, int minColumns, int maxColumns, int minRows, int maxRows) {
        Trainer.generateAndSave(gamesAmount, minColumns, maxColumns, minRows, maxRows, (new Random()).nextInt());
    }

    public static void generateAndSave(int gamesAmount, int minColumns, int maxColumns, int minRows, int maxRows, int seed) {

        // Generates games, solves them and stores their results in a file
        // simulationRatio determines roughly what ratio of edges should be filled in in an example (before filling in chains)

        Random rand = new Random(seed);
        double totalSolvingTime = 0;

        for (int game = 0; game < gamesAmount; game++) {

            while (true) {

                // Determine game parameters
                int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
                int rows = rand.nextInt(maxRows - minRows + 1) + minRows;

                // Simulate game
                System.out.println("Simulating game " + game + " with " + columns + " columns, " + rows + " rows");
                Board board = Trainer.simulateRandomGame(columns, rows);

                // Solve game
                long start = System.nanoTime();
                int res = 0;
                try {
                    res = AlphaBeta.search(board);
                } catch (AlphaBeta.TimeExceededException e) {
                    // Retry
                    continue;
                }
                totalSolvingTime += (System.nanoTime() - start) / 1000000000.0;

                // Store game
                try {
                    byte[] original;
                    try {
                        original = Files.readAllBytes(Paths.get(Trainer.solvedGamesPath));
                    } catch (NoSuchFileException e) {
                        original = new byte[0];
                    }
                    ByteBuffer buffer = ByteBuffer.allocate(original.length + 5 * 4 + (int) Math.ceil((double) (2 * columns * rows + columns + rows) / 8) + 4);
                    buffer.put(original, 0, original.length);
                    buffer.putInt(columns);
                    buffer.putInt(rows);
                    buffer.putInt(board.getCurrentPlayer());
                    buffer.putInt(board.scores[0]);
                    buffer.putInt(board.scores[1]);
                    // Write edges, bit by bit
                    int x = 0;
                    int y = 1;
                    for (int i = 0; i < (int) Math.ceil((double) (2 * columns * rows + columns + rows) / 8); i++) {
                        byte value = 0;
                        for (int bit = 0; bit < 8; bit++) {
                            // If edge is there, set bit to true
                            if (board.edges[x][y])
                                value = (byte) (value | (1 << bit));

                            // Go to next edge
                            y += 2;
                            if (y >= 2 * rows + 1) {
                                x++;
                                y = (x + 1) % 2;
                                if (x >= 2 * columns + 1)
                                    break;
                            }
                        }
                        buffer.put(value);
                    }
                    buffer.putInt(res);

                    // We create a separate file and rename it because of atomicity
                    Files.write(Paths.get(Trainer.solvedGamesPath + ".tmp"), buffer.array());
                    (new File(Trainer.solvedGamesPath + ".tmp")).renameTo(new File(Trainer.solvedGamesPath));
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }

                break;

            }

        }

        System.out.println("Finished solving " + gamesAmount + " games with " + minColumns + "-" + maxColumns + " columns, " + minRows + "-" + maxRows + " rows");
        System.out.println("Average solve time: " + totalSolvingTime / gamesAmount);

    }

    public static Board simulateRandomGame(int columns, int rows) {

        // Simulates a game on an empty board with the given dimensions starting with the given seed

        while (true) {

            // Keep simulating games until you don't fill out the entire board

            Board board = new Board(columns, rows, false); // We don't record undo during simulation, only solving
            double timeLimit = 0.005;

            // Initialization
            Agent[] agents = new Agent[]{
                    new MCTSAgent(0, timeLimit, rows, columns, ""),
                    new MCTSAgent(1, timeLimit, rows, columns, "")
            };

            // Simulation
            while (board.movesLeft > 0 && board.state == BoardState.START) {
                int[] move = agents[board.getCurrentPlayer()].getNextMove();
                board.registerMove(move);
                for (int i = 0; i < 2; i++) {
                    agents[i].registerAction(board.scores[i % 2], board.scores[(i + 1) % 2], move[0], move[1]);
                }
            }

            if (board.movesLeft > 0)
                return board;

        }

    }

    public static Example[] loadGames(String path) {

        // Load games from file

        try {

            // Read file
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            buffer.put(bytes);
            buffer.rewind();
            int bytesLeft = bytes.length;

            // Load games
            ArrayList<Example> examples = new ArrayList<>();
            Random rand = new Random();
            while (bytesLeft > 0) {

                // Load game data
                int columns = buffer.getInt();
                int rows = buffer.getInt();
                int currentPlayer = buffer.getInt();
                int score0 = buffer.getInt();
                int score1 = buffer.getInt();
                bytesLeft -= 5 * 4;

                // Load edges, bit by bit
                Board board = new Board(columns, rows, false);
                // Write edges, bit by bit
                int x = 0;
                int y = 1;
                for (int i = 0; i < (int) Math.ceil((double) (2 * columns * rows + columns + rows) / 8); i++) {
                    byte value = buffer.get();
                    bytesLeft -= 1;
                    for (int bit = 0; bit < 8; bit++) {
                        // If edge is there, set bit to true
                        if (((value >> bit) & 1) == 1)
                            board.registerMove(x, y);

                        // Go to next edge
                        y += 2;
                        if (y >= 2 * rows + 1) {
                            x++;
                            y = (x + 1) % 2;
                            if (x >= 2 * columns + 1)
                                break;
                        }
                    }
                }
                int res = buffer.getInt();
                bytesLeft -= 4;

                // Fix scores and current player
                board.currentPlayer = currentPlayer;
                board.scores[0] = score0;
                board.scores[1] = score1;

                // res now indicates if the current player loses (-1), ties (0) or wins (1)
                if (currentPlayer == 1) {
                    res *= -1;
                }

                // Verify if data was loaded correctly
                //System.out.println(board.edgesString());

                // Get heuristic input
                Vector input = board.getHeuristicInput();

                // Switch players in half of cases to remove bias
                if (rand.nextInt(2) == 1) {
                    double tmp = input.values[0];
                    input.values[0] = input.values[1];
                    input.values[1] = tmp;
                    input.values[input.height - 1] *= -1;
                    res *= -1;
                }

                // Store example
                examples.add(new Example(input, res));

            }

            System.out.println("Loaded " + examples.size() + " solved games.");

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
        int chainParityHeuristicCorrect = 0;
        for (Example example : examples) {
            avg.add(example.input);
            outputAvg += example.output;
            if (example.output * example.input.values[example.input.height - 1] > 0)
                chainParityHeuristicCorrect++;
            if (example.output > CustomMath.epsilon) {
                wins++;
            } else if (example.output < CustomMath.epsilon) {
                losses++;
            } else {
                ties++;
            }
        }
        avg.multiply(1.0 / examples.length);
        outputAvg /= examples.length;
        System.out.println("Average input: " + Arrays.toString(avg.values));
        System.out.println("Average output: " + outputAvg);
        System.out.println(wins + ", " + ties + ", " + losses);
        System.out.println("Chain parity heuristic correctness: " + (double) chainParityHeuristicCorrect / examples.length);

    }

    public static void saveNewANN(String path) {

        // Creates new random ANN with correct topology and saves it
        // Guideline for hidden size comes from https://stats.stackexchange.com/questions/181/how-to-choose-the-number-of-hidden-layers-and-nodes-in-a-feedforward-neural-netw
        int inputSize = (new Board(1, 1, false)).getHeuristicInput().height;
        int outputSize = 1;
        ANN ann = new ANN(inputSize, (inputSize + outputSize) / 2);
        ann.save(path);

    }

    public static void loadAndTrainANN(String annPath, String annBasePath, String annPerformancePath, Example[] examples, double trainingRatio) {

        // Training size indicates size of training set, rest of examples are used for validation
        ANN ann = ANN.load(annPath);
        int trainingSize = (int) Math.round(trainingRatio * examples.length);
        Example[] trainingSet = Arrays.copyOfRange(examples, 0, trainingSize);
        Example[] validationSet = Arrays.copyOfRange(examples, trainingSize, examples.length);
        ann.train(trainingSet, validationSet, annBasePath, annPerformancePath);

    }

}
