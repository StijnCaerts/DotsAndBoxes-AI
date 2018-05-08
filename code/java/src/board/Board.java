package board;

import java.util.ArrayList;
import java.util.HashMap;

public class Board {

    public int columns, rows;
    public int currentPlayer = 1;

    // Board representation
    public boolean[][] edges; // false means no line has been drawn yet
    public int[][] valence; // Amount of lines next to box, starts at 0
    public Chain[][] chainAt; // Stores the chain each box belongs to, null for boxes with valence < 3
    public ArrayList<Chain> chains = new ArrayList<>();

    // ANN input
    int[] scores = new int[2];
    HashMap<ChainType, int[]> chainSizes = new HashMap<>();

    public Board(int columns, int rows) {

        this.columns = columns;
        this.rows = rows;

        // Board representation initialization
        this.edges = new boolean[2*columns + 1][2*rows + 1];
        this.valence = new int[columns][rows];
        this.chainAt = new Chain[columns][rows];

    }

    public void registerMove(int x, int y) {
        // x, y are in the edge coordinate system (so in a grid of size (2*rows + 1)x(2*columns + 1))
    }
}
