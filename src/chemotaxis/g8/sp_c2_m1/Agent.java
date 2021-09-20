package chemotaxis.g8.sp_c2_m1;

import chemotaxis.sim.ChemicalCell;
import chemotaxis.sim.ChemicalCell.ChemicalType;
import chemotaxis.sim.DirectionType;
import chemotaxis.sim.Move;
import chemotaxis.sim.SimPrinter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class Agent extends chemotaxis.sim.Agent {

	/**
	 * Agent constructor
	 *
	 * @param simPrinter  simulation printer
	 *
	 */
	public Agent(SimPrinter simPrinter) {
		super(simPrinter);
	}


	private byte encode(boolean color, int steps, DirectionType dt) {
		return (byte)(steps + (color ? 0b10000000 : 0) + ((4 - dt.ordinal()) << 4));
	}
	private boolean decodeColor(byte x) { return (x & 0b10000000) > 0; }
	private int decodeSteps(byte x) { return x & 0b1111; }
	private DirectionType decodeDirection(byte x) { return DirectionType.values()[4 - ((x >> 4) & 0b111)]; }

	static private DirectionType opposite(DirectionType d) {
		if (d == DirectionType.CURRENT) return DirectionType.CURRENT;
		if (d == DirectionType.EAST) return DirectionType.WEST;
		if (d == DirectionType.NORTH) return DirectionType.SOUTH;
		if (d == DirectionType.WEST) return DirectionType.EAST;
		if (d == DirectionType.SOUTH) return DirectionType.NORTH;
		assert false;
		return null;
	}

	/**
	 * Move agent
	 *
	 * @param randomNum        random number available for agents
	 * @param previousState    byte of previous state
	 * @param currentCell      current cell
	 * @param neighborMap      map of cell's neighbors
	 * @return                 agent move
	 *
	 */
	@Override
	public Move makeMove(Integer randomNum, Byte previousState, ChemicalCell currentCell, Map<DirectionType, ChemicalCell> neighborMap) {
		Move move = new Move();

		boolean color = decodeColor(previousState);
		int steps = decodeSteps(previousState);
		DirectionType lastDirection = decodeDirection(previousState);
		System.out.println("color: " + color + ", steps: " + steps + ", last direction: " + lastDirection);


		double maxValue = -2, minValue = 2;
		DirectionType maxDirection = null, minDirection = null;
		move.directionType = DirectionType.CURRENT;
		for (DirectionType directionType : neighborMap.keySet()) {
			if (opposite(directionType) == lastDirection) continue;
			if (neighborMap.get(directionType).isBlocked()) continue;
			Map<ChemicalType, Double> values = neighborMap.get(directionType).getConcentrations();
			if (values.get(ChemicalType.BLUE) + values.get(ChemicalType.RED) < 0.001) continue;
			double value = values.get(ChemicalType.BLUE) - values.get(ChemicalType.RED);
			if (directionType == lastDirection) value *= 1.2;
			if (color) value = -value;
			System.out.println(directionType + ": " + value);

			if (value > maxValue) { maxValue = value; maxDirection = directionType; }
			if (value < minValue) { minValue = value; minDirection = directionType; }
		}
		System.out.println("max: " + maxValue + ", min: " + minValue);
		if (minValue != maxValue && steps > 2 && (Math.abs(maxValue - minValue) < 0.02 || minValue + maxValue < -0.001)) {
			maxValue = -minValue;
			maxDirection = minDirection;
			color = !color;
			steps = 0;
		}
//		if (true) {
//			move.currentState = previousState;
//			move.directionType = DirectionType.CURRENT;
//			return move;
//		}

		move.directionType = maxDirection;
		steps += 1;
		assert move.directionType != null;
		move.currentState = encode(color, steps, move.directionType);
		System.out.println("status: " + Integer.toBinaryString(move.currentState));

		return move;
	}
}