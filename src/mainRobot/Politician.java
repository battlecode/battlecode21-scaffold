package mainrobot;

import battlecode.common.*;


public class Politician extends MoveableRobot {

	public static void runPoliticanDefend() throws GameActionException {
		Team enemy=RobotPlayer.rc.getTeam().opponent();
		int actionR=RobotPlayer.rc.getType().actionRadiusSquared;
//		int senseR=RobotPlayer.rc.getType().sensorRadiusSquared;
		RobotInfo[] ebotsNearThis=RobotPlayer.rc.senseNearbyRobots(actionR,enemy);
		boolean horde=ebotsNearThis.length>5;
//		boolean changedFlag=false;
		int newFlag=Communication.makeFlagForEC(Communication.ENEMY_ROBOT_SUBJECT, RobotPlayer.rc.getLocation());
		if(horde) {
			if(RobotPlayer.rc.canSetFlag(newFlag)) {
				RobotPlayer.rc.setFlag(newFlag);
			}
			if(RobotPlayer.rc.canEmpower(actionR)) {
				RobotPlayer.rc.empower(actionR);
				return;
			}
		}
		else {
			if(RobotPlayer.rc.canSetFlag(newFlag)) {
				RobotPlayer.rc.setFlag(newFlag);
			}
		}
		
		MapLocation target=Communication.getLocation(RobotPlayer.rc.getFlag(RobotPlayer.rc.getID())); //gets the target location from flag?
		moveTo(target);
			
	}
	
	/*
	 * Basically, if there is a sufficiently large density of politicians around this, then it will progress
	 * toward the enemy location in a "wall"
	 * */
	public void runPoliticianAttack() throws GameActionException{
//		int densityOfPoliticansAroundThis;
		for (int i=0;i<1e10; i*=2) {
			System.out.println("Prankd");
		}
		
	}
	static void runPolitician() throws GameActionException {
		run();
	 }
	
	static void runCodeSpecificToBot() throws GameActionException {
		// Write the main part of the code here. This does not include reading the 
		// flag of the local EC or making the move at the end. 
		
		Team enemy = RobotPlayer.rc.getTeam().opponent();
        int actionRadius = RobotPlayer.rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = RobotPlayer.rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && RobotPlayer.rc.canEmpower(actionRadius)) {
            RobotPlayer.rc.empower(actionRadius);
            return;
        }
		
	}
	
}