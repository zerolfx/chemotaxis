package chemotaxis.g8;

import chemotaxis.sim.ChemicalCell;
import chemotaxis.sim.ChemicalCell.ChemicalType;
import chemotaxis.sim.ChemicalPlacement;
import chemotaxis.sim.SimPrinter;

import java.awt.*;
import java.util.*;

public class Controller extends chemotaxis.sim.Controller {
	static int[][] DIR = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}}; // down right up left   +1: turn left; -1: turn right
	int size;
	Map<Integer, Status> solution;
	ChemicalCell[][] G;


	private boolean checkValid(Point p) {
		return p.x >= 1 && p.y >= 1 && p.x <= size && p.y <= size;
	}

	private boolean isBlocked(Point p) {
		return !checkValid(p) || G[p.x - 1][p.y - 1].isBlocked();
	}

	private Point moveByDT(Point p, int dt) {
		return new Point(p.x + DIR[dt][0], p.y + DIR[dt][1]);
	}

	private ArrayList<Point> generatePath(Point start, int mode, int dt) {
		boolean[][] vis = new boolean[size + 1][size + 1];
		ArrayList<Point> res = new ArrayList<>();
		int turn = 0;
		if (mode >= 1 && mode <= 4) { // turn left right l&r r&l
			Point cur = moveByDT(start, dt);
			if (isBlocked(cur)) return null;
			while (true) {
				res.add(cur);
				turn += 1;
				Point nxt = moveByDT(cur, dt);
				while (isBlocked(nxt)) {
					if (mode == 1) {
						dt = (dt + 1) % 4;
					} else if (mode == 2) {
						dt = (dt + 3) % 4;
					} else if (mode == 3) {
						dt = (dt + (turn % 2 == 0 ? 1 : 3)) % 4;
					} else {
						dt = (dt + (turn % 2 == 0 ? 3 : 1)) % 4;
					}
					nxt = moveByDT(cur, dt);
				}
				if (vis[nxt.x][nxt.y]) return res;
				vis[nxt.x][nxt.y] = true;
				cur = nxt;
			}
		}
		return null;
	}

	static class Status {
		int time, cost;
		Point from;
		int mode, dt;
		public Status(int cost, int time, Point from) {
			this.cost = cost; this.time = time; this.from = from;
		}

		public Status(Status s) {
			this.cost = s.cost; this.time = s.time; this.from = s.from; this.mode = s.mode; this.dt = s.dt;
		}

		@Override
		public String toString() {
			return "Status{" +
					"time=" + time +
					", cost=" + cost +
					", from=" + from +
					", mode=" + mode +
					", dt=" + dt +
					'}';
		}

		public void setDt(int dt) {
			this.dt = dt;
		}

		public void setMode(int mode) {
			this.mode = mode;
		}

		public int getV() {
			return this.cost * 1000 + this.time;
		}
	}

	private void precompute(Point start, Point target, int timeLimit, int chemLimit, int goal) {
		Status[][] f = new Status[size + 1][size + 1];
		f[start.x][start.y] = new Status(0, 0, null);
		PriorityQueue<Status> pq = new PriorityQueue<>(Comparator.comparing((Status s) -> -s.getV()));
		pq.add(new Status(0, 0, start));

		while (!pq.isEmpty()) {
			Status uu = pq.poll(), u = f[uu.from.x][uu.from.y];
//			System.out.println(uu + "   " + u);
			if (uu.cost != u.cost || uu.time != u.time) continue;
			if (u.equals(target)) break;
			Point cur = uu.from;
//			System.out.println(cur);
			for (int mode = 1; mode <= 4; ++mode) {
				for (int dt = 0; dt < 4; ++dt) {
					ArrayList<Point> path = generatePath(cur, mode, dt);
//					System.out.println(mode + " " + dt + " " + path);
					if (path == null) continue;
					for (int i = 0; i < path.size(); ++i) {
						Point nxt = path.get(i);
						Status s = new Status(u.cost + 1, u.time + i + 1, cur);
						s.mode = mode; s.dt = dt;
						if (f[nxt.x][nxt.y] == null || f[nxt.x][nxt.y].getV() > s.getV()) {
							f[nxt.x][nxt.y] = s;
							Status ts = new Status(s); ts.from = nxt;
							pq.add(ts);
						}
					}
				}
			}
		}
		while (true) {
			Status s = f[target.x][target.y];
			System.out.println("     " + s);
//			Status ss = new Status(s); ss.from = target;
			if (target.equals(start)) break;
			solution.put(f[s.from.x][s.from.y].time, s);
			target = s.from;
		}
	}


	public Controller(Point start, Point target, Integer size, ChemicalCell[][] grid, Integer simTime, Integer budget, Integer seed, SimPrinter simPrinter, Integer agentGoal, Integer spawnFreq) {
		super(start, target, size, grid, simTime, budget, seed, simPrinter, agentGoal, spawnFreq);
		this.size = size;
		this.G = grid;
		this.solution = new HashMap<>();
		precompute(start, target, simTime, budget, agentGoal);
	}


	@Override
	public ChemicalPlacement applyChemicals(Integer currentTurn, Integer chemicalsRemaining, ArrayList<Point> locations, ChemicalCell[][] grid) {
		ChemicalPlacement res = new ChemicalPlacement();
		currentTurn -= 1;
		System.out.println(currentTurn + ": " + solution.getOrDefault(currentTurn, null));
		if (!solution.containsKey(currentTurn)) return res;
		Status s = solution.get(currentTurn);
		res.location = moveByDT(s.from, s.dt);
		if (s.mode == 1) {
			res.chemicals.add(ChemicalType.BLUE);
		} else if (s.mode == 2) {
			res.chemicals.add(ChemicalType.RED);
		} else if (s.mode == 3) {
			res.chemicals.add(ChemicalType.BLUE);
			res.chemicals.add(ChemicalType.GREEN);
		} else if (s.mode == 4) {
			res.chemicals.add(ChemicalType.RED);
			res.chemicals.add(ChemicalType.GREEN);
		}
		return res;
	}
}
