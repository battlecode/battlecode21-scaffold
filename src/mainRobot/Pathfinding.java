package mainrobot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;


public class Pathfinding {
	
	static Direction bugDirection = null;
	static final double passabilityThreshold = 0.7;

	//MARK: - CALCULATE MEDIAN
    static void calculateMedian() throws GameActionException {
        MapLocation location = RobotPlayer.rc.getLocation();
        int x;
        int y;

        //TODO: fix the rounding on the radius?
        int radius = RobotPlayer.rc.getType().sensorRadiusSquared;

        // from your current spot, are we within radius, Scotty?

        for (x = location.x - (int) Math.sqrt(radius); x <= location.x; x ++) {
            for (y = location.y - (int) Math.sqrt(radius); y <= location.y; y++) {
                if ((x - location.x) * (x - location.x) + (y - location.y) * (y - location.y) <= radius) {
                    MapLocation transformedLocation = new MapLocation(x, y);
                    if (transformedLocation.isWithinDistanceSquared(transformedLocation, RobotPlayer.rc.getType().sensorRadiusSquared)) {
                        double passability = RobotPlayer.rc.sensePassability(transformedLocation);
                        System.out.println(passability);
                    }
                }
            }
        }

    }
    
    ////////////////////////////////////////////////////////////////////////////
    // BASIC BUG - just follow the obstacle while it's in the way
    //             not the best bug, but works for "simple" obstacles
    //             for better bugs, think about Bug 2!


	static void basicBug(MapLocation target) throws GameActionException {
	    Direction d = RobotPlayer.rc.getLocation().directionTo(target);
	    if (RobotPlayer.rc.getLocation().equals(target)) {
	        // do something else, now that you're there
	        // here we'll just explode
	        if (RobotPlayer.rc.canEmpower(1)) {
	            RobotPlayer.rc.empower(1);
	        }
	    } else if (RobotPlayer.rc.isReady()) {
	        if (RobotPlayer.rc.canMove(d) && RobotPlayer.rc.sensePassability(RobotPlayer.rc.getLocation().add(d)) >= Pathfinding.passabilityThreshold) {
	            RobotPlayer.rc.move(d);
	            Pathfinding.bugDirection = null;
	        } else {
	            if (Pathfinding.bugDirection == null) {
	                Pathfinding.bugDirection = d;
	            }
	            for (int i = 0; i < 8; ++i) {
	                if (RobotPlayer.rc.canMove(Pathfinding.bugDirection) && RobotPlayer.rc.sensePassability(RobotPlayer.rc.getLocation().add(Pathfinding.bugDirection)) >= Pathfinding.passabilityThreshold) {
	                    RobotPlayer.rc.setIndicatorDot(RobotPlayer.rc.getLocation().add(Pathfinding.bugDirection), 0, 255, 255);
	                    RobotPlayer.rc.move(Pathfinding.bugDirection);
	                    Pathfinding.bugDirection = Pathfinding.bugDirection.rotateLeft();
	                    break;
	                }
	                RobotPlayer.rc.setIndicatorDot(RobotPlayer.rc.getLocation().add(Pathfinding.bugDirection), 255, 0, 0);
	                Pathfinding.bugDirection = Pathfinding.bugDirection.rotateRight();
	            }
	        }
	    }
	}

	/**
	     * Attempts to move in a given direction.
	     *
	     * @param dir The intended direction of movement
	     * @return true if a move was performed
	     * @throws GameActionException
	     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (RobotPlayer.rc.canMove(dir)) {
            RobotPlayer.rc.move(dir);
            calculateMedian();

            return true;
        } else return false;
    }

	/**
	 * Returns a random Direction.
	 *
	 * @return a random Direction
	 */
	static Direction randomDirection() {
	    return RobotPlayer.DIRECTIONS[(int) (Math.random() * RobotPlayer.DIRECTIONS.length)];
	}

	

}
