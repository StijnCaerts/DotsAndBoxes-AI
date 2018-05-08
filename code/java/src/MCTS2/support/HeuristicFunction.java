package MCTS2.support;

import MCTS2.Board;

/**
 * Create a class implementing this interface and instantiate
 * it. Pass the instance to the MCTS instance using the
 * {@link #setHeuristicFunction(HeuristicFunction h) setHeuristicFunction} method.

 * @author KGS
 *
 */
public interface HeuristicFunction {
	public float h(Board board);
}
