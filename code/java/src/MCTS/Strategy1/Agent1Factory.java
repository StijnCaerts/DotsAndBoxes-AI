package MCTS.Strategy1;

import main.Agent;
import main.AgentFactory;

public class Agent1Factory implements AgentFactory {
    @Override
    public Agent create(int player, double timeLimit, int rows, int columns, String gameId) {
        return new Agent1(player, timeLimit, rows, columns, gameId);
    }
}
