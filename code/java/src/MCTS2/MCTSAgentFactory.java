package MCTS2;

import main.Agent;
import main.AgentFactory;

public class MCTSAgentFactory implements AgentFactory {

    @Override
    public Agent create(int player, double timeLimit, int rows, int columns, String gameId) {
        return new MCTSAgent(player, timeLimit, rows, columns, gameId);
    }

}
