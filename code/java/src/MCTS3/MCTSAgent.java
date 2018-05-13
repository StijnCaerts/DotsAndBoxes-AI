package MCTS3;

import ann.ANN;
import board.Board;
import board.BoardState;
import main.Agent;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

public class MCTSAgent extends Agent {

    public static final String annPath = "final_ann";
    Node rootNode;
    Random rand; // All random decisions are based on this object, so can be seeded for determinism
    public int iterations = 0;
    public int moves = 0;
    ANN ann;

    public MCTSAgent(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.rootNode = new Node(new Board(columns, rows, false));
        this.rand = new Random();
        this.ann = ANN.load(MCTSAgent.annPath);
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {

        // find child that corresponds with the given move, if it exists
        // if no such child exist, create a new node with the corresponding board
        // set this node as the new root node
        int move = this.rootNode.board.edgeToInt(x, y);
        Optional<Node> optionalNode = this.rootNode.children.stream().filter(c -> c.move == move).findFirst();
        Node newRoot;
        if (optionalNode.isPresent()) {
            // Found move already
            // remove references to free up resources
            newRoot = optionalNode.get();
            newRoot.parent = null;
            newRoot.move = 0;
        } else {
            // Move wasn't in tree yet, start over
            Board newBoard = this.rootNode.board.deepcopy();
            newBoard.registerMove(move);
            newRoot = new Node(newBoard);
        }
        this.rootNode = newRoot;

    }

    @Override
    public int[] getNextMove() {

        long startTime = System.nanoTime();
        while (System.nanoTime() < startTime + this.timeLimit * 1000000000) {

            // Selection
            Node node = select();

            // Expansion
            if (node.canExpand()) {
                node = node.expand(this.rand);
            }

            // Simulation
            double result = simulate(node.board);

            // Back-propagation
            update(node, result);

            this.iterations++;
        }
        this.moves++;

        // Return most visited node's move
        Optional<Node> opt = this.rootNode.children.stream().max(Comparator.comparingInt(c -> c.plays));
        try {
            Node bestChild = opt.get();
            return rootNode.board.intToEdge(bestChild.move);
        } catch (NoSuchElementException e) {
            return null;
        }

    }

    Node select() {
        Node node = this.rootNode;

        while (!node.canExpand() && !node.children.isEmpty()) {
            node = node.selectChildUCB();
        }

        return node;
    }

    double simulate(Board board) {

        Board boardCopy = board.deepcopy();
        int move = boardCopy.getNextAcceptableMove(this.rand);
        while (move != 0 && !boardCopy.gameDecided() && boardCopy.getState() != BoardState.MIDDLE) {
            boardCopy.registerMove(move);
            move = boardCopy.getNextAcceptableMove(this.rand);
        }

        // Return score
        // 0 means the first player wins, 1 means the second player wins
        if (boardCopy.getState() == BoardState.MIDDLE) {
            // Estimate score using ANN heuristic
            double output = this.ann.predict(boardCopy.getHeuristicInput()); // Close to 1 means current player should win, close to -1 means other player should win
            if (boardCopy.getCurrentPlayer() == 0) {
                return (-output + 1) / 2;
            } else {
                return (output + 1) / 2;
            }
        } else {
            // We played till the end, just use the board outcome
            return boardCopy.gameResult();
        }

    }

    void update(Node node, double result) {
        while (node != null) {
            node.plays++;
            node.score += Node.getScore(result, this.player);
            node = node.parent;
        }
    }

}
