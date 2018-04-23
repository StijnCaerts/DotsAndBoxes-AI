from random import choice

class GameState(object):

    def __init__(self):
        self.next_turn_player = 1


    @property
    def game_result(self):
        return None

    def get_moves(self):
        return set()

    def get_random_move(self):
        moves = self.get_moves()
        return choice(tuple(moves)) if moves != set() else None

    def play_move(self, move):
        pass


class DotsAndBoxesState(GameState):
    def __init__(self, nb_rows, nb_cols):
        super(DotsAndBoxesState, self).__init__()

        self.nb_rows = nb_rows
        self.nb_cols = nb_cols
        rows = []
        for ri in range(nb_rows + 1):
            columns = []
            for ci in range(nb_cols + 1):
                columns.append({"v": 0, "h": 0})
            rows.append(columns)
        self.board = rows

        self.score = {1: 0, 2: 0}

    @property
    def game_result(self):
        # check if the board is full, then decide based on score
        free_lines = self.get_moves()
        if len(free_lines) > 0:
            return None
        elif self.score[1] > self.score[2]:
            return 0
        elif self.score[1] < self.score[2]:
            return 1
        else:
            return 0.5

    def get_moves(self):
        free_lines = []
        for ri in range(len(self.board)):
            row = self.board[ri]
            for ci in range(len(row)):
                cell = row[ci]
                if ri < (len(self.board) - 1) and cell["v"] == 0:
                    free_lines.append((ri, ci, "v"))
                if ci < (len(row) - 1) and cell["h"] == 0:
                    free_lines.append((ri, ci, "h"))
        return set(free_lines)

    def play_move(self, move):
        r, c, o = move
        assert self.board[r][c][o] == 0

        # check if this move makes a box
        makes_box = False
        if o == "h":
            if r - 1 >= 0:
                # check above
                if self.board[r-1][c]["h"] != 0 and self.board[r-1][c]["v"] != 0 and self.board[r-1][c+1]["v"] != 0:
                    makes_box = True
                    self.score[self.next_turn_player] += 1
            if r + 1 <= self.nb_rows:
                # check below
                if self.board[r+1][c]["h"] != 0 and self.board[r][c]["v"] != 0 and self.board[r][c+1]["v"] != 0:
                    makes_box = True
                    self.score[self.next_turn_player] += 1

        elif o == "v":
            if c - 1 >= 0:
                # check left
                if self.board[r][c-1]["v"] != 0 and self.board[r][c-1]["h"] != 0 and self.board[r+1][c-1]["h"] != 0:
                    makes_box = True
                    self.score[self.next_turn_player] += 1

            if c + 1 <= self.nb_cols:
                # check right
                if self.board[r][c+1]["v"] != 0 and self.board[r][c]["h"] != 0 and self.board[r+1][c]["h"] != 0:
                    makes_box = True
                    self.score[self.next_turn_player] += 1


        # register move
        self.board[r][c][o] = self.next_turn_player

        if not makes_box:
            # switch turns
            self.next_turn_player = self.next_turn_player % 2 + 1

