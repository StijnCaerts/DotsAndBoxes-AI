package MCTS2;

public enum FinalSelectionPolicy {
	maxChild, // The child with highest SCORE
	robustChild // The child with highest visit count
}
