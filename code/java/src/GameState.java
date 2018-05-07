import java.util.HashSet;
import java.util.Set;

public class GameState {

    private int rows, cols;
    private int next_turn_player;
    private int score[] = new int[3];
    private boolean edges[][];

    public GameState(int rows, int cols) {
        this.next_turn_player = 1;
        this.rows = rows;
        this.cols = cols;
        this.edges = new boolean[2*rows + 1][2*cols + 1];
    }

    public double gameResult() {
        if(!this.gameDecided() && !this.getMoves().isEmpty()) {
            if(score[1] > score[2]) {
                return 0;
            } else if (score[2] > score[1]) {
                return 1;
            } else {
                return 0;
            }
        } else {
            throw new GameStateNotDecidedException();
        }
    }

    public boolean gameDecided() {
        int total_points = this.rows * this.cols;
        int half_points = total_points / 2;
        if(score[1] > half_points || score[2] > half_points) {
            return true;
        } else {
            return false;
        }
    }

    public Set<Move> getMoves() {
        Set<Move> moves = new HashSet<>();
        for(int x = 0; x < 2*this.cols + 1; x++) {
            for(int y = (x + 1)%2; y < 2*this.rows + 1; y += 2) {
                if (!this.edges[x][y]) {
                    moves.add(new Move(x,y));
                }
            }
        }
        return moves;
    }

    public void playMove(int x, int y) {
        boolean makes_box = false;

        // check if this move makes a box
        if(x % 2 == 0 && y % 2 == 1) {
            // vertical move
            if(x - 2 >= 0) {
                // check left
                if(this.edges[x-2][y] && this.edges[x-1][y-1] && this.edges[x-1][y+1]) {
                    makes_box = true;
                    this.score[this.next_turn_player]++;
                }
            }
            if(x + 2 <= cols*2) {
                // check right
                if(this.edges[x+2][y] && this.edges[x+1][y-1] && this.edges[x+1][y+1]) {
                    makes_box = true;
                    this.score[this.next_turn_player]++;
                }
            }

        } else if (x % 2 == 1 && y % 2 == 0) {
            // horizontal move
            if(y - 2 >= 0) {
                // check above
                if(this.edges[x][y-2] && this.edges[x-1][y-1] && this.edges[x+1][y-1]) {
                    makes_box = true;
                    this.score[this.next_turn_player]++;
                }
            }
            if(y + 2 <= rows*2) {
                // check below
                if(this.edges[x][y+2] && this.edges[x-1][y+1] && this.edges[x+1][y+1]) {
                    makes_box = true;
                    this.score[this.next_turn_player]++;
                }
            }

        } else {
            throw new IllegalArgumentException();
        }

        this.edges[x][y] = true;

        if(!makes_box) {
            // switch players
            this.next_turn_player = (this.next_turn_player % 2) + 1;
        }
    }
}