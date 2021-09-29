package chemotaxis.g2;

import java.awt.Point;
import java.util.*;
import java.lang.Math;

import chemotaxis.sim.ChemicalPlacement;
import chemotaxis.sim.ChemicalCell;
import chemotaxis.sim.ChemicalCell.ChemicalType;
import chemotaxis.sim.DirectionType;
import chemotaxis.sim.SimPrinter;

public class Controller extends chemotaxis.sim.Controller {

	private List<Point> shortestPath;
	private Set<Point> shortestPathSet;
	private List<Point> corners;
	private Set<Point> blockedLocations;
	private int m;
	private int n;
	private final Point target;
	private String[][] policy;
	private Double[][] values;
	private boolean firstRound;
	private DirectionType[][] finalPolicy;
	private boolean red;
	private boolean blueStrategy;
	private Set<Point> agentsStack;
	//private ChemicalCell[][] grid1;
	//private ChemicalCell[][] grid2;
	//private ChemicalCell[][] grid3;
	//private ChemicalCell[][] grid4;


	/**
	 * Controller constructor
	 *
	 * @param start       start cell coordinates
	 * @param target      target cell coordinates
	 * @param size     	  grid/map size
	 * @param grid        game grid/map
	 * @param simTime     simulation time
	 * @param budget      chemical budget
	 * @param seed        random seed
	 * @param simPrinter  simulation printer
	 *
	 */
	public Controller(Point start, Point target, Integer size, ChemicalCell[][] grid, Integer simTime, Integer budget, Integer seed, SimPrinter simPrinter, Integer agentGoal, Integer spawnFreq) {
		super(start, target, size, grid, simTime, budget, seed, simPrinter, agentGoal, spawnFreq);
		this.m = grid.length;
		this.n = grid[0].length;
		this.shortestPath = this.getShortestPath(start, target, grid);
		this.shortestPathSet = new HashSet<Point>(this.shortestPath);
		this.corners = this.findCorners(shortestPath);
		this.corners.add(shortestPath.get(0)); // Added the starting point as a corner?
		this.blockedLocations = this.findBlockedLocations(grid);
		// Print map in console
		for (int i = 0; i < this.m; i ++){
			for (int j = 0; j < this.n; j ++){
				if (shortestPathSet.contains(new Point(i + 1, j + 1))){
					if (corners.contains(new Point(i + 1, j  + 1))){
						System.out.print("C" + " ");
					}else if(target.x == i + 1 && target.y == j + 1){
						System.out.print("O" + " ");
					}else {
						System.out.print("S" + " ");
					}
				}
				else if (blockedLocations.contains(new Point(i, j))){
					System.out.print("*" + " ");
				}else{
					System.out.print("." + " ");
				}
			}
			System.out.println();
		}
		this.target = target;
		this.findPolicy(0.8);
		this.firstRound = true;
		this.finalPolicy = convertPolicy(policy);
		this.red = true;
		int errorMargin = size;
		int sum = this.corners.size() * agentGoal + errorMargin;
		this.blueStrategy = (this.corners.size() * agentGoal + errorMargin < budget);
		this.agentsStack = new HashSet<Point>();
	}

	private DirectionType[][] convertPolicy (String[][] policy) {
		DirectionType[][] output = new DirectionType [policy[0].length][policy.length];
		for (int i=0; i < policy[0].length; i++) {
			for (int j=0; j < policy.length; j++) {
				switch (policy[i][j]) {
					case ">":
						output[i][j] = DirectionType.EAST;
						break;
					case "<":
						output[i][j] = DirectionType.WEST;
						break;
					case "^":
						output[i][j] = DirectionType.NORTH;
						break;
					case "v":
						output[i][j] = DirectionType.SOUTH;
						break;
					default:
						output[i][j] = DirectionType.CURRENT;

				}
			}
		}
		return output;
	}


