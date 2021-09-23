package chemotaxis.g8;

import chemotaxis.sim.ChemicalCell;
import chemotaxis.sim.ChemicalCell.ChemicalType;
import chemotaxis.sim.DirectionType;
import chemotaxis.sim.Move;
import chemotaxis.sim.SimPrinter;

import java.util.Map;
import java.util.Optional;

public class Agent extends chemotaxis.sim.Agent {
	public Agent(SimPrinter simPrinter) {
		super(simPrinter);
	}

	private int decodeMode(byte x) { return x & 0b111; }
	private boolean decodeTurn(byte x) { return (x & 0b01000000) != 0; }
	private DirectionType decodeDT(byte x) { return DirectionType.values()[4 - ((x >> 3) & 0b111)]; }

	private byte encode(DirectionType dt, int mode, boolean turn) {
		return (byte)(mode + ((4 - dt.ordinal()) << 3) + (turn ? 0b01000000 : 0));
	}

	private DirectionType turnLeft(DirectionType dt) {
		return switch (dt) {
			case NORTH -> DirectionType.WEST;
			case SOUTH -> DirectionType.EAST;
			case EAST -> DirectionType.NORTH;
			case WEST -> DirectionType.SOUTH;
			case CURRENT -> DirectionType.CURRENT;
		};
	}
	private DirectionType turnRight(DirectionType dt) {
		return switch (dt) {
			case NORTH -> DirectionType.EAST;
			case SOUTH -> DirectionType.WEST;
			case EAST -> DirectionType.SOUTH;
			case WEST -> DirectionType.NORTH;
			case CURRENT -> DirectionType.CURRENT;
		};
	}

	@Override
	public Move makeMove(Integer randomNum, Byte previousState, ChemicalCell currentCell, Map<DirectionType, ChemicalCell> neighborMap) {
		Move move = new Move();
		int lastMode = decodeMode(previousState);
		DirectionType lastDT = decodeDT(previousState);
		boolean lastTurn = decodeTurn(previousState);
		System.out.println("Agent: " + lastDT + "  " + lastMode);

		for (DirectionType dt: neighborMap.keySet()) {
			ChemicalCell cell = neighborMap.get(dt);
			int mode = 0;
			if (cell.getConcentration(ChemicalType.BLUE) > 0.99) mode += 1;
			if (cell.getConcentration(ChemicalType.RED) > 0.99) mode += 2;
			if (cell.getConcentration(ChemicalType.GREEN) > 0.99) mode += 4;
			if (mode != 0) {
				System.out.println("Agent Ins: " + dt + "  " + mode);
				move.directionType = dt;
				move.currentState = encode(dt, mode, false);
				return move;
			}
		}

		if (lastMode == 0) {
			move.directionType = DirectionType.CURRENT;
			move.currentState = previousState;
			return move;
		}

		if (1 <= lastMode && lastMode <= 4) {
			if (neighborMap.get(lastDT).isBlocked()) {
				System.out.println("Agent Hit Wall!");
				lastDT = switch (lastMode) {
					case 1 -> turnLeft(lastDT);
					case 2 -> turnRight(lastDT);
					case 3 -> lastTurn ? turnLeft(lastDT) : turnRight(lastDT);
					case 4 -> !lastTurn ? turnLeft(lastDT) : turnRight(lastDT);
					default -> throw new IllegalStateException("Unexpected value: " + lastMode);
				};
			}
		} else if (lastMode == 5) {
			lastDT = turnLeft(lastDT);
			while (neighborMap.get(lastDT).isBlocked()) lastDT = turnRight(lastDT);
		} else if (lastMode == 6) {
			lastDT = turnRight(lastDT);
			while (neighborMap.get(lastDT).isBlocked()) lastDT = turnLeft(lastDT);
		}


		move.directionType = lastDT;
		lastTurn = !lastTurn;
		move.currentState = encode(lastDT, lastMode, lastTurn);
		return move;
	}
}