package MCTS2;

import java.util.Arrays;

public class SelfPlay {

    private Board startingBoard;

    private Board currentBoard;

    private MCTS mcts;

    private long numGamesToPlay = 0;

    private int numMctsIterations = 1000;

    long[] scores;

    boolean showEachMove = false;

    int trainAfterGameCount = 0;

    public SelfPlay() {
        mcts = new MCTS();
        mcts.setExplorationConstant(0.2);
        mcts.setTimeDisplay(false);
        mcts.setOptimisticBias(0.0);
        mcts.setPessimisticBias(0.0);
        mcts.setMoveSelectionPolicy(FinalSelectionPolicy.robustChild);
    }

    public Board getBoard() {
        return startingBoard;
    }

    public void setBoard(Board board) {
        this.startingBoard = board;
    }

    public MCTS getMcts() {
        return mcts;
    }

    public void setMcts(MCTS mcts) {
        this.mcts = mcts;
    }

    public long getNumGamesToPlay() {
        return numGamesToPlay;
    }

    public long getNumWinsPlayer1() {
        return scores[0];
    }

    public long getNumWinsPlayer2() {
        return scores[1];
    }

    public long getNumDraws() {
        return scores[scores.length - 1];
    }

    public void setNumGamesToPlay(long numGamesToPlay) {
        this.numGamesToPlay = numGamesToPlay;
    }

    public int getNumMctsIterations() {
        return numMctsIterations;
    }

    public void setNumMctsIterations(int numMctsIterations) {
        this.numMctsIterations = numMctsIterations;
    }
    
    public int getTrainAfterGameCount() {
        return trainAfterGameCount;
    }

    public void setTrainAfterGameCount(int trainAfterGameCount) {
        this.trainAfterGameCount = trainAfterGameCount;
    }

    public void go() {
        init();
        for (int i = 0; i < numGamesToPlay; i++) {
            Board b = playOneGame();
            processForTraining(b, i);
        }
    }

    private void processForTraining(Board b, int i) {
        if (trainAfterGameCount == 0) {
            return;
        }
        
        if (i > 0 && i % trainAfterGameCount == 0) {
            mcts.playoutAi.train();
        }
        
        mcts.playoutAi.addCompletedGame(b);
        
    }

    private void init() {
        scores = new long[startingBoard.getQuantityOfPlayers() + 1];
    }

    private Board playOneGame() {
        currentBoard = startingBoard.duplicate();
        while (!currentBoard.gameOver()) {
            playOneMove(currentBoard);
        }
        processScore(currentBoard);
        return currentBoard;
    }

    private void playOneMove(Board board2) {
        Move move = mcts.findBestMove(currentBoard, numMctsIterations, false);
        currentBoard.makeMove(move);
        if (showEachMove) {
            System.out.println("move = " + move.toString());
            System.out.println(currentBoard.toString());
        }
    }

    private void processScore(Board board2) {
        int index = getScoreIndex(currentBoard.getScore());
        scores[index]++;
        String finalStatus = getGameStatus(index);
        System.out.println("Last game = " + finalStatus + "   all games = " + Arrays.toString(scores));
        if (showEachMove) {
            System.out.println("\n\n");
        }
    }

    private String getGameStatus(int index) {
        if (index == scores.length - 1) {
            return "Draw";
        }
        return "Winner player " + (index + 1);
    }

    private int getScoreIndex(double[] scores) {
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0.9) {
                return i;
            }
        }
        return scores.length;
    }

    public void setPlayoutAi(PlayoutAi playoutAi) {
        mcts.setPlayoutAi(playoutAi);
    }

    public void setParallel(int numThreads) {
        mcts.enableRootParallelisation(numThreads);        
    }
}
