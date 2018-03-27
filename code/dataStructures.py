class DisjointSet:
    def __init__(self, nbElements):
        self.parent = []
        self.rank = []
        for i in range(0, nbElements):
            self.parent[i] = i
            self.rank[i] = 0

    def find(self, x):
        if self.parent[x] != x:
            self.parent[x] = self.find(self.parent[x])
        return self.parent[x]

    def union(self, x, y):
        xRoot = self.find(x)
        yRoot = self.find(y)

        # x and y already belong to the same set
        if xRoot == yRoot:
            return

        # merge the set of x and y
        if self.rank[xRoot] < self.rank[yRoot]:
            self.parent[xRoot] = yRoot
        elif self.rank[xRoot] > self.rank[yRoot]:
            self.parent[yRoot] = xRoot
        else:
            self.parent[xRoot] = yRoot
            self.rank[yRoot] += 1

    def inSameSet(self, x, y):
        return self.find(x) == self.find(y)