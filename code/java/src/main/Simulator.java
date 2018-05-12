package main;

import MCTS2.AsyncSearchAgentFactory;
import board.Board;

import java.util.Arrays;
import java.util.Random;

public class Simulator {

    public static void main(String[] args) {
        int[] res = Simulator.simulate(new AsyncSearchAgentFactory(), new MCTS2.MCTSAgentFactory(), 50, 0.5, 5, 6, 5, 5, true);
        System.out.println(Arrays.toString(res));
    }

    public static int[] simulate(AgentFactory factory1, AgentFactory factory2, int gamesAmount, double timeLimit, int minColumns, int maxColumns, int minRows, int maxRows, boolean print) {

        // Simulates a series of random games in between two classes of agents
        // Returns int-array of length 6:
        // [agent1 wins with agent1 playing first, tie with agent1 playing first, agent1 loses with agent1 playing first,
        // agent1 wins with agent2 playing first, tie with agent2 playing first, agent1 loses with agent2 playing first]

        Random rand = new Random();
        int[] res = new int[6];
        int mcts1Iterations = 0;
        int mcts1Moves = 0;
        int mcts2Iterations = 0;
        int mcts2Moves = 0;
        for(int game = 0; game < gamesAmount; game++) {

            // If game is even, agent1 starts the game

            // Initialization
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;
            Board board = new Board(columns, rows, false);
            Agent[] agents = new Agent[] {
                    factory1.create(game%2, timeLimit, rows, columns, Integer.toString(game)),
                    factory2.create((game + 1)%2, timeLimit, rows, columns, Integer.toString(game))
            };

            // Simulation
            if (print) {
                System.out.println("Simulating game " + game + " with " + columns + " columns and " + rows + " rows");
            }
            while (board.movesLeft > 0) {
                int[] move = agents[(game + board.getCurrentPlayer())%2].getNextMove();
                board.registerMove(move);
                for(int i = 0; i < 2; i++) {
                    agents[i].registerAction(board.scores[(game + i)%2], board.scores[(game + i + 1)%2], move[0], move[1]);
                }
            }

            // End
            res[3*(game%2) + (int) Math.signum(board.scores[1] - board.scores[0]) + 1]++;
            if (print) {
                MCTS2.AsyncSearchAgent mcts = (MCTS2.AsyncSearchAgent) agents[0];
                mcts1Iterations += mcts.iterations;
                mcts1Moves += mcts.moves;
                System.out.println("MCTS:");
                System.out.println("Iterations: " + mcts.iterations);
                System.out.println("Moves: " + mcts.moves);
                System.out.println("Average iterations/move: " + (double) mcts.iterations/mcts.moves);
                System.out.println("Average time/iteration: " + timeLimit*mcts.moves/mcts.iterations);
                MCTS2.MCTSAgent mcts2 = (MCTS2.MCTSAgent) agents[1];
                mcts2Iterations += mcts2.iterations;
                mcts2Moves += mcts2.moves;
                System.out.println("MCTS2:");
                System.out.println("Iterations: " + mcts2.iterations);
                System.out.println("Moves: " + mcts2.moves);
                System.out.println("Average iterations/move: " + (double) mcts2.iterations/mcts2.moves);
                System.out.println("Average time/iteration: " + timeLimit*mcts2.moves/mcts2.iterations);


                System.out.println(Arrays.toString(board.scores));
                System.out.println(Arrays.toString(res));
            }

        }

        if (print) {
            System.out.println("Simulated " + gamesAmount + " games in between " + factory1.getClass() + " and " + factory2.getClass());
            System.out.println("timeLimit: " + timeLimit + ", columns: " + minColumns + "-" + maxColumns + ", rows: " + minRows + "-" + maxRows);
            System.out.println(Arrays.toString(res));
            System.out.println("MCTS:");
            System.out.println("Iterations: " + mcts1Iterations);
            System.out.println("Moves: " + mcts1Moves);
            System.out.println("Average iterations/move: " + (double) mcts1Iterations/mcts1Moves);
            System.out.println("Average time/iteration: " + timeLimit*mcts1Moves/mcts1Iterations);
            System.out.println("MCTS2:");
            System.out.println("Iterations: " + mcts2Iterations);
            System.out.println("Moves: " + mcts2Moves);
            System.out.println("Average iterations/move: " + (double) mcts2Iterations/mcts2Moves);
            System.out.println("Average time/iteration: " + timeLimit*mcts2Moves/mcts2Iterations);
        }

        return res;

    }

}
