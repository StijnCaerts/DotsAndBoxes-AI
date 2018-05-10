package board;

import MCTS.DBMove;
import MCTS.Move;
import exceptions.GameStateNotDecidedException;
import math.Vector;

import java.util.*;

public class Board implements MCTS.Board {

    public static final int[][] neighborDirections = new int[][] {
            {1, 0},
            {0, -1},
            {-1, 0},
            {0, 1},
    };

    // These don't actually limit chains on the board, just how they are presented to the heuristic
    public static final int maxOpenChainSize = 8;
    public static final int maxLoopSize = 4;

    // General
    public final int columns, rows;
    public boolean recordUndo;
    public int currentPlayer = 0;
    public int[] scores = new int[2];

    // Temporary variables used during calculations, doesn't store state across multiple moves
    public boolean boxClosed; // Whether or not a box was closed during this move
    public boolean chainSplit; // Whether or not a previous box update already split a shared chain

    // Board representation
    public boolean[][] edges; // false means no line has been drawn yet
    public int[][] valence; // Amount of lines next to box, starts at 0
    public Chain[][] chainAt; // Stores the chain each box belongs to, null for boxes with valence 0, 1, or 4, not null for all boxes with valence 2 or 3
    public HashSet<Chain> chains; // Mostly used for adding/removing instead of iteration, so HashSet instead of ArrayList

    // Moves
    public int movesLeft;
    public int[] movesLeftPerColumn; // Used to quickly iterate and random-access moves
    public int[] optimalMoves;

    // Undo
    public Transaction currentTransaction;
    public ArrayList<Transaction> undoStack;

    // Interface methods

    public Board(int columns, int rows, boolean recordUndo) {

        this.columns = columns;
        this.rows = rows;
        this.recordUndo = recordUndo;

        // Board representation initialization
        this.edges = new boolean[2*columns + 1][2*rows + 1];
        this.valence = new int[columns][rows];
        this.chainAt = new Chain[columns][rows];
        this.chains = new HashSet<>();
        this.movesLeft = 2*this.columns*this.rows + this.columns + this.rows;
        this.movesLeftPerColumn = new int[2*this.columns + 1];
        for(int x = 0; x < 2*this.columns + 1; x++) {
            this.movesLeftPerColumn[x] = this.rows + x%2;
        }
        this.optimalMoves = new int[0];

        this.undoStack = new ArrayList<>();

    }

    public Board deepcopy() {

        // Creates a deep copy of the important data of this board (including the board representation, but not undo stack nor variables used for temporary calculations)

        // Copy rows, columns, current player and scores
        Board newBoard = new Board(this.columns, this.rows, this.recordUndo);
        newBoard.currentPlayer = this.currentPlayer;
        newBoard.scores = new int[] {this.scores[0], this.scores[1]};

        // Copy edges
        for(int x = 0; x < 2*this.columns + 1; x++) {
            for(int y = (x + 1)%2; y < 2*this.rows + 1; y += 2) {
                newBoard.edges[x][y] = this.edges[x][y];
            }
        }

        // Copy chains
        HashMap<Chain, Chain> chainMap = new HashMap<>();
        for(Chain chain : this.chains) {
            Chain newChain = new Chain(new ArrayList<>(chain.boxes), chain.type);
            newBoard.chains.add(newChain);
            chainMap.put(chain, newChain); // Tested, copying ArrayList<Integer> like this works, and enum is immutable so can't be copied
        }

        // Copy valence and chainAt matrix
        for(int x = 0; x < this.columns; x++) {
            for(int y = 0; y < this.rows; y++) {
                newBoard.valence[x][y] = this.valence[x][y];
                newBoard.chainAt[x][y] = chainMap.get(this.chainAt[x][y]);
            }
        }

        // Copy moves
        newBoard.movesLeft = this.movesLeft;
        newBoard.movesLeftPerColumn = new int[this.movesLeftPerColumn.length];
        System.arraycopy(this.movesLeftPerColumn, 0, newBoard.movesLeftPerColumn, 0, this.movesLeftPerColumn.length);
        newBoard.optimalMoves = new int[this.optimalMoves.length];
        System.arraycopy(this.optimalMoves, 0, newBoard.optimalMoves, 0, this.optimalMoves.length);

        return newBoard;

    }

    public boolean canUndo() {
        return this.undoStack.size() > 0;
    }

    public void undo() {
        // Undoes the last transaction on the undo stack
        if (canUndo())
            undoMove(this.undoStack.remove(this.undoStack.size() - 1));
    }

