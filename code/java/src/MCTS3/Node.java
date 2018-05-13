package MCTS3;

import board.Board;
import board.RandomMoveGenerator;

import java.util.*;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

public class Node {

    Node parent;
    public int move;
    public Board board;
    int plays = 0;
    double score = 0.0;

    ArrayList<Node> children;
    RandomMoveGenerator generator; // Should only be initialized in nodes without optimal moves
    int alreadyGeneratedOptimalMove = -1; // Indicates index of optimal move that was already generated

    public Node(Board board, Node parent, int move) {
        this.board = board;
        this.parent = parent;
        this.move = move;
        this.children = new ArrayList<>();
        this.generator = null;
    }

    public Node(Board board) {
        this(board, null, 0);
    }


    Node selectChildUCB() {
        Optional<Node> optNode = this.children.stream().max(Comparator.comparing(c -> c.score / c.plays + sqrt(2 * log(this.plays) / c.plays)));
        try {
            Node maxNode = optNode.get();
            return maxNode;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    boolean canExpand() {
        if (this.generator == null) {
            if (this.board.hasOptimalMoves()) {
                // Only consider optimal moves
                return this.children.size() < this.board.getOptimalMoves().length;
            } else {
                // Consider all legal moves
                return this.board.movesLeft > 0;
            }
        } else {
            return this.generator.hasMovesLeft();
        }
    }

    Node expand(Random rand) {

        // Creates a new random child and returns it

        if (canExpand()) {

            // Generate move
            int move = 0;
            if (this.board.hasOptimalMoves()) {
                // Only consider optimal moves
                if (this.board.optimalMoves.length == 1) {
                    move = this.board.getOptimalMoves()[0];
                } else {
                    if (this.alreadyGeneratedOptimalMove == -1) {
                        this.alreadyGeneratedOptimalMove = rand.nextInt(2);
                        move = this.board.getOptimalMoves()[this.alreadyGeneratedOptimalMove];
                    } else {
                        move = this.board.getOptimalMoves()[(this.alreadyGeneratedOptimalMove + 1) % 2];
                    }
                }
            } else {
                // Consider all legal moves
                if (this.generator == null)
                    this.generator = new RandomMoveGenerator(this.board);
                for (int i = 0; i < 100; i++) {
                    move = this.generator.getRandomLegalMoveAsInt(rand);
                    if (this.board.isBad(move)) {
                        this.generator.reverseMove(move);
                    } else {
                        break;
                    }
                }
            }

            // Create child
            Board childBoard = this.board.deepcopy();
            childBoard.registerMove(move);
            Node child = new Node(childBoard, this, move);
            this.children.add(child);
            return child;

        } else {
            return null;
        }

    }

    static double getScore(double result, int player) {
        if (result == 0.5) {
            return result;
        }
        if (player != (int) result) {
            return 0.0;
        } else {
            return 1.0;
        }
    }
}
