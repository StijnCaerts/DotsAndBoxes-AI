package board;

import java.util.ArrayList;

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
    public ArrayList<Chain> chains;

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

    }

    public void registerMove(int x, int y) {

        // x, y are in the edge coordinate system (so in a grid of size (2*columns + 1)x(2*rows + 1))

        // Update edge matrix
        this.edges[x][y] = true;

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
                    int ny = x + neighborDirection[1];
                    if (boxesConnected(x, y, nx, ny) && this.chainAt[nx][ny] != null) {
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
                        if (chain.boxes.get(0) == neighborBox) {
                            chain.prepend(box);
                        } else {
                            chain.append(box);
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
                                // Mark correct chain in chainAt table
                                for(int i = 0; i < chain2.size; i++) {
                                    int addedBox = chain2.boxes.get(i);
                                    int[] addedBoxCoords = intToBox(addedBox);
                                    this.chainAt[addedBoxCoords[0]][addedBoxCoords[1]] = chain1;
                                }
                                this. chains.remove(chain2);

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
                                this.chains.remove(chain2);

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
                switch(chain.type) {
                    case OPEN:
                        break;
                    case LOOP:

                        // Marked edge must be inside of loop, so loop becomes closed chain
                        // This case can only occur if this box is the first to be updated
                        // Chain size stays the same, boxes may need to be re-ordered

                        // Find neighboring connected box
                        int neighborBox = -1;
                        x2 = -1;
                        y2 = -1;
                        for(int[] neighborDirection : Board.neighborDirections) {
                            // Iterate through neighboring boxes
                            int nx = x + neighborDirection[0];
                            int ny = x + neighborDirection[1];
                            if (boxesConnected(x, y, nx, ny)) {
                                x2 = nx;
                                y2 = ny;
                                neighborBox = boxToInt(x2, y2);
                                break;
                            }
                        }

                        int index = chain.boxes.indexOf(box); // Own position in chain
                        ArrayList<Integer> newBoxes = new ArrayList<>();
                        newBoxes.add(box);
                        if (chain.boxes.get((index + 1)%chain.size) == neighborBox) {
                            // Same ordering
                            for(int i = (index + 1)%chain.size; i != index; i = ((i + 1)%chain.size)) {
                                newBoxes.add(chain.boxes.get(i));
                            }
                        } else {
                            // Reverse ordering
                            for(int i = (index - 1)%chain.size; i != index; i = ((i - 1)%chain.size)) {
                                newBoxes.add(chain.boxes.get(i));
                            }
                        }
                        chain.boxes = newBoxes;

                        chain.type = ChainType.CLOSED;
                        this.chainSplit = true;

                        break;
                    case HALF_OPEN:
                        break;
                    case CLOSED:

                        if (!this.chainSplit) {
                            // There is a split within a closed chain

                            // Check if split is at beginning or end of chain

                            //TODO: Continue here

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
                break;
            default:
                // New valence < 2: do nothing, box wasn't and will not be in a chain
                break;
        }

    }

    public boolean boxesConnected(int x1, int y1, int x2, int y2) {
        // Calculates whether or not these boxes don't have a line in between them
        // Should only be called for neighboring boxes
        // Because of the way the edge coordinate system works, the edge in between neighboring boxes is simply the average of the box coordinates converted to the edge coordinate system
        return !this.edges[x1 + x2 + 1][y1 + y2 + 1];
    }

    public static void prependChain(Chain main, Chain add, boolean reverse) {
        // reverse indicates order in which boxes are prepended, not order in which they will end up in the main chain
        if (reverse) {
            // Add in reverse order
            for(int i = add.size - 1; i < add.size; i++) {
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
            for(int i = add.size - 1; i < add.size; i++) {
                main.append(add.boxes.get(i));
            }
        } else {
            // Add in same order
            for(int i = 0; i < add.size; i++) {
                main.append(add.boxes.get(i));
            }
        }
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

}