    public String edgesString() {

        // Converts this edges matrix to a human-readable string
        String res = "";
        for(int y = 0; y < 2*this.rows + 1; y++) {
            for(int x = 0; x < 2*this.columns + 1; x++) {
                if (x%2 == 0 && y%2 == 0) {
                    // Node
                    res += ".";
                } else if (x%2 == 1 && y%2 == 1) {
                    // Box
                    res += " ";
                } else {
                    // Edges
                    if (this.edges[x][y]) {
                        if (y%2 == 0) {
                            // Horizontal edge
                            res += "_";
                        } else {
                            // Vertical edge
                            res += "|";
                        }
                    } else {
                        res += " ";
                    }
                }
            }
            if (y != 2*this.rows)
                res += "\n";
        }
        return res;

    }

    public int[] intToEdge(int edge) {
        // Converts an int ID to a edge
        return new int[] {edge%(2*this.columns + 1), edge/(2*this.columns + 1)};
    }

    public int edgeToInt(int x, int y) {
        // Converts a edge to an int ID
        return y*(2*this.columns + 1) + x;
    }

    public void registerMove(int x, int y) {

        // x, y are in the edge coordinate system (so in a grid of size (2*columns + 1)x(2*rows + 1))

        // Record main part of move in transaction if necessary
        if (this.recordUndo) {
            this.currentTransaction = new Transaction(x, y, this.optimalMoves, this.currentPlayer, this.scores);
        }

        // Update edge matrix
        this.edges[x][y] = true;

        // Update legal moves
        this.movesLeft--;
        this.movesLeftPerColumn[x]--;

        // Update valence matrix
        this.boxClosed = false;
        this.chainSplit = false;
        if (x%2 == 0) {
            // Vertical edge, increase valence of left and right boxes
            if (x/2 - 1 >= 0) {
                boxUpdate(x/2 - 1, y/2);
            }
            if (x/2 < this.columns) {
                boxUpdate(x/2, y/2);
            }
        } else {
            // Horizontal edge, increase valence of top and bottom boxes
            if (y/2 - 1 >= 0) {
                boxUpdate(x/2, y/2 - 1);
            }
            if (y/2 < this.rows) {
                boxUpdate(x/2, y/2);
            }
        }

        // Update optimal moves
        updateOptimalMoves();

        // If during updating no boxes were closed, switch players
        if (!this.boxClosed)
            this.currentPlayer = (this.currentPlayer + 1)%2;

        // Push current transaction if necessary
        if (this.recordUndo) {
            this.undoStack.add(this.currentTransaction);
        }

    }

    public boolean hasOptimalMoves() {
        return this.optimalMoves.length > 0;
    }

    public int[] getOptimalMoves() {
        // Returns the actual object, caller should take care of reference semantics
        return this.optimalMoves;
    }

    public MoveIterator getLegalMoveIterator() {
        // Returns a new legal move iterator for this board
        return new MoveIterator(this);
    }

    public int[] getRandomLegalMove(Random rand) {
        int index = rand.nextInt(this.movesLeft);
        for(int x = 0; x < 2*this.columns + 1; x++) {
            if (index >= this.movesLeftPerColumn[x]) {
                // Move is not in this column, move on
                index -= this.movesLeftPerColumn[x];
                continue;
            } else {
                // Move is in this column, iterate through rows
                for(int y = (x + 1)%2; y < 2*this.rows + 1; y += 2) {
                    if (!this.edges[x][y]) {
                        if (index == 0) {
                            return new int[] {x, y};
                        }
                        index--;
                    }
                }
            }
        }
        assert(false);
        return null;
    }

    public math.Vector getHeuristicInput() {
        // Calculates heuristic input based on board representation
        // Shouldn't be called more than once per move for efficiency
        // Heuristic input consists of (in this order):
        // - score of current player
        // - score of other player
        // - amount of open chains of size 1 to maxOpenChainSize (inclusive),
        // - amount of loops of size 4 to maxLoopSize (inclusive)
        double[] res = new double[2 + Board.maxOpenChainSize + Board.maxLoopSize - 3];
        res[0] = this.scores[this.currentPlayer];
        res[1] = this.scores[(this.currentPlayer + 1)%2];
        for(Chain chain : this.chains) {
            if (chain.type == ChainType.OPEN) {
                res[1 + Math.min(Board.maxOpenChainSize, chain.size)]++;
            } else if (chain.type == ChainType.LOOP) {
                res[-2 + Board.maxOpenChainSize + Math.min(Board.maxLoopSize, chain.size)]++;
            }
        }
        return new Vector(res);
    }

    // Helper methods

