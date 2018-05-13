package main;

import MCTS.Strategy1.Agent1Factory;
import MCTS.Strategy2.Agent2Factory;
import MCTS.Strategy3.Agent3Factory;
import board.Board;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

public class Simulator {

    public static void main(String[] args) throws IOException {
        int[] res;
        String filename = "simulations.txt";
        int gamesAmount = 100;
        double timelimit = 0.5;

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
        ;
        String simulationStage;

        for (int size = 6; size <= 7; size++) {
            simulationStage = "Simulating Strategy1 vs Strategy2, size: " + Integer.toString(size);
            writer.println(simulationStage);
            writer.close();
            System.out.println(simulationStage);
            res = Simulator.simulate(new Agent1Factory(), new Agent2Factory(), gamesAmount, timelimit, size, size, size, size, false);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            writer.println(Arrays.toString(res));

            simulationStage = "Simulating Strategy2 vs Strategy3, size: " + Integer.toString(size);
            writer.println(simulationStage);
            writer.close();
            System.out.println(simulationStage);
            res = Simulator.simulate(new Agent2Factory(), new Agent3Factory(), gamesAmount, timelimit, size, size, size, size, false);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            writer.println(Arrays.toString(res));

            simulationStage = "Simulating Strategy3 vs MCTS2, size: " + Integer.toString(size);
            writer.println(simulationStage);
            writer.close();
            System.out.println(simulationStage);
            res = Simulator.simulate(new Agent3Factory(), new MCTS2.MCTSAgentFactory(), gamesAmount, timelimit, size, size, size, size, false);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            writer.println(Arrays.toString(res));

            simulationStage = "Simulating MCTS2 vs MCTS3, size: " + Integer.toString(size);
            writer.println(simulationStage);
            writer.close();
            System.out.println(simulationStage);
            res = Simulator.simulate(new MCTS2.MCTSAgentFactory(), new MCTS3.MCTSAgentFactory(), gamesAmount, timelimit, size, size, size, size, false);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            writer.println(Arrays.toString(res));

            simulationStage = "Simulating MCTS2 vs MCTS2Async, size: " + Integer.toString(size);
            writer.println(simulationStage);
            writer.close();
            System.out.println(simulationStage);
            res = Simulator.simulate(new MCTS2.MCTSAgentFactory(), new MCTS2.AsyncSearchAgentFactory(), gamesAmount, timelimit, size, size, size, size, false);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            writer.println(Arrays.toString(res));
        }
        writer.close();
    }

    public static int[] simulate(AgentFactory factory1, AgentFactory factory2, int gamesAmount, double timeLimit, int minColumns, int maxColumns, int minRows, int maxRows, boolean print) {

        // Simulates a series of random games in between two classes of agents
        // Returns int-array of length 6:
        // [agent1 wins with agent1 playing first, tie with agent1 playing first, agent1 loses with agent1 playing first,
        // agent1 wins with agent2 playing first, tie with agent2 playing first, agent1 loses with agent2 playing first]

        Random rand = new Random();
        int[] res = new int[6];
        int mcts2Iterations = 0;
        int mcts2Moves = 0;
        int mcts3Iterations = 0;
        int mcts3Moves = 0;
        for (int game = 0; game < gamesAmount; game++) {

            // If game is even, agent1 starts the game

            // Initialization
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;
            Board board = new Board(columns, rows, false);
            Agent[] agents = new Agent[]{
                    factory1.create(game % 2, timeLimit, rows, columns, Integer.toString(game)),
                    factory2.create((game + 1) % 2, timeLimit, rows, columns, Integer.toString(game))
            };

            // Simulation
            if (true) {
                System.out.println("Simulating game " + game + " with " + columns + " columns and " + rows + " rows");
            }
            while (board.movesLeft > 0) {
                int[] move = agents[(game + board.getCurrentPlayer()) % 2].getNextMove();
                board.registerMove(move);
                for (int i = 0; i < 2; i++) {
                    agents[i].registerAction(board.scores[(game + i) % 2], board.scores[(game + i + 1) % 2], move[0], move[1]);
                }
            }

            // End
            res[3 * (game % 2) + (int) Math.signum(board.scores[1] - board.scores[0]) + 1]++;
            if (print) {

                MCTS2.MCTSAgent mcts2 = (MCTS2.MCTSAgent) agents[0];
                mcts2Iterations += mcts2.iterations;
                mcts2Moves += mcts2.moves;
                System.out.println("MCTS2:");
                System.out.println("Iterations: " + mcts2.iterations);
                System.out.println("Moves: " + mcts2.moves);
                System.out.println("Average iterations/move: " + (double) mcts2.iterations / mcts2.moves);
                System.out.println("Average time/iteration: " + timeLimit * mcts2.moves / mcts2.iterations);
                MCTS3.MCTSAgent mcts3 = (MCTS3.MCTSAgent) agents[1];
                mcts3Iterations += mcts3.iterations;
                mcts3Moves += mcts3.moves;
                System.out.println("MCTS3:");
                System.out.println("Iterations: " + mcts3.iterations);
                System.out.println("Moves: " + mcts3.moves);
                System.out.println("Average iterations/move: " + (double) mcts3.iterations / mcts3.moves);
                System.out.println("Average time/iteration: " + timeLimit * mcts3.moves / mcts3.iterations);


                System.out.println(Arrays.toString(board.scores));
                System.out.println(Arrays.toString(res));
            }

        }

        if (print) {
            System.out.println("Simulated " + gamesAmount + " games in between " + factory1.getClass() + " and " + factory2.getClass());
            System.out.println("timeLimit: " + timeLimit + ", columns: " + minColumns + "-" + maxColumns + ", rows: " + minRows + "-" + maxRows);
            System.out.println(Arrays.toString(res));
            System.out.println("MCTS2:");
            System.out.println("Iterations: " + mcts2Iterations);
            System.out.println("Moves: " + mcts2Moves);
            System.out.println("Average iterations/move: " + (double) mcts2Iterations / mcts2Moves);
            System.out.println("Average time/iteration: " + timeLimit * mcts2Moves / mcts2Iterations);
            System.out.println("MCTS3:");
            System.out.println("Iterations: " + mcts3Iterations);
            System.out.println("Moves: " + mcts3Moves);
            System.out.println("Average iterations/move: " + (double) mcts3Iterations / mcts3Moves);
            System.out.println("Average time/iteration: " + timeLimit * mcts3Moves / mcts3Iterations);
        }

        return res;

    }

}
