package MCTS2;

import main.Agent;
import main.AgentFactory;

public class AsyncSearchAgentFactory implements AgentFactory {
    @Override
    public Agent create(int player, double timeLimit, int rows, int columns, String gameId) {
        return new AsyncSearchAgent(player, timeLimit, rows, columns, gameId);
    }
}
