package mainrobot;

import battlecode.common.*;


public class MoveableRobot {

	// Information about local EC
	static RobotInfo localECInfo = null;
	static int currentECFlag = 0;
	
	// Information about robot
	static String assignedGroup = null;
	static MapLocation targetLocation = null;
	static Direction targetDirection = null;
	
	
	/**
	 * Performs actions necessary when the robot spawns. These include, but may not be 
	 * limited to: 
	 * 1. Set localECInfo to obtained RobotInfo object of the local EC
	 * 2. Set targetLocation to the location given by the local EC
	 * 3. Set targetDirection to the direction from the EC to the target location, will later 
	 * 		just be the direction from the bot to the targetLocation after another location is specified
	 * 
	 * @throws GameActionException 
	 */
	static void initializeRobot() throws GameActionException {
		
		RobotInfo[] nearbyBotInfo = RobotPlayer.rc.senseNearbyRobots(2, RobotPlayer.rc.getTeam());
		for (RobotInfo info : nearbyBotInfo) {
			if (info.getType().equals(RobotType.ENLIGHTENMENT_CENTER)) {
				localECInfo = info;
				break;
			}
		}
		
		currentECFlag = RobotPlayer.rc.getFlag(localECInfo.ID);
		targetLocation = Communication.getLocation(currentECFlag);
		targetDirection = localECInfo.location.directionTo(targetLocation);
		
	}
	
	/**
	 * Reads the local EC flag. If the to group of the EC flag is the assignedGroup of this robot, 
	 * then the method will set the targetLocation and targetDirections to their corresponding values 
	 * interpreted from the flag.
	 * 
	 * @throws GameActionException
	 */
	static void readLocalECFlag() throws GameActionException {
		
		currentECFlag = RobotPlayer.rc.getFlag(localECInfo.ID);
		if (Communication.getGroupToFromFlag(currentECFlag).equals(assignedGroup)) {
			targetLocation = Communication.getLocation(currentECFlag);
			targetDirection = RobotPlayer.rc.getLocation().directionTo(targetLocation);
		}
		
	}
	
	/**
	 * Main move method for moveable robots. Calls the proper move algorithm in Pathfinding.
	 * 
	 * @param target MapLocation
	 * @throws GameActionException
	 */
	static void moveTo(MapLocation target) throws GameActionException {
		
		Pathfinding.basicBug(target);
		
	}
	
	/**
	 * Performs actions that all moveable robots need to do at the beginning of their 
	 * turns. This includes possibly initializing the robot and reading the local 
	 * EC flag/setting targetLocation accordingly.
	 * 
	 * @throws GameActionException
	 */
	static void startRun() throws GameActionException {
		
		if (RobotPlayer.turnCount == 0) {
			initializeRobot();
		} else {
			
			readLocalECFlag();
			
		}
		
	}
	
	static void runCodeSpecificToBot() throws GameActionException {
		// This method should be overwritten in the specific robot class.
	}
	
	/**
	 * If the robot still has a move to make, this will make it move 
	 * to attempt to go towards the targetLocation.
	 * 
	 * @throws GameActionException
	 */
	static void endRun() throws GameActionException {
		
		if (RobotPlayer.rc.isReady()) {
			moveTo(targetLocation);
		}
		
	}
	
	static void run() throws GameActionException {
		
		startRun();
		runCodeSpecificToBot();
		endRun();
		
	}
	
}
