package mainrobot;

import battlecode.common.*;


public class EnlightenmentCenter {
	
	// Variables for controlling number of bots placed.
	static int muckrakersPlaced = 0;
	
	// Variables dealing with flagging the conrolled bots
	static int flagForBotPlacedLastRound = -1;
	
	// Variables dealing with getting the flags of the controlled bots
    
    static void runMuckrakerEarlyGame() throws GameActionException {
        if (muckrakersPlaced < 8) {
            Muckraker.targetDirection = RobotPlayer.DIRECTIONS[muckrakersPlaced];
            if (RobotPlayer.rc.canBuildRobot(RobotType.MUCKRAKER, Muckraker.targetDirection, RobotPlayer.MUCKRAKER_INFLUENCE)) {
                RobotPlayer.rc.buildRobot(RobotType.MUCKRAKER, Muckraker.targetDirection, RobotPlayer.MUCKRAKER_INFLUENCE);
                muckrakersPlaced++;
            }
        }
    }

	/**
	 * Returns a random spawnable RobotType
	 *
	 * @return a random RobotType
	 */
	static RobotType randomSpawnableRobotType() {
	    return RobotPlayer.SPAWNABLE_ROBOTS[(int) (Math.random() * RobotPlayer.SPAWNABLE_ROBOTS.length)];
	}
	
	static void buildRobot(RobotType type, Direction direction, int influence, MapLocation assignedLocation, String assignedGroup) throws GameActionException {
		RobotPlayer.rc.buildRobot(type, direction, influence);
		flagForBotPlacedLastRound = Communication.makeFlagForCreatedBot(assignedGroup, assignedLocation);
	}
	
	/**
	 * If a bot was placed last round, this will display the flag that that robot 
	 * will read to initialize, otherwise this will run code to display some other flag.
	 * 
	 * @throws GameActionException
	 */
	static void endRun() throws GameActionException {
		
		if (flagForBotPlacedLastRound != -1) {
			RobotPlayer.rc.setFlag(flagForBotPlacedLastRound);
		} else {
			// Set flag to something else if desired
		}
		
	}

	static void runEnlightenmentCenter() throws GameActionException {
		
	    RobotType toBuild = randomSpawnableRobotType();
	    int influence = 50;
	    for (Direction dir : RobotPlayer.DIRECTIONS) {
	        if (RobotPlayer.rc.canBuildRobot(toBuild, dir, influence)) {
	            buildRobot(toBuild, dir, influence, RobotPlayer.rc.getLocation().add(Direction.SOUTH), Communication.MUCKRAKER_GROUP);
	        } else {
	            break;
	        }
	    }
	    
	    endRun();
	    
	}

}