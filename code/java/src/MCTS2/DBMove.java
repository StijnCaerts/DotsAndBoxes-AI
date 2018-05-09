package MCTS2;

import java.util.Objects;

public class DBMove implements Move {
    public final int x;
    public final int y;

    public DBMove(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBMove)) return false;
        DBMove other = (DBMove) obj;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y);
    }

    @Override
    public String toString() {
        return "Move: x=" + Integer.toString(this.x) + ", y=" + Integer.toString(this.y);
    }

    @Override
    public int compareTo(Move o) {
        return 0;
    }
}
