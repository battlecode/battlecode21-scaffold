package mainrobot;

import battlecode.common.*;


public class Muckraker extends MoveableRobot {
	
	static MapLocation locationAhead;

    /**
     * pretty much algorithm for searching the map in the early game
     * uhh
     * moves in the general direction (no optimal pathfinding implemented yet)
     * when the bot senses the edge of the map, change direction to rotate right
     * honestly wtf am i doing haha sorry teammates im a noob
     * dwdw im learning
     * will soon be a pro
     * 
     * @throws GameActionException
     */
    static void moveEarlyGame() throws GameActionException {
        locationAhead = RobotPlayer.rc.adjacentLocation(targetDirection);    // um
        locationAhead = locationAhead.add(targetDirection);      // dont ask
        locationAhead = locationAhead.add(targetDirection);      // i dont know
        locationAhead = locationAhead.add(targetDirection);
        locationAhead = locationAhead.add(targetDirection);
        if (RobotPlayer.rc.onTheMap(locationAhead)) {
            Pathfinding.tryMove(targetDirection);
        } else {
            targetDirection = targetDirection.rotateRight();
            Pathfinding.tryMove(targetDirection);
        }
    }

	// i- this is so messy
    static void runMuckraker() throws GameActionException {
        run();
    }
    
    static void runCodeSpecificToBot() throws GameActionException {
    	if (RobotPlayer.rc.getRoundNum() < 100) {
            moveEarlyGame();
        }
//        Team enemy = RobotPlayer.rc.getTeam().opponent();
//        int actionRadius = RobotPlayer.rc.getType().actionRadiusSquared;
//        for (RobotInfo robot : RobotPlayer.rc.senseNearbyRobots(actionRadius, enemy)) {
//            if (robot.type.canBeExposed()) {
//                // It's a slanderer... go get them!
//                if (RobotPlayer.rc.canExpose(robot.location)) {
////                    System.out.println("e x p o s e d");
//                    RobotPlayer.rc.expose(robot.location);
//                    return;
//                }
//            } else if (robot.type.equals(RobotType.ENLIGHTENMENT_CENTER)) {
//                Communication.makeFlagForEC(Communication.ENEMY_EC_SUBJECT, robot.location);
//            } else {
//            Communication.makeFlagForEC(Communication.ENEMY_ROBOT_SUBJECT, robot.location);
//            }
//        }
//        Pathfinding.tryMove(targetDirection);
    }
}
