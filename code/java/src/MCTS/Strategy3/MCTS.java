package MCTS.Strategy3;

import MCTS.Board;
import MCTS.Move;

import java.util.*;

// Based on https://github.com/DieterBuys/mcts-player
public class MCTS {

    private Node rootNode;

    public void init(Board board) {
        assert (this.rootNode == null);
        this.rootNode = new Node(board);
    }

    private Node select() {
        Node node = this.rootNode;

        while (node.pendingMoves.isEmpty() && !node.children.isEmpty()) {
            node = node.selectChildUCB();
        }

        return node;
    }

    private Node expand(Node node) {
        assert (!node.pendingMoves.isEmpty());

        ArrayList<Move> moves = new ArrayList<>(node.pendingMoves);
        int i = new Random().nextInt(moves.size());
        return node.expandMove(moves.get(i));
    }

    private double simulate(Board board) {
        Board b = board.duplicate();

        Move m = b.getRandomMove();
        while (m != null && !b.gameDecided()) {
            b.playMove(m);
            m = b.getRandomMove();
        }
        return b.gameResult();
    }

    private void update(Node node, double result) {
        while (node != null) {
            node.plays++;
            node.score += node.getScore(result, this.rootNode.board.getNextTurnPlayer());
            node = node.parent;
        }
    }

    public Move getNextMove(Board board, double timeAllowed) {
        this.rootNode = new Node(board);
        return getNextMove(timeAllowed);
    }

    public Move getNextMove(double timeAllowed) {
        if (timeAllowed < 0) {
            timeAllowed = 1.0;
        }

        int iterations = 0;

        long startTime = System.nanoTime();
        while (System.nanoTime() < startTime + timeAllowed * 1000000000) {
            Node node = this.select();
            if (!node.pendingMoves.isEmpty()) {
                node = this.expand(node);
            }

            double result = this.simulate(node.board);
            this.update(node, result);

            iterations++;
        }
        // System.out.println("Iterations: " + iterations);

        // return most visited node's move
        Optional<Node> opt = this.rootNode.children.stream().max(Comparator.comparingInt(c -> c.plays));
        try {
            Node n = opt.get();
            return n.move;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public void registerMove(Move move) {
        // find child that corresponds with the given move, if it exists
        // if no such child exist, create a new node with the corresponding board
        // set this node as the new root node
        Optional<Node> optionalNode = this.rootNode.children.stream().filter(c -> c.move.equals(move)).findFirst();
        Node newRoot;
        if (optionalNode.isPresent()) {
            newRoot = optionalNode.get();
            // remove references to free up resources
            newRoot.parent = null;
            newRoot.move = null;
        } else {
            Board newBoard = this.rootNode.board.duplicate();
            newBoard.playMove(move);
            newRoot = new Node(newBoard);
        }
        this.rootNode = newRoot;
    }
}