    protected void undoMove(Transaction transaction) {

        // Reverses the move described by this transaction

        // Undo main part of move
        int x = transaction.x;
        int y = transaction.y;
        this.edges[x][y] = false;
        this.movesLeft++;
        this.movesLeftPerColumn[x]++;
        this.currentPlayer = transaction.currentPlayer;
        this.scores = transaction.scores;

        // Undo box valence updates
        for(int i = 0; i < transaction.boxesAmount; i++) {
            this.valence[transaction.boxCoords[i][0]][transaction.boxCoords[i][1]]--;
        }

        // Undo box chain updates in reverse order
        for(int i = transaction.subTransactions.size() - 1; i >= 0; i--) {
            SubTransaction subTransaction = transaction.subTransactions.get(i);
            subTransaction.undo();
        }

    }

    protected void pushSubTransaction(SubTransaction subTransaction) {
        // Should only be called when there is a current transaction
        this.currentTransaction.subTransactions.add(subTransaction);
    }

    protected void updateOptimalMoves() {

        // Calculates zero to two optimal moves
        // From https://www.aaai.org/ocs/index.php/AAAI/AAAI12/paper/viewFile/5126/5218:
        // "In states with more than one chain, we can completely fill in all but one of the chains and follow the appropriate strategy for the last-remaining chain.
        // In these cases, a half-open chain should be left for last, if possible, as this requires sacrificing only two boxes when leaving a hard-hearted handout."
        // Note that the chain we keep should have enough space to create a hard-hearted handout (at least 4 boxes in closed chains, at least 2 in half-open chains)

        // Find chains to play or to keep for a later decision
        Chain validHalfOpenChain = null;
        Chain validClosedChain = null;
        Chain chainToPlay = null;
        for(Chain chain : this.chains) {

            if ((chain.type == ChainType.HALF_OPEN && chain.size != 2) || (chain.type == ChainType.CLOSED && chain.size != 4)) {

                // Even if we play a box in this chain, it will remain a valid/invalid chain, so we don't have to choose yet
                chainToPlay = chain;
                break;

            } else if (chain.type == ChainType.HALF_OPEN) {

                // Valid half-open chain
                if (validHalfOpenChain == null) {
                    // Store as valid half-open chain
                    validHalfOpenChain = chain;
                    if (validClosedChain != null) {
                        // We already found a valid closed chain and stored it, so mark the closed chain to be played and keep this one for later
                        chainToPlay = validClosedChain;
                        break;
                    }
                } else {
                    // We already found another valid half-open chain, so we can play in this one
                    chainToPlay = chain;
                    break;
                }

            } else if (chain.type == ChainType.CLOSED) {

                // Valid closed chain
                if (validHalfOpenChain == null && validClosedChain == null) {
                    // Store as valid closed chain only if no other candidates have been found yet, including half-open chains
                    validClosedChain = chain;
                } else {
                    // We already have a valid candidate to keep for later, so play in this one
                    chainToPlay = validClosedChain;
                    break;
                }

            }
        }

        if (chainToPlay != null) {

            // Play in this chain right away, no choice required
            if (chainToPlay.size > 1) {
                // Just play in between box 0 and 1
                int[] boxCoords1 = intToBox(chainToPlay.boxes.get(0));
                int[] boxCoords2 = intToBox(chainToPlay.boxes.get(1));
                this.optimalMoves = new int[] {edgeToInt(boxCoords1[0] + boxCoords2[0] + 1, boxCoords1[1] + boxCoords2[1] + 1)};
            } else {
                // Chain is half-open and has size 1
                int[] boxCoords = intToBox(chainToPlay.boxes.get(0));
                for(int[] neighborDirection : Board.neighborDirections) {
                    int x = 2*boxCoords[0] + 1 + neighborDirection[0];
                    int y = 2*boxCoords[1] + 1 + neighborDirection[1];
                    if (!this.edges[x][y]) {
                        this.optimalMoves = new int[] {edgeToInt(x, y)};
                        break;
                    }
                }
            }

        } else if (validHalfOpenChain != null || validClosedChain != null) {

            // At most one valid chain was found, so play half-hearted hand-out
            // Chain is either closed with size 4 or open with size 2
            // In both cases, choose in between two open edges around second box

            // Choose in between two open edges around second box
            int[] boxCoords = intToBox((validHalfOpenChain != null ? validHalfOpenChain : validClosedChain).boxes.get(1));
            this.optimalMoves = new int[2];
            int i = 0;
            for(int[] neighborDirection : Board.neighborDirections) {
                int x = 2*boxCoords[0] + 1 + neighborDirection[0];
                int y = 2*boxCoords[1] + 1 + neighborDirection[1];
                if (!this.edges[x][y]) {
                    this.optimalMoves[i++] = edgeToInt(x, y);
                    if (i == 2)
                        break;
                }
            }

        } else {
            // No valid chains or chains to play in were found, so there are no optimal moves
            this.optimalMoves = new int[0];
        }

    }

