package MCTS.Strategy2;

import MCTS.Board;
import MCTS.Move;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Optional;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

// Based on https://github.com/DieterBuys/mcts-player
public class Node {
    Node parent;
    public Move move;
    public Board board;
    int plays = 0;
    double score = 0.0;

    ArrayList<Move> pendingMoves;
    ArrayList<Node> children;


    public Node(Board board, Node parent, Move move) {
        this.board = board;
        this.parent = parent;
        this.move = move;

        this.pendingMoves = new ArrayList<>(board.getMoves());
        this.children = new ArrayList<>();
    }

    public Node(Board board) {
        this(board, null, null);
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

    Node expandMove(Move move) {
        this.pendingMoves.remove(move);
        Board childBoard = this.board.duplicate();
        childBoard.playMove(move);

        Node child = new Node(childBoard, this, move);
        this.children.add(child);
        return child;
    }

    double getScore(double result, int player) {
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
