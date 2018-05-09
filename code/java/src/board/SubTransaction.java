package board;

public interface SubTransaction {

    // Stores how to reverse box chain update
    void undo();

}
