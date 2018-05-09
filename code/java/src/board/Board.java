package board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Board {

    public static final int[][] neighborDirections = new int[][] {
            {1, 0},
            {0, -1},
            {-1, 0},
            {0, 1},
    };

    // These don't actually limit chains on the board, just how they are presented to the ANN
    public static final int maxOpenChainSize = 10;
    public static final int maxLoopSize = 10;

    public int columns, rows;
    public int currentPlayer = 1;

    // Temporary variables used during calculations, doesn't store state across multiple moves
    public boolean boxClosed; // Whether or not a box was closed during this move
    public boolean chainSplit; // Whether or not a previous box update already split a shared chain

    // Board representation
    public boolean[][] edges; // false means no line has been drawn yet
    public int[][] valence; // Amount of lines next to box, starts at 0
    public Chain[][] chainAt; // Stores the chain each box belongs to, null for boxes with valence 0, 1, or 4, not null for all boxes with valence 2 or 3
    public HashSet<Chain> chains;
    public HashSet<Integer> movesLeft;

    // ANN input
    // We only keep track of open chain sizes since closed and half-open chains should be played by MCTS first (including half-hearted hand-outs)
    int[] scores = new int[2];

    public Board(int columns, int rows) {

        this.columns = columns;
        this.rows = rows;

        // Board representation initialization
        this.edges = new boolean[2*columns + 1][2*rows + 1];
        this.valence = new int[columns][rows];
        this.chainAt = new Chain[columns][rows];
        this.chains = new HashSet<>();
        this.movesLeft = new HashSet<>();
        for(int x = 0; x < 2*columns + 1; x++) {
            for(int y = (x + 1)%2; y < 2*rows + 1; y += 2) {
                this.movesLeft.add(edgeToInt(x, y));
            }
        }

    }

    public Board deepcopy() {

        // Creates a deep copy of the important data of this board (including the board representation, but not variables used for temporary calculations)

        // Copy rows, columns, current player and scores
        Board newBoard = new Board(this.columns, this.rows);
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

        // Copy moves left
        newBoard.movesLeft = new HashSet<>(this.movesLeft);

        return newBoard;

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

    public void registerMove(int x, int y) {

        // x, y are in the edge coordinate system (so in a grid of size (2*columns + 1)x(2*rows + 1))

        // Update edge matrix
        this.edges[x][y] = true;

        // Update possible moves
        this.movesLeft.remove(edgeToInt(x, y));

        // Update valence matrix
        this.boxClosed = false;
        this.chainSplit = false;
        if (x%2 == 0) {
            // Vertical edge, increase valence of left and right boxes
            if (x/2 - 1 >= 0) {
                increaseValenceAndUpdate(x/2 - 1, y/2);
            }
            if (x/2 < this.columns) {
                increaseValenceAndUpdate(x/2, y/2);
            }
        } else {
            // Horizontal edge, increase valence of top and bottom boxes
            if (y/2 - 1 >= 0) {
                increaseValenceAndUpdate(x/2, y/2 - 1);
            }
            if (y/2 < this.rows) {
                increaseValenceAndUpdate(x/2, y/2);
            }
        }

        // If during updating no boxes were closed, switch players
        if (!this.boxClosed)
            this.currentPlayer = this.currentPlayer%2 + 1;

    }

    public void increaseValenceAndUpdate(int x, int y) {

        // x and y are in the box coordinate system (so in a grid of size columns x rows)

        // Increase valence
        this.valence[x][y]++;

        // Check if a box is made
        if (this.valence[x][y] == 4) {
            this.boxClosed = true;
            this.scores[this.currentPlayer - 1]++;
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
                        break;
                    case 1:
                        // Box merges with end of existing chain, chain keeps type (open or half-open)
                        chain = this.chainAt[x2][y2];
                        this.chainAt[x][y] = chain;
                        int neighborBox = boxToInt(x2, y2);
                        if (chain.size == 1 || chain.boxes.get(0) != neighborBox) {
                            chain.append(box);
                        } else {
                            chain.prepend(box);
                        }
                        break;
                    case 2:
                        // Both chains become one (any type) and this box is put in the middle of them
                        Chain chain1 = this.chainAt[x2][y2];
                        Chain chain2 = this.chainAt[x3][y3];
                        if (chain1 == chain2) {
                            // Chains on both sides are the same, so it becomes a loop
                            // Box can be added on either side
                            chain1.type = ChainType.LOOP;
                            this.chainAt[x][y] = chain1;
                            chain1.append(box);
                        } else {

                            // Chains on both sides are different, so it doesn't become a loop

                            if (chain1.type == ChainType.HALF_OPEN || chain2.type == ChainType.HALF_OPEN) {

                                // At least one of the chains is half-open
                                // Keep this chain and append the other one to it

                                // Check if one of the chains is half-open, we will keep this one first
                                if (chain1.type != ChainType.HALF_OPEN && chain2.type == ChainType.HALF_OPEN) {
                                    Chain temp = chain1;
                                    chain1 = chain2;
                                    chain2 = temp;
                                }
                                this.chainAt[x][y] = chain1;
                                chain1.append(box);

                                // Check which side of chain connects to new box
                                int[] chain2Start = intToBox(chain2.boxes.get(0));
                                Board.appendChain(chain1, chain2, !((chain2Start[0] == x2 && chain2Start[1] == y2) || (chain2Start[0] == x3 && chain2Start[1] == y3)));
                                markAndRemoveChain(chain2, chain1);

                                // Update chain 1 type
                                if (chain2.type == ChainType.HALF_OPEN) {
                                    chain1.type = ChainType.CLOSED;
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
                                } else {
                                    chain1.append(box);
                                    int[] chain2Start = intToBox(chain2.boxes.get(0));
                                    Board.appendChain(chain1, chain2, !(chain2Start[0] == x3 && chain2Start[1] == y3));
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
                        } else if (splitIndex == chain.size) {
                            // Split at end, reverse box order and change chain type
                            Collections.reverse(chain.boxes);
                            chain.type = ChainType.HALF_OPEN;
                        } else {
                            if (!this.chainSplit) {

                                // Split in middle, split into two half-open chains
                                // We keep first part in old chain and reverse it, while copying the second part to a new chain

                                Chain newChain = new Chain(new ArrayList<>(chain.boxes.subList(splitIndex, chain.size)), ChainType.HALF_OPEN); // Copy second part to new chain
                                markChain(newChain, newChain); // Mark boxes in second part as part of new chain
                                this.chains.add(newChain);
                                chain.boxes.subList(splitIndex, chain.size).clear(); // Remove second part from old chain
                                Collections.reverse(chain.boxes); // Fix order in old chain
                                chain.size = splitIndex; // Update old chain's size
                                chain.type = ChainType.HALF_OPEN; // Fix old chain's type

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
                        x2 = -1;
                        y2 = -1;
                        for(int[] neighborDirection : Board.neighborDirections) {
                            // Iterate through neighboring boxes
                            int nx = x + neighborDirection[0];
                            int ny = y + neighborDirection[1];
                            if (onBoard(nx, ny) && boxesConnected(x, y, nx, ny)) {
                                x2 = nx;
                                y2 = ny;
                                neighborBox = boxToInt(x2, y2);
                                break;
                            }
                        }

                        // Rotate boxes to make sure the list starts and ends with the right boxes
                        if (chain.boxes.get((index + 1)%chain.size) == neighborBox) {
                            // Same ordering
                            // Rotate list so that box is in front
                            Collections.rotate(chain.boxes, -index);
                        } else {
                            // Reverse ordering
                            // Rotate list so that box is at the back
                            Collections.rotate(chain.boxes, chain.size - 1 - index);
                        }

                        chain.type = ChainType.CLOSED;
                        this.chainSplit = true;

                        break;
                    case HALF_OPEN:

                        splitIndex = findSplitIndex(chain, x, y, index);
                        if (!this.chainSplit) {

                            if (splitIndex == chain.size) {
                                // Split at end, just change chain type
                                chain.type = ChainType.CLOSED;
                            } else if (splitIndex == 1) {

                                // Split one box from start
                                // We keep second part in old chain and remove first part
                                int[] removedBoxCoords = intToBox(chain.boxes.get(0));
                                this.chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = null;
                                chain.removeIndex(0);

                            } else {

                                // Split in middle, split into a closed and half-open chain
                                // We keep first part in old chain, while copying the second part to a new chain

                                Chain newChain = new Chain(new ArrayList<>(chain.boxes.subList(splitIndex, chain.size)), ChainType.HALF_OPEN); // Copy second part to new chain
                                markChain(newChain, newChain); // Mark boxes in second part as part of new chain
                                this.chains.add(newChain);
                                chain.boxes.subList(splitIndex, chain.size).clear(); // Remove second part from old chain
                                chain.size = splitIndex; // Update old chain's size
                                chain.type = ChainType.CLOSED; // Fix old chain's type

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
                                if (chain.size == 2) {
                                    // Remove full chain
                                    markAndRemoveChain(chain, null);
                                } else {
                                    // Remove first box from chain
                                    int[] removedBoxCoords = intToBox(chain.boxes.get(0));
                                    this.chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = null;
                                    chain.removeIndex(0);
                                }
                            } else if (splitIndex == chain.size - 1) {
                                // Split at end but not start of chain
                                // Remove last box from chain
                                int[] removedBoxCoords = intToBox(chain.boxes.get(chain.size - 1));
                                this.chainAt[removedBoxCoords[0]][removedBoxCoords[1]] = null;
                                chain.removeIndex(chain.size - 1);
                            } else {
                                // Split somewhere in the middle
                                // We keep first part in the old chain and create a new chain for the second part
                                Chain newChain = new Chain(new ArrayList<>(chain.boxes.subList(splitIndex, chain.size)), ChainType.CLOSED); // Copy second part to new chain
                                markChain(newChain, newChain); // Mark boxes in second part as part of new chain
                                this.chains.add(newChain);
                                chain.boxes.subList(splitIndex, chain.size).clear(); // Remove second part from old chain
                                chain.size = splitIndex; // Update old chain's size
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

    public boolean onBoard(int x, int y) {
        // Checks if the given box coordinates are on the board
        return x >= 0 && x < this.columns && y >= 0 && y < this.rows;
    }

    public static boolean boxesAdjacent(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1;
    }

    public boolean boxesConnected(int x1, int y1, int x2, int y2) {
        // Calculates whether or not these boxes don't have a line in between them
        // Should only be called for adjacent boxes
        // Because of the way the edge coordinate system works, the edge in between neighboring boxes is simply the average of the box coordinates converted to the edge coordinate system
        return !this.edges[x1 + x2 + 1][y1 + y2 + 1];
    }

    public boolean boxesAdjacentAndConnected(int x1, int y1, int x2, int y2) {
        // Assumes coordinates are actual box coordinates on this board, but don't need to be adjacent
        return Board.boxesAdjacent(x1, y1, x2, y2) && boxesConnected(x1, y1, x2, y2);
    }

    public static void prependChain(Chain main, Chain add, boolean reverse) {
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

    public static void appendChain(Chain main, Chain add, boolean reverse) {
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

    public void markAndRemoveChain(Chain toBeMarked, Chain marker) {
        // Marks all boxes in toBeMarked as part of marker in the chainAt table and then removes toBeMarked from the list of chains
        markChain(toBeMarked, marker);
        this.chains.remove(toBeMarked);
    }

    public void markChain(Chain toBeMarked, Chain marker) {
        // Marks all boxes in toBeMarked as part of marker in the chainAt table and then removes toBeMarked from the list of chains
        // marker can be null, toBeMarked cannot be null
        // Mark correct chain in chainAt table
        for(int i = 0; i < toBeMarked.size; i++) {
            int addedBox = toBeMarked.boxes.get(i);
            int[] addedBoxCoords = intToBox(addedBox);
            this.chainAt[addedBoxCoords[0]][addedBoxCoords[1]] = marker;
        }

    }

    public int findSplitIndex(Chain chain, int x, int y, int index) {
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

    public int boxToInt(int x, int y) {
        // Converts a box to an int ID
        // Used to store boxes as an ArrayList in the Chain class
        return y*this.columns + x;
    }

    public int[] intToBox(int box) {
        // Converts an int ID to a box
        return new int[] {box%this.columns, box/this.columns};
    }

    public int edgeToInt(int x, int y) {
        // Converts a edge to an int ID
        return y*(2*this.columns + 1) + x;
    }

    public int[] intToEdge(int edge) {
        // Converts an int ID to a edge
        return new int[] {edge%(2*this.columns + 1), edge/(2*this.columns + 1)};
    }

}
