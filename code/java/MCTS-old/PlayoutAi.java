package MCTS2;

import java.util.List;

public interface PlayoutAi {

    /**
     * Give the chance to adjust the score calcuated by a
     * 
     * @param moves
     * @return array of weights (same length as moves)
     */
    public double[] getScore(Board initialBoard, Board finalBoard, List<Move> moves);

    /**
     * Returns an array of probability weights for each move possible on this
     * board. The higher the returned value, the better the move is likely to
     * be.
     * 
     * @param moves
     * @return array of weights (same length as moves)
     */
    public double[] getWeights(Board board, List<Move> moves);

    /**
     * Add a complete game to a collection that can be trained
     * 
     * @param b complete board game
     */
    public void addCompletedGame(Board b);

    /**
     * Improve AI by learning from completed games
     */
    public void train();

    /**
     * Provides the ability to stop a playout early. If iterations are too
     * small, it would be better to stop early and rely more on this AI to
     * estimate score
     * 
     * @param moves moves played so far
     * @param board current board
     * 
     * @return true to stop
     */
    public boolean stopPlayoutEarly(Board board, List<Move> moves);
}
