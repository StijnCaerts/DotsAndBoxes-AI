
def alpha_beta(node, alpha, beta):
	
	# Based on https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Pseudocode
	# node needs to support three operations: isTerminal(), value(), getChildren(), maximizingPlayer()
	
	if node.isTerminal()
		return node.value()
	
	if node.maximizingPlayer():
		
		v = float("-inf")
		for child in node.getChildren():
			
			v = max(v, alpha_beta(child, alpha, beta))
			alpha = max(alpha, v)
			if beta <= alpha:
				break
		
	else:
		
		v = float("inf")
		for child in node.getChildren():
			
			v = min(v, alpha_beta(child, alpha, beta))
			beta = min(beta, v)
			if beta <= alpha:
				break
	
	return v
