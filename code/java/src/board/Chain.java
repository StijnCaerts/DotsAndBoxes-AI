package board;

import java.util.ArrayList;

public class Chain {

    // Stores the boxes in this chain in order
    // For half-open chains: list must start with box of valence 3
    // For loops: any starting point is valid
    public ArrayList<Integer> boxes;

    public int size; // Stores the size of boxes

    public ChainType type;

    public Chain(int box, ChainType type) {
        this.boxes = new ArrayList<>();
        this.boxes.add(box);
        this.size = 1;
        this.type = type;
    }

    public Chain(ArrayList<Integer> boxes, ChainType type) {
        // boxes is not copied, caller should take care of reference semantics
        this.boxes = boxes;
        this.size = boxes.size();
        this.type = type;
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
