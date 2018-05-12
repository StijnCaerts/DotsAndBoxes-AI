package MCTS2;

public class AsyncSearchAgent extends MCTSAgent {

    private Thread searchThread;
    private AsyncSearch asyncSearch;

    public AsyncSearchAgent(int player, double timeLimit, int rows, int columns, String gameId) {
        super(player, timeLimit, rows, columns, gameId);
        this.asyncSearch = new AsyncSearch();
    }

    @Override
    public void registerAction(int ownScore, int opponentScore, int x, int y) {
        // stop async search
        if(this.searchThread != null) {
            this.asyncSearch.stop();
            try {
                this.searchThread.join();
                this.searchThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        super.registerAction(ownScore, opponentScore, x, y);

        // start asyncsearch if next user is other user
        if(this.rootNode.board.hasOptimalMoves() && this.rootNode.board.getCurrentPlayer() != this.player) {
            this.searchThread = new Thread(this.asyncSearch);
            this.searchThread.start();
        }
    }

    class AsyncSearch implements Runnable {
        private volatile boolean running;

        @Override
        public void run() {
            this.running = true;
            while(this.running) {
                search();
            }
        }

        public void stop() {
            this.running = false;
        }

        private void search() {
            // Selection
            Node node = select();

            // Expansion
            if(node.canExpand()) {
                node = node.expand(rand);
            }

            // Simulation
            double result = simulate(node.board);

            // Back-propagation
            update(node, result);

            iterations++;
        }
    }
}


