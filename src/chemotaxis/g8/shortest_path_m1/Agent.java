package chemotaxis.g8.shortest_path_m1;

import chemotaxis.sim.ChemicalCell;
import chemotaxis.sim.ChemicalCell.ChemicalType;
import chemotaxis.sim.DirectionType;
import chemotaxis.sim.Move;
import chemotaxis.sim.SimPrinter;

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


	private Optional<DirectionType> extract(int x) {
		x &= 0b1111;
		if (x == 0) return Optional.empty();
		x -= 0b1000;
		for (DirectionType dir: DirectionType.values()) {
			if (dir.ordinal() == x) return Optional.of(dir);
		}
		return Optional.empty();
	}

	private byte encode(DirectionType d) {
		return (byte)(d.ordinal() + 0b1000);
	}
	private byte encode2(DirectionType d2, DirectionType d) {
		return (byte)((d2.ordinal() << 4) + d.ordinal() + 0b10001000);
	}

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

		ChemicalType chosenChemicalType = ChemicalType.BLUE;

		Optional<DirectionType> last = extract(previousState);
		Optional<DirectionType> last2 = extract(previousState >> 4);
		if (last.isPresent() && last2.isPresent()) assert !last.get().equals(last2.get());

		double highestConcentration = -1;
		for (DirectionType directionType : neighborMap.keySet()) {
			double multiplier = 1.0;
			if (last.isPresent() && directionType.equals(DirectionType.CURRENT)) multiplier = 0.1;
			if (last.isPresent() && last.get() == opposite(directionType)) multiplier = 0;
			if (last.isPresent() && last.get() == directionType) multiplier = 2.0;
			if (last2.isPresent() && last2.get() == opposite(directionType)) multiplier = 0.3;
			if (highestConcentration <= neighborMap.get(directionType).getConcentration(chosenChemicalType) * multiplier) {
				highestConcentration = neighborMap.get(directionType).getConcentration(chosenChemicalType) * multiplier;
				move.directionType = directionType;
			}
		}
		if (move.directionType != DirectionType.CURRENT) {
			if (last.isEmpty()) move.currentState = encode(move.directionType);
			else move.currentState = encode2(last.get(), move.directionType);
		} else {
			move.currentState = 0;
		}


		return move;
	}
}