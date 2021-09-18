package chemotaxis.SAMG8;

import chemotaxis.sim.ChemicalCell;
import chemotaxis.sim.ChemicalCell.ChemicalType;
import chemotaxis.sim.DirectionType;
import chemotaxis.sim.Move;
import chemotaxis.sim.SimPrinter;

import java.util.Map;

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


	// returns true if they are NOT the opposite OR the same
	private boolean prevToDir(Byte previousState, DirectionType d) {
		if (previousState == 1 && d == DirectionType.NORTH) {return false;}
		else if (previousState == 2 && d == DirectionType.WEST) {return false;}
		else if (previousState == 3 && d == DirectionType.EAST) {return false;}
		else if (previousState == 4 && d == DirectionType.SOUTH) {return false;}
		else if (previousState == 0 && d == DirectionType.CURRENT) {return false;}
		else {return true;}
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

		double highestConcentration = 0;
		for (DirectionType directionType : neighborMap.keySet()) {
			if (highestConcentration <= neighborMap.get(directionType).getConcentration(chosenChemicalType) && this.prevToDir(previousState, directionType)) {
				highestConcentration = neighborMap.get(directionType).getConcentration(chosenChemicalType);
				move.directionType = directionType;
				if (directionType == DirectionType.EAST) {
					move.currentState = 2;
				}
				if (directionType == DirectionType.WEST) {
					move.currentState = 3;
				}
				if (directionType == DirectionType.NORTH) {
					move.currentState = 4;
				}
				if (directionType == DirectionType.SOUTH) {
					move.currentState = 1;
				}
			}
		}
		// opposite of prev move
		if (move.directionType == null) {
			if (previousState == 1) {
				move.directionType = DirectionType.NORTH;
				move.currentState = 4;
			}
			if (previousState == 2) {
				move.directionType = DirectionType.WEST;
				move.currentState = 3;
			}
			if (previousState == 3) {
				move.directionType = DirectionType.EAST;
				move.currentState = 2;
			}
			if (previousState == 4) {
				move.directionType = DirectionType.SOUTH;
				move.currentState = 1;
			}
			if (previousState == 0) {
				int n = randomNum%4 + 1;
				if (n == 1) {
					move.directionType = DirectionType.SOUTH;
					move.currentState = 1;
				}
				else if (n == 2) {
					move.directionType = DirectionType.EAST;
					move.currentState = 2;
				}
				else if (n == 3) {
					move.directionType = DirectionType.WEST;
					move.currentState = 3;
				}
				else if (n == 4) {
					move.directionType = DirectionType.NORTH;
					move.currentState = 4;
				}

			}
		}
		return move;
	}
}