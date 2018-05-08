package MCTS3;

import java.util.*;

public class MCTS {

    private Node rootNode;

    private Node select() {
        Node node = this.rootNode;

        while(node.pendingMoves.isEmpty() && !node.children.isEmpty()) {
            node = node.selectChildUCB();
        }

        return node;
    }

    private Node expand(Node node) {
        assert(!node.pendingMoves.isEmpty());

        ArrayList<Move> moves = new ArrayList<>(node.pendingMoves);
        int i = new Random().nextInt(moves.size());
        return node.expandMove(moves.get(i));
    }

    private double simulate(Board board, int maxIterations) {
        Board b = board.duplicate();

        Move m = b.getRandomMove();
        while(m != null && !b.gameDecided()) {
            b.playMove(m);
            m = b.getRandomMove();

            maxIterations--;
            if(maxIterations <= 0) {
                throw new IllegalArgumentException("Game to deep to simulate with " + Integer.toString(maxIterations) + " iterations.");
            }
        }
        return b.gameResult();
    }

    private double simulate(Board board) {
        return simulate(board, 1000);
    }

    private void update(Node node, double result) {
        while(node != null) {
            node.plays++;
            node.score += node.getScore(result);
            node = node.parent;
        }
    }

    public Move getNextMove(Board board, double timeAllowed) {
        if(timeAllowed < 0) {
            timeAllowed = 1.0;
        }
        this.rootNode = new Node(board);
        int iterations = 0;

        long startTime = System.nanoTime();
        while(System.nanoTime() < startTime + timeAllowed*1000000000) {
            Node node = this.select();
            if(!node.pendingMoves.isEmpty()) {
                node = this.expand(node);
            }

            double result = this.simulate(node.board);
            this.update(node, result);

            iterations++;
        }

        // return most visited node's move
        Optional<Node> opt = this.rootNode.children.stream().max(Comparator.comparingInt(c -> c.plays));
        try {
            Node n = opt.get();
            return n.move;
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
