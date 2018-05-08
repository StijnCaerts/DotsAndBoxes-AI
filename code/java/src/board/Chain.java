package board;

import java.util.ArrayList;

public class Chain {

    // Loops are considered open
    public ChainType type;

    // Stores the boxes in this chain in order (for half-open chains: list must start with box of valence 3)
    // In case of loops: any starting point is valid
    public ArrayList<Integer> boxes;

    public int size; // Stores the size of boxes

    public Chain(int box, ChainType type) {
        this.type = type;
        this.boxes = new ArrayList<>();
        this.boxes.add(box);
        this.size = 1;
    }

    public void prepend(int box) {
        this.boxes.add(0, box);
        this.size++;
    }

    public void append(int box) {
        this.boxes.add(box);
        this.size++;
    }

}
