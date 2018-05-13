from numpy import *
from math import sqrt
from copy import deepcopy
from time import time

class ANN:
	
	"""ANN with one hidden layer, one output and full connections in between consecutive layers.
	Initial weights are chosen from a normal distribution.
	Activation function is tanh."""
	
	INIT_SIGMA = 0.02
	REL_STOP_MARGIN = 0.01
	MAX_ITERATIONS = 1000000
	ACTIVATION = tanh
	D_ACTIVATION = lambda x: 1 - tanh(x)**2 # Derivative of tanh
	VEC_ACTIVATION = vectorize(ACTIVATION)
	VEC_D_ACTIVATION = vectorize(D_ACTIVATION)
	STEP_SIZE = 0.1
	
	def __init__(self, input_size, hidden_size):
		
		#self.input_size = input_size
		#self.hidden_size = hidden_size
		self.hidden_weights = random.normal(0, ANN.INIT_SIGMA, (hidden_size, input_size))
		self.output_weights = random.normal(0, ANN.INIT_SIGMA, hidden_size)
	
	def get_weights(self):
		return self.hidden_weights, self.output_weights
	
	def predict(self, input_vector):
		
		# Predicts the output for this input vector
		# input_vector will be normalized
		
		input_vector = input_vector/linalg.norm(input_vector)
		return ANN.ACTIVATION(dot(self.output_weights, ANN.VEC_ACTIVATION(dot(self.hidden_weights, input_vector))))
	
	@staticmethod
	def frob_norm(a, b):
		
		# Calculates the total Frobenius norm of both matrices A and B
		return sqrt(linalg.norm(a)**2 + linalg.norm(b)**2)
	
	def train(self, examples):
		
		#print("Training")
		start = time()
		
		# examples is a list of (input, output)-tuples
		# input will be normalized
		# We stop when the weights have converged within some relative margin
		
		for example in examples:
			example[0] = example[0]/linalg.norm(example[0])
		
		iteration = 0
		while True:
			
			
			# Store old weights to check for convergence later
			prev_hidden_weights = deepcopy(self.hidden_weights)
			prev_output_weights = deepcopy(self.output_weights)
			
			for k in range(len(examples)):
				
				input_vector, output = examples[k]
				
				# Calculate outputs
				hidden_input = dot(self.hidden_weights, input_vector)
				hidden_output = ANN.VEC_ACTIVATION(hidden_input)
				final_input = dot(self.output_weights, hidden_output)
				predicted_output = ANN.ACTIVATION(final_input)
				
				#print("Output:", output)
				#print("Predicted output:", predicted_output)
				
				# Used in calculations
				prediction_error = output - predicted_output
				output_derivative = ANN.D_ACTIVATION(final_input)
				
				# Adjust output weights and calculate requested hidden change
				requested_hidden_change = prediction_error*output_derivative*self.output_weights
				self.output_weights = self.output_weights + ANN.STEP_SIZE*prediction_error*hidden_output
				
				#print("After adjusting output weights:", ANN.ACTIVATION(dot(self.output_weights, hidden_output)))
				
				# Backpropagate requested hidden change to adjust hidden weights
				self.hidden_weights = self.hidden_weights + ANN.STEP_SIZE*outer(requested_hidden_change*(ANN.VEC_D_ACTIVATION(hidden_input)), input_vector)
				
				#print("After adjusting hidden weights:", ANN.ACTIVATION(dot(self.output_weights, ANN.VEC_ACTIVATION(dot(self.hidden_weights, input_vector)))))
				
				# Check stop criteria
				iteration += 1
				if iteration >= ANN.MAX_ITERATIONS:
					break
			
			# Check stop criteria
			if iteration >= ANN.MAX_ITERATIONS:
				break
			diff = ANN.frob_norm(self.hidden_weights - prev_hidden_weights, self.output_weights - prev_output_weights)
			base = ANN.frob_norm(self.hidden_weights, self.output_weights)
			#if base > 0 and diff/base < ANN.REL_STOP_MARGIN:
			#	break
		
		print(time() - start)
		print("Stopped training after %s iterations."%iteration)

# TESTING

def print_difference(ann1, ann2):
	
	# Prints the differences in weights in between two ANN's with identical topology
	
	hidden_weights1, output_weights1 = ann1.get_weights()
	hidden_weights2, output_weights2 = ann2.get_weights()
	hidden_diff = hidden_weights1 - hidden_weights2
	output_diff = output_weights1 - output_weights2
	
	print(hidden_diff)
	print(output_diff)
	print("Frobenius norms:")
	print("Hidden weights difference:", linalg.norm(hidden_diff))
	print("Output weights difference:", linalg.norm(output_diff))
	print("Both:", ANN.frob_norm(hidden_diff, output_diff))

def RMSE(ann, examples):
	
	total = 0
	for input_vector, output in examples:
		total += (output - ann.predict(input_vector))**2
	return sqrt(total/len(examples))

def generate_examples(amount, input_size, evaluate):
	# evaluate is a function mapping an input vector onto a numerical value
	examples = []
	inputs = random.normal(0, 100, (amount, input_size))
	for i in range(amount):
		input_vector = inputs[i]
		examples.append([input_vector, evaluate(input_vector)])
	return examples

def test():
	
	# Test the ANN by having it model another ANN with identical topology but unknown weights
	
	input_size = 5
	hidden_size = 3
	real = ANN(input_size, hidden_size)
	model = ANN(input_size, hidden_size)
	
	# Generate training data
	training_data = generate_examples(10000, input_size, real.predict)
	validation_data = generate_examples(10000, input_size, real.predict)
	
	# Print initial difference, train, then print new difference
	print("Initial difference:")
	print_difference(real, model)
	print("Initial RMSE (on training data):", RMSE(model, training_data))
	print("Initial RMSE (on validation data):", RMSE(model, validation_data))
	model.train(training_data)
	print("After training:")
	print_difference(real, model)
	print("After training RMSE (on training data):", RMSE(model, training_data))
	print("After training RMSE (on validation data):", RMSE(model, validation_data))

if __name__ == "__main__":
	test()


