package MCTS.Strategy2;

import main.Agent;
import main.AgentFactory;

public class Agent2Factory implements AgentFactory {
    @Override
    public Agent create(int player, double timeLimit, int rows, int columns, String gameId) {
        return new Agent2(player, timeLimit, rows, columns, gameId);
    }
}