	private void findPolicy(double gamma){
		String[][] init = this.extractPolicy(new double[this.m][this.n], gamma);
		System.out.println("Init");
		for (int i = 0; i < this.m; i ++){
			for (int j = 0; j < this.n; j ++){
				System.out.print(init[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println("New Policy");

		this.policy = policyIteration(init, new double[this.m][this.n], gamma);
		for (int i = 0; i < this.m; i ++){
			for (int j = 0; j < this.n; j ++){
				System.out.print(policy[i][j] + " ");
			}
			System.out.println();
		}
	}

	public Point closestToTarget(Set<Point> locations) {
		int closestDistance = 9999999;
		Point closest = new Point(0,0);
		for(Point p : locations) {
			int x = p.x;
			int y = p.y;
			int distance = Math.abs(x - this.target.x) + Math.abs(y - this.target.y);
			if(distance > 0 && distance < closestDistance) {
				closest = p;
				closestDistance = distance;
			}
		}
		return closest;
	}

	/**
	 *  A private class that represents a grid's coordinate and its predecessor. Used in BFS implementation
	 */
	private class Node {
		private int x, y;
		private Node prev;

		public Node() {this(0, 0); }

		public Node(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public Node getPrev(){
			return this.prev;
		}

		public int getX(){
			return this.x;
		}
		public int getY(){
			return this.y;
		}

		public void setPrev(Node prev){
			this.prev = prev;
		}

		@Override
		public String toString() {
			return "Node{" +
					"x=" + x +
					", y=" + y +
					", prev=" + prev +
					'}';
		}
	}

	/**
	 * BFS implementation that searches the shortest path between point p and the target on the grid
	 *
	 * @param p             a point on the grid/map
	 * @param target        the target point on the grid/map
	 * @param grid          game grid/map
	 * @return              a list of points that represent the shortest path between point p and the target
	 */
	private List<Point> getShortestPath(Point p, Point target, ChemicalCell[][] grid) {
		Queue<Node> queue = new LinkedList<Node>();
		boolean[][] visited = new boolean[this.m][this.n];
		Node start = new Node(p.x, p.y);
		queue.add(start);
		List<Point> path = new ArrayList<Point>();
		int targetX = target.x, targetY = target.y;
		while (!queue.isEmpty()) {
			Node curNode = queue.poll();
			if (curNode.getX() == targetX && curNode.getY() == targetY) {
				while (curNode != null) {
					path.add(new Point(curNode.getX(), curNode.getY()));
					curNode = curNode.getPrev();
				}
				Collections.reverse(path);
				break;
			}
			for (Node nei: getNeighbors(curNode, grid, visited)){
				visited[nei.getX() - 1][nei.getY() - 1] = true;
				queue.add(nei);
			}
		}
		return path;
	}

	/**
	 * Gets the neighbor of the current cell
	 *
	 * @param n         current node (a cell in the grid)
	 * @param grid      game grid/map
	 * @param visited   a 2d array indicating whether a grid has been visited during BFS
	 * @return          a list of nodes that represents possible neighbors of the current cell
	 */
	private List<Node> getNeighbors(Node n, ChemicalCell[][] grid, boolean[][] visited){
		List<Node> neighbors = new ArrayList<Node>();
		int x = n.getX() - 1, y = n.getY() - 1;

		// Note: make sure to create new node based on an 1-indexed map
		if (isCellValid(grid, visited, x - 1, y)){
			Node next = new Node(x, y + 1);
			next.setPrev(n);
			neighbors.add(next);
		}
		if (isCellValid(grid, visited, x + 1, y )){
			Node next = new Node(x + 2, y + 1);
			next.setPrev(n);
			neighbors.add(next);
		}
		if (isCellValid(grid, visited, x , y - 1)){
			Node next = new Node(x + 1, y);
			next.setPrev(n);
			neighbors.add(next);
		}
		if (isCellValid(grid, visited, x , y + 1)){
			Node next = new Node(x + 1, y + 2);
			next.setPrev(n);
			neighbors.add(next);
		}
		return neighbors;
	}

	/**
	 *
	 * @param grid      game grid/map
	 * @param visited   a 2d array indicating whether a grid has been visited during BFS
	 * @param x         the x coordinate of a node (a cell on the grid)
	 * @param y         the y coordinate of a node (a cell on the grid)
	 * @return          whether the current cell is a valid cell that the agent can move to
	 */
	private boolean isCellValid(ChemicalCell grid[][], boolean visited[][], int x, int y) {
		return (x >= 0) && (x < this.m) && (y >= 0) && (y < this.n) && grid[x][y].isOpen() && !visited[x][y];
	}

	private List<Point> findCorners(List<Point> shortestPath){

		List<Point> corners = new ArrayList<Point>();
		if (shortestPath.size() <= 2){
			return corners;
		}
		Point a = shortestPath.get(0), b = shortestPath.get(1);
		for(int i = 2; i < shortestPath.size() ; i++){
			Point c = shortestPath.get(i);
			int aX = a.x, aY = a.y, cX = c.x, cY = c.y;
			if (aX - cX != 0 && aY - cY != 0){
				corners.add(b);
			}
			a = b;
			b = c;
		}
		System.out.println(corners);
		System.out.println(corners.size());
		return corners;
	}

	private List<String> actions(int x, int y){
		ArrayList<String> res = new ArrayList<String>();
		if(x == target.x - 1 && y == target.y - 1){ // Target point is 1-index
			res.add("G");
		}else if(this.blockedLocations.contains(new Point(x, y))){ //Block locations are 0-indexed
			res.add("X");
		}else{
			res = new ArrayList<String>(Arrays.asList("<", ">", "^", "v", "C"));
		}
		return res;
//		return new ArrayList<DirectionType>(Arrays.asList(DirectionType.EAST, DirectionType.NORTH, DirectionType.SOUTH, DirectionType.WEST, DirectionType.CURRENT));
	}

	/**
	 * Compute the Q-value for the given state-action pair,
	 * given a set of values for the problem and discount factor gamma.
	 * Living reward is -2
	 *
	 * @param values	The current state values
	 * @param gamma		The discount factor
	 * @return			a value for state (x, y)
	 */
	private double Qvalue(int x, int y, String action, double[][] values, double gamma){
		// Multiply values by the discount factor gamma
		double[][] gValues = this.matrixMul(values, gamma);
		int livingReward = -10;
		//Handle obstacle, goal, and optimal path states
		if (action.equals("G")) return 100;
		if (action.equals("X")) return -100;
//		if (this.shortestPathSet.contains(new Point(x + 1, y + 1))) return 30; // shortestPathSet is 1-indexed

		// All possible successor states
		int westX = x, westY = Math.max(y - 1, 0);
		int eastX = x, eastY = Math.min(y + 1, values[0].length - 1);
		int northX = Math.max(x - 1, 0), northY = y;
		int southX = Math.min(x + 1, values.length - 1), southY = y;

		if (action.equals("<")){
			return gValues[westX][westY] + livingReward;
		}
		if (action.equals(">")) {
			return gValues[eastX][eastY] + livingReward;
		}
		if (action.equals("^")){
			return gValues[northX][northY] + livingReward;
		}
		if (action.equals("v")){
			return gValues[southX][southY] + livingReward;
		}
		return gValues[x][y] + livingReward;
	}

	private double[][] matrixMul(double[][] values, double num){
		double[][] res = new double[values.length][values[0].length];
		for (int i = 0; i < values.length; i ++){
			for (int j = 0; j < values[0].length; j ++){
				res[i][j] = values[i][j] * num;
			}
		}
		return res;
	}

	private double[][] matrixAdd(double[][] m1, double[][] m2) throws Exception{

		if (m1.length != m2.length && m1[0].length != m2[0].length) {
			throw new Exception("matrix dimensions do not match");
		}
		double[][] res = new double[m1.length][m1[0].length];
		for (int i = 0; i < m1.length; i ++){
			for (int j = 0; j < m1[0].length; j ++){
				res[i][j] = m1[i][j] + m2[i][j];
			}
		}
		return res;
	}

	/**
	 * Given the current state values, extract the best policy for each state
	 *
	 * @param values 	current state values
	 * @param gamma		discount factor
	 * @return			2d array of string containing new policy
	 */
	private String[][] extractPolicy(double[][] values, double gamma){

		String[][] policy = new String[m][n];
		for (int i = 0; i < this.m; i ++){
			for (int j = 0; j < this.n; j++){
				double bestValue = -99999999.9;
				for (String action: this.actions(i, j)){
					double newValue = this.Qvalue(i, j, action, values, gamma);
					if (newValue > bestValue) {
						bestValue = newValue;
						policy[i][j] = action;
					}
				}
			}
		}
		return policy;
	}

	/**
	 *  Given the current policy and values, evaluate the policy
	 * @param policy 		current poliy
	 * @param values		current state values
	 * @param gamma			discount  factor
	 * @param threshold		threshold for value convergence
	 * @return				new values after doing the new policy
	 */
	private double[][] evaluatePolicy(String[][] policy, double[][]values, double gamma, double threshold){
		double maxDiff = threshold + 1.0;
		while (maxDiff >= threshold){
			maxDiff = -999999.9;
			double[][] newValues = new double[m][n];
			for (int i = 0; i < this.m; i ++){
				for (int j = 0; j < this.n; j++) {
					newValues[i][j] = this.Qvalue(i, j, policy[i][j], values, gamma);
					maxDiff = Math.max(maxDiff, Math.abs(newValues[i][j] - values[i][j]));
					}
			}
			values = newValues;
		}
		return values;
	}

	private String[][] policyIteration(String[][] policy, double[][] values, double gamma){
		while (true){
//			for (int i = 0; i < this.m; i ++){
//				for (int j = 0; j < this.n; j ++){
//					System.out.print(policy[i][j] + " ");
//				}
//				System.out.println();
//			}
			values = this.evaluatePolicy(policy, values, gamma, 0.00001);
			String[][] newPolicy = this.extractPolicy(values, gamma);

			if (this.comparePolicy(newPolicy, policy)){
				return policy;
			}
			policy = newPolicy;
		}
	}

	private Boolean comparePolicy(String[][] p1, String[][] p2){
		for (int i = 0; i < p1.length; i ++){
			for (int j = 0; j < p1[0].length; j++) {
				if (! p1[i][j].equals(p2[i][j])){
					return false;
				}
			}
		}
		return true;
	}


	/**
	 * Find the coordinates of the obstacle
	 * @param grid 		game grid
	 * @return			a set of obstacles coordinates (0-indexed)
	 */
	private Set<Point> findBlockedLocations(ChemicalCell[][] grid){
		Set<Point> res = new HashSet<Point>();
		for (int i = 0; i < grid.length; i++){
			for (int j = 0; j < grid[0].length; j++){
				if (grid[i][j].isBlocked()){
					res.add(new Point(i, j));
				}
			}
		}
		return res;
	}
	private boolean inPath(Point agent) {
		int pathLength = shortestPath.size();
		int pX = agent.x;
		int pY = agent.y;
		for (int i = 0; i < pathLength; i++){
			int tempX = shortestPath.get(i).x;
			int tempY = shortestPath.get(i).y;
			if (pX == tempX && pY == tempY) {
				return true;
			}
		}
		return false;
	}

	private Point nextCell(Point location) {
		int pathLength = shortestPath.size();
		int pX = location.x;
		int pY = location.y;
		boolean flag = false;
		for (int i =0; i < pathLength; i++) {
			if (flag) {
				return shortestPath.get(i);
			}
			int tempX = shortestPath.get(i).x;
			int tempY = shortestPath.get(i).y;
			if (pX == tempX && pY == tempY) {
				flag = true;
			}
		}
		return new Point(0, 0);
	}

	private int closestPathPoint(Point agent) {
		int closestDistance = 9999999;
		int closestIdx = 0;
		for(int i = 0; i < shortestPath.size(); i++) {
			int x = shortestPath.get(i).x;
			int y = shortestPath.get(i).y;
			int distance = Math.abs(x - agent.x) + Math.abs(y - agent.y);
			if(distance > 0 && distance < closestDistance) {
				closestIdx = i;
				closestDistance = distance;
			}
		}
		return closestIdx;
	}

	private boolean cellOccupied(Point p, List<Point> locations) {
		for (int i=0; i< locations.size(); i++) {
			if ( p.equals(locations.get(i)) ) {
				return true;
			}
		}
		return false;
	}

	private Set<Point> agentsAtCorner (ArrayList<Point> locations) {
		Set<Point> agents = new HashSet<Point>();
		for (Point agent : locations) {
			if (! inPath(agent)) {
				agents.add(agent);
			}
			for (Point corner : this.corners) {
				if (agent.equals(corner)) {
					agents.add(agent);
				}
			}
		}
		return agents;
	}

	private boolean oppositeDirection (DirectionType currentCell, DirectionType nextCell) {
		switch (currentCell) {
			case EAST:
				if (nextCell == DirectionType.WEST) {
					System.out.println("agent is going EAST but policy says to go WEST");
					return true;
				}
				break;
			case WEST:
				if (nextCell == DirectionType.EAST) {
					System.out.println("agent is going WEST but policy says to go EAST");
					return true;
				}
				break;
			case NORTH:
				if (nextCell == DirectionType.SOUTH) {
					System.out.println("agent is going NORTH but policy says to go SOUTH");
					return true;
				}
				break;
			case SOUTH:
				if (nextCell == DirectionType.NORTH) {
					System.out.println("agent is going NORTH but policy says to go SOUTH");
					return true;
				}
				break;
		}
		return false;
	}

	private Point nextLocation (Point location, DirectionType direction) {
		int newX;
		int newY;
		switch (direction) {
			case EAST:
				newX = location.x + 1;
				newY = location.y;
				break;
			case WEST:
				newX = location.x - 1;
				newY = location.y;
				break;
			case NORTH:
				newX = location.x;
				newY = location.y - 1;
				break;
			case SOUTH:
				newX = location.x;
				newY = location.y + 1;
				break;
			default:
				newX = location.x;
				newY = location.y;
		}

		return new Point(newX, newY);
	}


	private boolean validCell (int x, int y, ChemicalCell grid[][]) {
		return (x > 0) && (x <= grid.length) && (y > 0) && (y <= grid[0].length) && grid[x][y].isOpen() ;
	}

	private DirectionType expectedMove (Point p, ChemicalCell[][] grid, boolean red) {
		List<Double> concentrations = new ArrayList<Double>();
		ChemicalType color = ChemicalType.GREEN;
		if (red) {
			color = ChemicalType.RED;
		}

		int x = p.x;
		int y = p.y;
		if (validCell(x - 1, y, grid)){
			concentrations.add(grid[x-1][y].getConcentration(color));
		} else {
			concentrations.add(0.0);
		}
		if (validCell(x + 1, y, grid)){
			concentrations.add(grid[x+1][y].getConcentration(color));
		} else {
			concentrations.add(0.0);
		}
		if (validCell(x , y - 1, grid)){
			concentrations.add(grid[x][y-1].getConcentration(color));
		} else {
			concentrations.add(0.0);
		}
		if (validCell(x , y + 1, grid)){
			concentrations.add(grid[x][y+1].getConcentration(color));
		} else {
			concentrations.add(0.0);
		}
		for (int k=0; k < 4; k++){
			System.out.println(concentrations.get(k));
		}
		List<Double> max = new ArrayList<>();
		max.add(0.0);
		int id = 5;
		for (int i=0; i< 4; i++) {
			if (concentrations.get(i) > max.get(0)) {
				for (int j=0; j< max.size(); j++) {
					max.remove(j);
				}
				max.add(concentrations.get(i));
				id = i;
			} else if (concentrations.get(i) == max.get(0)) {
				max.add(concentrations.get(i));
				id = 4;
			}
		}
		System.out.println(id);
		switch(id) {
			case 0:
				System.out.println("GOING SOUTH");
				return DirectionType.SOUTH;
			case 1:
				System.out.println("GOING NORTH");
				return DirectionType.NORTH;
			case 2:
				System.out.println("GOING EAST");
				return DirectionType.EAST;
			case 3:
				System.out.println("GOING WEST");
				return DirectionType.WEST;
		}
		System.out.println("STAYING PUT");
		return DirectionType.CURRENT;
	}


	/**
	 * Apply chemicals to the map
	 *
	 * @param currentTurn         current turn in the simulation
	 * @param chemicalsRemaining  number of chemicals remaining
	 * @param locations     current locations of the agents
	 * @param grid                game grid/map
	 * @return                    a cell location and list of chemicals to apply
	 *
	 */
	@Override
	public ChemicalPlacement applyChemicals(Integer currentTurn, Integer chemicalsRemaining, ArrayList<Point> locations, ChemicalCell[][] grid) {

		ChemicalPlacement chemicalPlacement = new ChemicalPlacement();
		int newX;
		int newY;
		int step = 5;

		//if (currentTurn > shortestPath.size()+2) {
		//	this.firstRound = false;
		//}

		if (this.blueStrategy) {
			//this.agentsStack.addAll(agentsAtCorner(locations));
			this.agentsStack = agentsAtCorner(locations);
			if (agentsStack.size() == 0) {
				return chemicalPlacement;
			} else {
				Point agent = closestToTarget(agentsStack);
				agentsStack.remove(closestToTarget(agentsStack));
				// Point next = nextLocation(agent, this.finalPolicy[agent.x][agent.y]);

				Point next;
				if (inPath(agent)) {
					next = nextCell(agent);
					if (cellOccupied(next, locations)) {
						next = nextCell(next);
					}
				} else {
					int closestPathPointId = closestPathPoint(agent);
					next = this.shortestPath.get(closestPathPointId);
					int xDelta = next.x - agent.x;
					int yDelta = next.y - agent.y;
					if (xDelta < 0) {
						xDelta = -1;
					} else if (xDelta > 0) {
						xDelta = 1;
					}
					if (yDelta < 0) {
						yDelta = -1;
					} else if (yDelta > 0) {
						yDelta = 1;
					}
					newX = agent.x + xDelta;
					newY = agent.y + yDelta;
					while (cellOccupied(new Point(newX, newY), locations)) {
						newX = agent.x + xDelta;
						newY = agent.y + yDelta;
					}
					next = new Point(newX, newY);

				}

				List<ChemicalType> chemicals = new ArrayList<>();
				chemicals.add(ChemicalType.BLUE);

				chemicalPlacement.location = next;
				chemicalPlacement.chemicals = chemicals;

				return chemicalPlacement;
			}
		}
		else {

			if (currentTurn % step == 1 && this.firstRound) {
				int cellId;
				if (currentTurn >= shortestPath.size()) {
					cellId = Math.min((currentTurn / shortestPath.size()) + 3, shortestPath.size() - 1);
				} else {
					cellId = Math.min(currentTurn + 3, shortestPath.size() - 1);
				}
				newX = shortestPath.get(cellId).x;
				newY = shortestPath.get(cellId).y;

				List<ChemicalType> chemicals = new ArrayList<>();
				if (this.red) {
					chemicals.add(ChemicalType.RED);
					this.red = false;
				} else {
					chemicals.add(ChemicalType.GREEN);
					this.red = true;
				}

				chemicalPlacement.location = new Point(newX, newY);
				chemicalPlacement.chemicals = chemicals;

				return chemicalPlacement;

			} else {
				// if all agents will follow the best policy, no need to add chemicals
				for (int i = 0; i < locations.size(); i++) {
					if (oppositeDirection(expectedMove(locations.get(i), grid, true), this.finalPolicy[locations.get(i).x][locations.get(i).y])) {
						agentsStack.add(locations.get(i));
					}
				}
			}

			if (agentsStack.size() != 0) {
				Point agent = closestToTarget(agentsStack);
				agentsStack.remove(closestToTarget(agentsStack));

				DirectionType move = this.finalPolicy[agent.x][agent.y];

				List<ChemicalType> chemicals = new ArrayList<>();
				chemicals.add(ChemicalType.BLUE);

				chemicalPlacement.location = nextLocation(agent, move);
				chemicalPlacement.chemicals = chemicals;

				return chemicalPlacement;

			}
		}

		return chemicalPlacement;

	}
}