from GameState import GameState

class Board(GameState):
	
	# Interface methods
	
	def __init__(self, nb_rows, nb_cols, player):
		
		# Basic initialization
		self.nb_rows = nb_rows
		self.nb_cols = nb_cols
		self.player = player # 1 or 2
		self.scores = [0, 0, 0]
		self.max_score = nb_rows*nb_cols
		
		# Construct edges and nodes matrix
		self.edges = [] # Boolean, true means uncut
		self.nodes = [] # Represents valence of nodes from strings-and-coins
		for x in range(self.nb_cols):
			self.edges.append([True]*self.nb_rows)
			self.edges.append([True]*(self.nb_rows + 1))
			self.nodes.append([4]*self.nb_rows)
		self.edges.append([True]*self.nb_rows)
		
		# Construct all possible moves
		self.moves_left = set() # Moves are represented as ints
		for x in range(len(self.edges)):
			for y in range(len(self.edges[x])):
				self.moves_left.add(self.coords_to_edge(x, y))
		
		# TODO: chain updating
		# Initialize chains
		# Chains are represented as lists of nodes
		self.closed_chains = [] # Chains which start and end in nodes of valence

	@property
	def game_result(self):
		
		own_score = self.scores[self.player]
		opponent_score = self.scores[self.player % 2 + 1]
		if len(self.moves_left) == 0:
			diff = own_score - opponent_score
			if diff > 0:
				return 1
			elif diff < 0:
				return 0
			else:
				return 0.5
		else:
			# Check if one player already has at least half of all points
			if own_score > self.max_score//2:
				return 1
			elif opponent_score > self.max_score//2:
				return 0
			else:
				return None

	def get_moves(self):
		return self.moves_left

	def get_random_move(self):
		moves = self.get_moves()
		return choice(tuple(moves)) if moves != set() else None

	def play_move(self, move):
		x, y = self.edge_to_coords(move)
		self.edges[x][y] = False
		self.moves_left.remove(move)
		
		# Update valence
		if x%2 == 0:
			# Horizontal edge, decrease valence of left and right nodes
			for node_x in x//2 - 1, x//2:
				if node_x >= 0 and node_x < nb_cols:
					self.nodes[node_x][y] -= 1
		else:
			# Vertical edge, decrease valence of top and bottom nodes
			for node_y in y - 1, y:
				if node_y >= 0 and node_y < nb_rows:
					self.nodes[x//2][node_y] -= 1
		
		# TODO: chain updating
	
	# Own methods
	
	def undo_move(self, move):
		x, y = self.edge_to_coords(move)
		self.edges[x][y] = True
		self.moves_left.add(move)
		
		# Update valence
		if x%2 == 0:
			# Horizontal edge, decrease valence of left and right nodes
			for node_x in x//2 - 1, x//2:
				if node_x >= 0 and node_x < nb_cols:
					self.nodes[node_x][y] += 1
		else:
			# Vertical edge, decrease valence of top and bottom nodes
			for node_y in y - 1, y:
				if node_y >= 0 and node_y < nb_rows:
					self.nodes[x//2][node_y] += 1
		
		# TODO: chain updating
	
	def coords_to_edge(self, x, y):
		return x*(self.nb_cols + 1) + y
	
	def edge_to_coords(self, move):
		return move//(self.nb_cols + 1), move%(self.nb_cols + 1)
	
