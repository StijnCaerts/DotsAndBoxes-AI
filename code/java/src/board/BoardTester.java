package board;

import java.util.Random;

public class BoardTester {

    public static void main(String[] args) {

        BoardTester.verifyRandomGames(false,10000, 5, 10, 5, 10, 189486484);

    }

    public static boolean verifyRandomGames(boolean verify, int amount, int minColumns, int maxColumns, int minRows, int maxRows, int seed) {

        // Simulates random games consisting of legal moves and verifies invariants after every move
        // Also undoes all moves at the end and verifies the invariants after every undo
        // If no violations are found, returns true
        // If violations are found, prints information and returns false

        // We use set seed so we can reproduce any bugs we find easily
        Random rand = new Random(seed);

        double totalCopyTime = 0;
        double totalUndoTime = 0;
        double totalMoveRegistrationTime = 0;
        double totalOptimalMovingUpdating = 0;
        double totalHeuristicInputCalculationTime = 0;
        int totalMoves = 0;
        int totalCopies = 0;

        for(int game = 0; game < amount; game++) {

            // Initialization
            int columns = rand.nextInt(maxColumns - minColumns + 1) + minColumns;
            int rows = rand.nextInt(maxRows - minRows + 1) + minRows;
            Board board = new Board(columns, rows, false);
            if (verify && !BoardTester.verifyInvariants(board))
                return false;

            if (game%1000 == 0) {
                System.out.println("Started simulating game " + game);
            }

            // Play random moves until none are left
            while(board.movesLeft > 0) {

                // Select random move
                int[] selectedEdgeCoords = board.getRandomLegalMove(rand);

                // Play and verify
                Board oldBoard = null;
                try {

                    // Copy board
                    totalCopies++;
                    long start = System.nanoTime();
                    oldBoard = board.deepcopy();
                    totalCopyTime += (System.nanoTime() - start)/1000000000.0;

                    // Play move
                    totalMoves++;
                    start = System.nanoTime();
                    board.registerMove(selectedEdgeCoords[0], selectedEdgeCoords[1]);
                    totalMoveRegistrationTime += (System.nanoTime() - start)/1000000000.0;

                    // Update optimal moves
                    start = System.nanoTime();
                    board.updateOptimalMoves();
                    totalOptimalMovingUpdating += (System.nanoTime() - start)/1000000000.0;

                    // Calculate heuristc input
                    start = System.nanoTime();
                    board.getHeuristicInput();
                    totalHeuristicInputCalculationTime += (System.nanoTime() - start)/1000000000.0;

                    // Verify
                    if (verify && !BoardTester.verifyInvariants(board))
                        throw new RuntimeException("Invariants violated.");

                } catch (RuntimeException e) {
                    System.out.println("Failed with " + board.movesLeft + " moves left!");
                    e.printStackTrace();
                    System.out.println("Old board:");
                    System.out.println(oldBoard.edgesString());
                    System.out.println("New board:");
                    System.out.println(board.edgesString());
                    return false;
                }

            }

            // Undo all moves
            if (board.recordUndo) {
                while (board.canUndo()) {
                    Board oldBoard = null;
                    try {

                        // Copy board
                        totalCopies++;
                        long start = System.nanoTime();
                        oldBoard = board.deepcopy();
                        totalCopyTime += (System.nanoTime() - start)/1000000000.0;

                        // Undo move
                        start = System.nanoTime();
                        board.undo();
                        totalUndoTime += (System.nanoTime() - start)/1000000000.0;

                        // Verify
                        if (verify && !BoardTester.verifyInvariants(board))
                            throw new RuntimeException("Invariants violated.");

                    } catch (RuntimeException e) {
                        System.out.println("Failed with " + board.movesLeft + " moves undone!");
                        e.printStackTrace();
                        System.out.println("Old board:");
                        System.out.println(oldBoard.edgesString());
                        System.out.println("New board:");
                        System.out.println(board.edgesString());
                        return false;
                    }
                }
            }

        }

        System.out.println("Tested " + amount + " games with " + minColumns + "-" + maxColumns + " columns " + minRows + "-" + maxRows + " rows.");
        System.out.println("Average board copy time: " + totalCopyTime/totalCopies);
        System.out.println("Average undo time: " + totalUndoTime/totalMoves);
        System.out.println("Average move registration time (including optimal move updating): " + totalMoveRegistrationTime/totalMoves);
        System.out.println("Average optimal move updating time: " + totalOptimalMovingUpdating/totalMoves);
        System.out.println("Average heuristic input calculation time: " + totalHeuristicInputCalculationTime/totalMoves);
        return true;

    }

