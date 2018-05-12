package MCTS.Strategy3;

import MCTS.Strategy3.Agent3;
import main.Agent;
import main.AgentFactory;

public class Agent3Factory implements AgentFactory {

    @Override
    public Agent create(int player, double timeLimit, int rows, int columns, String gameId) {
        return new Agent3(player, timeLimit, rows, columns, gameId);
    }

}