    protected void boxUpdate(int x, int y) {

        // x and y are in the box coordinate system (so in a grid of size columns x rows)

        // Increase valence
        this.valence[x][y]++;
        if (this.recordUndo) {
            this.currentTransaction.boxCoords[this.currentTransaction.boxesAmount++] = new int[] {x, y};
        }

        // Check if a box is made
        if (this.valence[x][y] == 4) {
            this.boxClosed = true;
            this.scores[this.currentPlayer]++;
        }

        // Update chains
        int box = boxToInt(x, y);
        int x2, y2;
        switch(this.valence[x][y]) {

            case 2:

                // New valence = 2: box wasn't part of a chain but will be part of a chain
                // Check both non-filled edges to see if those boxes are already part of chains and merge relevant chains if so

                // Find neighboring boxes in chains
                int neighboringChains = 0;
                x2 = -1;
                y2 = -1;
                int x3 = -1;
                int y3 = -1;
                for(int[] neighborDirection : Board.neighborDirections) {
                    // Iterate through neighboring boxes
                    int nx = x + neighborDirection[0];
                    int ny = y + neighborDirection[1];
                    if (onBoard(nx, ny) && boxesConnected(x, y, nx, ny) && this.chainAt[nx][ny] != null) {
                        neighboringChains++;
                        if (neighboringChains == 1) {
                            x2 = nx;
                            y2 = ny;
                        } else {
                            x3 = nx;
                            y3 = ny;
                            break;
                        }
                    }
                }

                // Update chains as required
                Chain chain;
                switch(neighboringChains) {
                    case 0:
                        // Box simply becomes open chain by itself
                        chain = new Chain(box, ChainType.OPEN);
                        this.chains.add(chain);
                        this.chainAt[x][y] = chain;
                        if (this.recordUndo) {
                            pushSubTransaction(new SubTransaction() {
                                @Override
                                public void undo() {
                                    chains.remove(chain);
                                    chainAt[x][y] = null;
                                }
                            });
                        }
                        break;
                    case 1:
                        // Box merges with end of existing chain, chain keeps type (open or half-open)
                        chain = this.chainAt[x2][y2];
                        this.chainAt[x][y] = chain;
                        int neighborBox = boxToInt(x2, y2);
                        if (chain.size == 1 || chain.boxes.get(0) != neighborBox) {
                            chain.append(box);
                            if (this.recordUndo) {
                                pushSubTransaction(new SubTransaction() {
                                    @Override
                                    public void undo() {
                                        chainAt[x][y] = null;
                                        chain.removeEnd();
                                    }
                                });
                            }
                        } else {
                            chain.prepend(box);
                            if (this.recordUndo) {
                                pushSubTransaction(new SubTransaction() {
                                    @Override
                                    public void undo() {
                                        chainAt[x][y] = null;
                                        chain.removeStart();
                                    }
                                });
                            }
                        }
                        break;
                    case 2:
                        // Both chains become one (any type) and this box is put in the middle of them
                        Chain chain1 = this.chainAt[x2][y2];
                        Chain chain2 = this.chainAt[x3][y3];
                        if (chain1 == chain2) {
                            // Chains on both sides are the same, so it becomes a loop
                            // Box can be added on either side
                            this.chainAt[x][y] = chain1;
                            chain1.type = ChainType.LOOP;
                            chain1.append(box);
                            if (this.recordUndo) {
                                Chain finalChain1 = chain1;
                                pushSubTransaction(new SubTransaction() {
                                    @Override
                                    public void undo() {
                                        chainAt[x][y] = null;
                                        finalChain1.type = ChainType.OPEN;
                                        finalChain1.removeEnd();
                                    }
                                });
                            }
                        } else {

                            // Chains on both sides are different, so it doesn't become a loop

                            if (chain1.type == ChainType.HALF_OPEN || chain2.type == ChainType.HALF_OPEN) {

                                // At least one of the chains is half-open
                                // Keep this chain and append the other one to it

                                // Check if one of the chains is half-open, we will keep this one first
                                if (chain1.type != ChainType.HALF_OPEN) {
                                    Chain temp = chain1;
                                    chain1 = chain2;
                                    chain2 = temp;
                                }
                                this.chainAt[x][y] = chain1;
                                chain1.append(box);

                                // Update chain 1 type
                                if (chain2.type == ChainType.HALF_OPEN) {
                                    chain1.type = ChainType.CLOSED;
                                }

                                // Check which side of chain connects to new box
                                int[] chain2Start = intToBox(chain2.boxes.get(0));
                                Board.appendChain(chain1, chain2, !((chain2Start[0] == x2 && chain2Start[1] == y2) || (chain2Start[0] == x3 && chain2Start[1] == y3)));
                                markAndRemoveChain(chain2, chain1);

                                if (this.recordUndo) {
                                    // chain1 is always half-open at the start of this case
                                    // chain2 is not changed, just removed from chains set, so can be stored to be re-added later
                                    Chain finalChain1 = chain1;
                                    Chain finalChain2 = chain2;
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chainAt[x][y] = null;
                                            finalChain1.type = ChainType.HALF_OPEN;
                                            finalChain1.removeEndRange(finalChain2.size + 1);
                                            markAndAddChain(finalChain2);
                                        }
                                    });
                                }

                            } else {

                                // Both chains are open
                                // Keep chain 1 and add the other one to it
                                // Chain type stays open

                                this.chainAt[x][y] = chain1;
                                int[] chain1Start = intToBox(chain1.boxes.get(0));
                                if (chain1Start[0] == x2 && chain1Start[1] == y2) {
                                    chain1.prepend(box);
                                    int[] chain2Start = intToBox(chain2.boxes.get(0));
                                    Board.prependChain(chain1, chain2, !(chain2Start[0] == x3 && chain2Start[1] == y3));

                                    if (this.recordUndo) {
                                        // chain2 is not changed, just removed from chains set, so can be stored to be re-added later
                                        Chain finalChain1 = chain1;
                                        Chain finalChain2 = chain2;
                                        pushSubTransaction(new SubTransaction() {
                                            @Override
                                            public void undo() {
                                                chainAt[x][y] = null;
                                                finalChain1.removeStartRange(finalChain2.size + 1);
                                                markAndAddChain(finalChain2);
                                            }
                                        });
                                    }

                                } else {
                                    chain1.append(box);
                                    int[] chain2Start = intToBox(chain2.boxes.get(0));
                                    Board.appendChain(chain1, chain2, !(chain2Start[0] == x3 && chain2Start[1] == y3));

                                    if (this.recordUndo) {
                                        // chain2 is not changed, just removed from chains set, so can be stored to be re-added later
                                        Chain finalChain1 = chain1;
                                        Chain finalChain2 = chain2;
                                        pushSubTransaction(new SubTransaction() {
                                            @Override
                                            public void undo() {
                                                chainAt[x][y] = null;
                                                finalChain1.removeEndRange(finalChain2.size + 1);
                                                markAndAddChain(finalChain2);
                                            }
                                        });
                                    }
                                }
                                markAndRemoveChain(chain2, chain1);

                            }

                        }
                        break;
                    default:
                        break;
                }

                break;

            case 3:

                // New valence = 3: box was already part of a chain and will be part of a chain
                // Chain will go from open to half-open or from half-open to closed and possibly split up
                // Effect of other box with increased valence:
                // Edge of board: same, we don't need to take the edges into account
                // Other new valence = 1: do nothing, same
                // Other new valence = 2: look for chains to connect to, but is disconnected from this box so stays the same
                // Other new valence = 3: both boxes were part of the same chain and are now split up, needs asymmetric handling
                // Other new valence = 4: we're in a half-open or closed chain, make it one shorter

                chain = this.chainAt[x][y];
                int index = chain.boxes.indexOf(box); // Own position in chain;
                int splitIndex;
                switch(chain.type) {
                    case OPEN:

                        splitIndex = findSplitIndex(chain, x, y, index);
                        if (splitIndex == 0) {
                            // Split at start, just change chain type
                            chain.type = ChainType.HALF_OPEN;
                            if (this.recordUndo) {
                                pushSubTransaction(new SubTransaction() {
                                    @Override
                                    public void undo() {
                                        chain.type = ChainType.OPEN;
                                    }
                                });
                            }
                        } else if (splitIndex == chain.size) {
                            // Split at end, reverse box order and change chain type
                            Collections.reverse(chain.boxes);
                            chain.type = ChainType.HALF_OPEN;
                            if (this.recordUndo) {
                                pushSubTransaction(new SubTransaction() {
                                    @Override
                                    public void undo() {
                                        Collections.reverse(chain.boxes);
                                        chain.type = ChainType.OPEN;
                                    }
                                });
                            }
                        } else {
                            if (!this.chainSplit) {

                                // Split in middle, split into two half-open chains
                                // We keep first part in old chain and reverse it, while copying the second part to a new chain

                                Chain newChain = new Chain(new ArrayList<>(chain.boxes.subList(splitIndex, chain.size)), ChainType.HALF_OPEN); // Copy second part to new chain
                                markAndAddChain(newChain);
                                chain.boxes.subList(splitIndex, chain.size).clear(); // Remove second part from old chain
                                Collections.reverse(chain.boxes); // Fix order in old chain
                                chain.size = splitIndex; // Update old chain's size
                                chain.type = ChainType.HALF_OPEN; // Fix old chain's type

                                if (this.recordUndo) {
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chain.type = ChainType.OPEN;
                                            chain.size += newChain.size;
                                            Collections.reverse(chain.boxes);
                                            chain.boxes.addAll(newChain.boxes);
                                            markAndRemoveChain(newChain, chain);
                                        }
                                    });
                                }

                                this.chainSplit = true;

                            }
                        }

                        break;
                    case LOOP:

                        // Marked edge must be inside of loop, so loop becomes closed chain
                        // This case can only occur if this box is the first to be updated, so no check for chain splits
                        // Chain size stays the same, boxes may need to be re-ordered

                        // Find neighboring connected box
                        int neighborBox = -1;
                        for(int[] neighborDirection : Board.neighborDirections) {
                            // Iterate through neighboring boxes
                            int nx = x + neighborDirection[0];
                            int ny = y + neighborDirection[1];
                            if (onBoard(nx, ny) && boxesConnected(x, y, nx, ny)) {
                                neighborBox = boxToInt(nx, ny);
                                break;
                            }
                        }

                        // Rotate boxes to make sure the list starts and ends with the right boxes
                        int shift;
                        if (chain.boxes.get((index + 1)%chain.size) == neighborBox) {
                            // Same ordering
                            // Rotate list so that box is in front
                            shift = -index;
                            Collections.rotate(chain.boxes, shift);
                        } else {
                            // Reverse ordering
                            // Rotate list so that box is at the back
                            shift = chain.size - 1 - index;
                            Collections.rotate(chain.boxes, shift);
                        }

                        chain.type = ChainType.CLOSED;

                        if (this.recordUndo) {
                            pushSubTransaction(new SubTransaction() {
                                @Override
                                public void undo() {
                                    chain.type = ChainType.LOOP;
                                    Collections.rotate(chain.boxes, -shift);
                                }
                            });
                        }

                        this.chainSplit = true;

                        break;
                    case HALF_OPEN:

                        splitIndex = findSplitIndex(chain, x, y, index);
                        if (!this.chainSplit) {

                            if (splitIndex == chain.size) {
                                // Split at end, just change chain type
                                chain.type = ChainType.CLOSED;
                                if (this.recordUndo) {
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chain.type = ChainType.HALF_OPEN;
                                        }
                                    });
                                }
                            } else if (splitIndex == 1) {

                                // Split one box from start
                                // We keep second part in old chain and remove first part
                                int[] removedBoxCoords = intToBox(chain.boxes.get(0));
                                this.chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = null;
                                chain.removeIndex(0);
                                if (this.recordUndo) {
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chain.prepend(boxToInt(removedBoxCoords[0], removedBoxCoords[1]));
                                            chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = chain;
                                        }
                                    });
                                }

                            } else {

                                // Split in middle, split into a closed and half-open chain
                                // We keep first part in old chain, while copying the second part to a new chain

                                Chain newChain = new Chain(new ArrayList<>(chain.boxes.subList(splitIndex, chain.size)), ChainType.HALF_OPEN); // Copy second part to new chain
                                markAndAddChain(newChain); // Mark boxes in second part as part of new chain
                                chain.boxes.subList(splitIndex, chain.size).clear(); // Remove second part from old chain
                                chain.size = splitIndex; // Update old chain's size
                                chain.type = ChainType.CLOSED; // Fix old chain's type

                                if (this.recordUndo) {
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chain.type = ChainType.HALF_OPEN;
                                            chain.size += newChain.size;
                                            chain.boxes.addAll(newChain.boxes);
                                            markAndRemoveChain(newChain, chain);
                                        }
                                    });
                                }

                            }

                            this.chainSplit = true;

                        }

                        break;
                    case CLOSED:

                        if (!this.chainSplit) {

                            // There is a split within a closed chain

                            // Handle different split cases
                            splitIndex = findSplitIndex(chain, x, y, index);
                            if (splitIndex == 1) {

                                // Split at start of chain
                                // Remove first box from chain
                                int[] removedBoxCoords = intToBox(chain.boxes.get(0));
                                this.chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = null;
                                chain.removeIndex(0);

                                if (this.recordUndo) {
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chain.prepend(boxToInt(removedBoxCoords[0], removedBoxCoords[1]));
                                            chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = chain;
                                        }
                                    });
                                }

                            } else if (splitIndex == chain.size - 1) {

                                // Split at end of chain
                                // Remove last box from chain
                                int[] removedBoxCoords = intToBox(chain.boxes.get(chain.size - 1));
                                this.chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = null;
                                chain.removeIndex(chain.size - 1);

                                if (this.recordUndo) {
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chain.append(boxToInt(removedBoxCoords[0], removedBoxCoords[1]));
                                            chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = chain;
                                        }
                                    });
                                }

                            } else {

                                // Split somewhere in the middle
                                // We keep first part in the old chain and create a new chain for the second part
                                Chain newChain = new Chain(new ArrayList<>(chain.boxes.subList(splitIndex, chain.size)), ChainType.CLOSED); // Copy second part to new chain
                                markAndAddChain(newChain); // Mark boxes in second part as part of new chain
                                chain.boxes.subList(splitIndex, chain.size).clear(); // Remove second part from old chain
                                chain.size = splitIndex; // Update old chain's size

                                if (this.recordUndo) {
                                    pushSubTransaction(new SubTransaction() {
                                        @Override
                                        public void undo() {
                                            chain.size += newChain.size;
                                            chain.boxes.addAll(newChain.boxes);
                                            markAndRemoveChain(newChain, chain);
                                        }
                                    });
                                }

                            }

                            this.chainSplit = true;
                        }

                        break;
                    default:
                        break;
                }

                break;

            case 4:

                // New valence = 4: box was part of a chain but will not be part of a chain anymore
                // Remove box from its old chain and update chain (removing it if it has size 0)

                if (!this.chainSplit) {

                    // Box wasn't closed as part of some other chain split yet

                    // Check if box needs updating
                    chain = this.chainAt[x][y];

                    if (chain.size == 1) {
                        // Half-open chain simply closed itself, so remove chain
                        markAndRemoveChain(chain, null);
                        if (this.recordUndo) {
                            pushSubTransaction(new SubTransaction() {
                                @Override
                                public void undo() {
                                    markAndAddChain(chain);
                                }
                            });
                        }
                    } else {

                        // Chain has at least size 2

                        index = chain.boxes.indexOf(box); // Own position in chain;
                        int[] neighborCoords = intToBox(chain.boxes.get(index == 0 ? 1 : chain.size - 2));
                        int actualNeighborValence = 0; // May not equal valence stored in matrix because the box still needs to be updated
                        for(int[] neighborDirection : Board.neighborDirections) {
                            if (this.edges[2 * neighborCoords[0] + 1 + neighborDirection[0]][2 * neighborCoords[1] + 1 + neighborDirection[1]]) {
                                actualNeighborValence++;
                            }
                        }

                        if (actualNeighborValence == 4) {
                            // Neighbor box also has new valence 4, so we will need to perform an update ourselves
                            // Mark boxes and remove chain
                            markAndRemoveChain(chain, null);
                            if (this.recordUndo) {
                                pushSubTransaction(new SubTransaction() {
                                    @Override
                                    public void undo() {
                                        markAndAddChain(chain);
                                    }
                                });
                            }
                            this.chainSplit = true;
                        }

                    }

                }

                break;

            default:
                // New valence < 2: do nothing, box wasn't and will not be in a chain
                break;
        }

    }

    protected boolean onBoard(int x, int y) {
        // Checks if the given box coordinates are on the board
        return x >= 0 && x < this.columns && y >= 0 && y < this.rows;
    }

    protected static boolean boxesAdjacent(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1;
    }

    protected boolean boxesConnected(int x1, int y1, int x2, int y2) {
        // Calculates whether or not these boxes don't have a line in between them
        // Should only be called for adjacent boxes
        // Because of the way the edge coordinate system works, the edge in between neighboring boxes is simply the average of the box coordinates converted to the edge coordinate system
        return !this.edges[x1 + x2 + 1][y1 + y2 + 1];
    }

    protected boolean boxesAdjacentAndConnected(int x1, int y1, int x2, int y2) {
        // Assumes coordinates are actual box coordinates on this board, but don't need to be adjacent
        return Board.boxesAdjacent(x1, y1, x2, y2) && boxesConnected(x1, y1, x2, y2);
    }

    protected static void prependChain(Chain main, Chain add, boolean reverse) {
        // reverse indicates order in which boxes are prepended, not order in which they will end up in the main chain
        if (reverse) {
            // Add in reverse order
            for(int i = add.size - 1; i >= 0; i--) {
                main.prepend(add.boxes.get(i));
            }
        } else {
            // Add in same order
            for(int i = 0; i < add.size; i++) {
                main.prepend(add.boxes.get(i));
            }
        }
    }

    protected static void appendChain(Chain main, Chain add, boolean reverse) {
        if (reverse) {
            // Add in reverse order
            for(int i = add.size - 1; i >= 0; i--) {
                main.append(add.boxes.get(i));
            }
        } else {
            // Add in same order
            for(int i = 0; i < add.size; i++) {
                main.append(add.boxes.get(i));
            }
        }
    }

    protected void markAndRemoveChain(Chain toBeMarked, Chain marker) {
        // Marks all boxes in toBeMarked as part of marker in the chainAt table and then removes toBeMarked from the set of chains
        markChain(toBeMarked, marker);
        this.chains.remove(toBeMarked);
    }

    protected void markAndAddChain(Chain toBeMarked) {
        // Marks all boxes in toBeMarked as part of toBeMarked in the chainAt table and then adds toBeMarked to the set of chains
        // Mostly used for undoing
        markChain(toBeMarked, toBeMarked);
        this.chains.add(toBeMarked);
    }

    protected void markChain(Chain toBeMarked, Chain marker) {
        // Marks all boxes in toBeMarked as part of marker in the chainAt table and then removes toBeMarked from the set of chains
        // marker can be null, toBeMarked cannot be null
        // Mark correct chain in chainAt table
        for(int i = 0; i < toBeMarked.size; i++) {
            int addedBox = toBeMarked.boxes.get(i);
            int[] addedBoxCoords = intToBox(addedBox);
            this.chainAt[addedBoxCoords[0]][addedBoxCoords[1]] = marker;
        }

    }

    protected int findSplitIndex(Chain chain, int x, int y, int index) {
        // Look for split around box (with coordinates x and y) at index in chain
        // Chain type should still be its old type
        // Should not be used for loops
        // split index meaning: index + 0 indicates split happened in front of box in chain.boxes, index + 1 indicates behind
        // In case of an open chain of size 1, the splitIndex can be 0 or 1 since the order is irrelevant at this point

        // Check for split in middle of chain
        for(int i = 0; i < 2; i++) {
            int testIndex = index + 2*i - 1;
            if (testIndex >= 0 && testIndex < chain.size) {
                int[] neighborCoords = intToBox(chain.boxes.get(testIndex));
                if (!boxesConnected(x, y, neighborCoords[0], neighborCoords[1]))
                    return index + i;
            }
        }

        // Split happened at edges of chain
        if (chain.type == ChainType.HALF_OPEN) {
            // Split must have happened at the end (open side)
            return chain.size;
        } else {
            // Chain must (have been) open, check both sides
            for(int i = 0; i < chain.size; i += chain.size - 1) {
                int[] boxCoords = intToBox(chain.boxes.get(i));
                if (this.valence[boxCoords[0]][boxCoords[1]] == 3) {
                    return (i > 0 ? chain.size : 0);
                }
                if (chain.size == 1)
                    break;
            }
        }

        assert(false);
        return -1;
    }

    protected int boxToInt(int x, int y) {
        // Converts a box to an int ID
        // Used to store boxes as an ArrayList in the Chain class
        return y*this.columns + x;
    }

    protected int[] intToBox(int box) {
        // Converts an int ID to a box
        return new int[] {box%this.columns, box/this.columns};
    }

    // Board interface methods

    @Override
    public double gameResult() {
        if(this.gameDecided()) {
            if(this.scores[0] > this.scores[1]) {
                return 0.0d;
            } else if (this.scores[1] > this.scores[0]) {
                return 1.0d;
            } else {
                return 0.5d;
            }
        } else {
            throw new GameStateNotDecidedException();
        }
    }

    @Override
    public boolean gameDecided() {
        int total_points = this.rows * this.columns;
        int half_points = total_points / 2;
        if(this.scores[0] + this.scores[1] == total_points) return true;
        if(this.scores[0] > half_points || this.scores[1] > half_points) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Set<Move> getMoves() {
        //TODO: We should make this more efficient, not feasible to create new object for every possible move at every node
        HashSet<Move> lm = new HashSet<>();
        for(MoveIterator it = getLegalMoveIterator(); it.hasNext(); ) {
            int[] edgeCoords = it.getNextMove();
            lm.add(new DBMove(edgeCoords[0], edgeCoords[1]));
        }
        return lm;
    }

    @Override
    public MCTS.Board duplicate() {
        return this.deepcopy();
    }

    @Override
    public void playMove(Move move) {
        DBMove dbm = (DBMove) move;
        this.registerMove(dbm.x, dbm.y);
    }

    @Override
    public int getNextTurnPlayer() {
        return this.currentPlayer;
    }

    @Override
    public Move getRandomMove() {
        if(this.hasOptimalMoves()) {
            int[] oms = this.getOptimalMoves();
            int i = new Random().nextInt(oms.length);
            int[] om = this.intToEdge(oms[i]);
            return new DBMove(om[0], om[1]);
        } else {
            return MCTS.Board.super.getRandomMove();
        }
    }

    @Override
    public Set<Move> getOptimal() {
        Set<Move> optimalMoves = new HashSet<>();
        if(this.hasOptimalMoves()) {
            int[] oms = this.getOptimalMoves();
            for(int i = 0; i < oms.length; i++) {
                int[] om = intToEdge(oms[i]);
                optimalMoves.add(new DBMove(om[0], om[1]));
            }
        }
        return optimalMoves;
    }

}
