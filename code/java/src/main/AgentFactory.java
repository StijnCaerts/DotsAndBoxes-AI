package main;

public interface AgentFactory {

    Agent create(int player, double timeLimit, int rows, int columns, String gameId);

}