    public static boolean verifyInvariants(Board board) {

        // Verifies if the invariants of the board representation are met in the given board
        // If no violations are found, returns true
        // If violations are found, prints information and returns false

        // We assume edge matrix is correct and verify state from there

        // Verify valence matrix
        for(int x = 0; x < board.columns; x++) {
            for(int y = 0; y < board.rows; y++) {
                int actualValence = 0;
                for(int[] neighborDirection : Board.neighborDirections) {
                    int nx = x + neighborDirection[0];
                    int ny = y + neighborDirection[1];
                    if (board.edges[x + nx + 1][y + ny + 1])
                        actualValence++;
                }
                if (board.valence[x][y] != actualValence) {
                    System.out.println("Invariant violation: box at " + x + ", " + y + " has stored valence " + board.valence[x][y] + " but actual valence " + actualValence);
                    return false;
                }
            }
        }

        // Verify chainAt matrix
        // Check that box is part of chain iff valence is 2 or 3
        // Check that box which is part of chain according to chainAt matrix is also part of chain according to chain
        // Check that chain which box is part of according to chainAt matrix is also in the list of chains
        for(int x = 0; x < board.columns; x++) {
            for(int y = 0; y < board.rows; y++) {
                Chain chain = board.chainAt[x][y];
                boolean shouldBeInChain = (board.valence[x][y] == 2 || board.valence[x][y] == 3);
                if (chain != null && !shouldBeInChain) {
                    System.out.println("Invariant violation: box at " + x + ", " + y + " is part of chain " + chain + " according to chainAt matrix but has valence " + board.valence[x][y]);
                    return false;
                } else if (chain == null && shouldBeInChain) {
                    System.out.println("Invariant violation: box at " + x + ", " + y + " isn't part of any chain according to chainAt matrix but has valence " + board.valence[x][y]);
                    return false;
                } else if (chain != null) {
                    // Only need to perform further verifications if box is actually in chain
                    if (!chain.boxes.contains(board.boxToInt(x, y))) {
                        System.out.println("Invariant violation: box at " + x + ", " + y + " is part of chain " + chain + " according to chainAt matrix but not according to chain itself");
                        return false;
                    }
                    if (!board.chains.contains(chain)) {
                        System.out.println("Invariant violation: box at " + x + ", " + y + " is part of chain " + chain + " but the board's list of chains doesn't contain this chain");
                        return false;
                    }
                }
            }
        }

        // Verify chains
        // Check that the chain size is equal to the size of the boxes list
        // Check that the chain size is at least 1
        // Check that each box in the chain is also part of this chain according to chainAt matrix
        // Check that all but first and last boxes have valence 2
        // Check that adjacent boxes in the chain are also adjacent and connected on the board
        // Check that first and last boxes with valence 2 are connected to at least one box without a chain, unless this chain is a loop
        // ChainType checks:
        // - OPEN: Check that first and last boxes have valence 2
        // - LOOP: Check that first and last boxes have valence 2 and are adjacent and connected
        // - HALF_OPEN: Check that first box has valence 3 and last box has valence 2
        // - CLOSED: Check that first and last boxes have valence 3 and that chain size is larger than 1
        for(Chain chain : board.chains) {

            if (chain.size != chain.boxes.size()) {
                System.out.println("Invariant violation: chain " + chain + " has stored size " + chain.size + " but actual size " + chain.boxes.size());
                return false;
            }

            if (chain.size == 0) {
                System.out.println("Invariant violation: chain " + chain + " has size 0");
                return false;
            }

            for(int i = 0; i < chain.size; i++) {

                int[] boxCoords = board.intToBox(chain.boxes.get(i));
                int x1 = boxCoords[0];
                int y1 = boxCoords[1];

                if (board.chainAt[x1][y1] != chain) {
                    System.out.println("Invariant violation: box at " + x1 + ", " + y1 + " is part of chain " + chain + " according to chain itself but not according to chainAt");
                    return false;
                }

                // Check connectivity
                if (i < chain.size - 1) {
                    boxCoords = board.intToBox(chain.boxes.get(i + 1));
                    int x2 = boxCoords[0];
                    int y2 = boxCoords[1];
                    if (!board.boxesAdjacentAndConnected(x1, y1, x2, y2)) {
                        System.out.println("Invariant violation: boxes at " + x1 + ", " + y1 + " and " + x2 + ", " + y2 + " are adjacent in chain " + chain
                                + " but not actually adjacent and connected on board");
                        return false;
                    }
                }

                if (i == 0) {
                    // First box checks
                    if (chain.type == ChainType.OPEN || chain.type == ChainType.LOOP) {
                        if (board.valence[x1][y1] != 2) {
                            System.out.println("Invariant violation: box at " + x1 + ", " + y1 + " is the first box in chain " + chain + " of type " + chain.type
                                    + " but has valence " + board.valence[x1][y1]);
                            return false;
                        }
                    } else {
                        if (board.valence[x1][y1] != 3) {
                            System.out.println("Invariant violation: box at " + x1 + ", " + y1 + " is the first box in chain " + chain + " of type " + chain.type
                                    + " but has valence " + board.valence[x1][y1]);
                            return false;
                        }
                    }
                } else if (i == chain.size - 1) {
                    // Last box checks
                    if (chain.type != ChainType.CLOSED) {
                        if (board.valence[x1][y1] != 2) {
                            System.out.println("Invariant violation: box at " + x1 + ", " + y1 + " is the last box in chain " + chain + " of type " + chain.type
                                    + " but has valence " + board.valence[x1][y1]);
                            return false;
                        }
                    } else {
                        if (board.valence[x1][y1] != 3) {
                            System.out.println("Invariant violation: box at " + x1 + ", " + y1 + " is the last box in chain " + chain + " of type " + chain.type
                                    + " but has valence " + board.valence[x1][y1]);
                            return false;
                        }
                    }
                } else {
                    // Box is neither at start or end, so just verify valence
                    if (board.valence[x1][y1] != 2) {
                        System.out.println("Invariant violation: box at " + x1 + ", " + y1 + " is a middle box in chain " + chain + " but has valence " + board.valence[x1][y1]);
                        return false;
                    }
                }

            }

            // ChainType-specific checks
            if (chain.type == ChainType.LOOP) {

                int[] boxCoords = board.intToBox(chain.boxes.get(0));
                int x1 = boxCoords[0];
                int y1 = boxCoords[1];
                boxCoords = board.intToBox(chain.boxes.get(chain.size - 1));
                int x2 = boxCoords[0];
                int y2 = boxCoords[1];
                if (!board.boxesAdjacentAndConnected(x1, y1, x2, y2)) {
                    System.out.println("Invariant violation: chain " + chain + " of type " + chain.type + " has first box at " + x1 + ", " + y1
                            + " and last box at " + x2 + ", " + y2 + " but these aren't adjacent and connected");
                    return false;
                }

            } else {

                // Check if first and last boxes (if they have valence 2) are connected to at least one box without a chain
                for(int i = 0; i < chain.size; i += chain.size - 1) {
                    int[] boxCoords = board.intToBox(chain.boxes.get(i));
                    int x = boxCoords[0];
                    int y = boxCoords[1];
                    if (board.valence[x][y] == 2) {
                        boolean found = false;
                        for (int[] neighborDirection : Board.neighborDirections) {
                            int nx = x + neighborDirection[0];
                            int ny = y + neighborDirection[1];
                            // Neighbor can be off-board, boxesConnected will simply take the edge in between
                            if (board.boxesConnected(x, y, nx, ny) && (!board.onBoard(nx, ny) || board.chainAt[nx][ny] == null)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            System.out.println("Invariant violation: chain " + chain + " of type " + chain.type + " has first/last box at " + x + ", " + y
                                    + " but couldn't find an adjacent and connected box without a chain");
                            return false;
                        }
                    }
                    if (chain.size == 1)
                        break;
                }

                if (chain.type == ChainType.CLOSED) {
                    if (chain.size == 1) {
                        System.out.println("Invariant violation: chain " + chain + " of type " + chain.type + " has size 1");
                        return false;
                    }
                }
            }

        }

        // No checks failed
        return true;

    }

}
