package mainrobot;

import battlecode.common.*;


public class Slanderer extends MoveableRobot {
	RobotController rc;

	final static int[] GOOD_SLANDERER_INPUT_INFLUENCES = {
			21, 41, 63, 85, 107, 130, 154, 178, 203, 228, 255, 282, 310, 339, 368, 399, 431, 463, 497,
			532, 568, 605, 643, 683, 724, 766, 810, 855, 902, 949
	}; // Do not put more than 949 in EVER!!! You will waste influence pointlessly.


	//this method is used to move away from enemy by passing in a location away from enemy
	public void moveAwayFromEnemy(MapLocation target) throws GameActionException {
		Direction dToTarget=rc.getLocation().directionTo(target);
		for (int i=0; i<8; i++) {
			if(rc.canMove(dToTarget)) {
				rc.move(dToTarget);
				break;
			}
			else {
				dToTarget.rotateLeft();
			}
		}
	}
	
	public void sensedEnemies() throws GameActionException {	
		RobotInfo[] oppsNearMe=rc.senseNearbyRobots();
		int newFlag=Communication.makeFlagForEC(Communication.ENEMY_ROBOT_SUBJECT,rc.getLocation());
		if(oppsNearMe.length>0) {
			if(rc.canSetFlag(newFlag)) {
				rc.setFlag(newFlag);
			}
		}
	}
	
	static void runSlanderer() throws GameActionException {  
		run();

	}
	
	static void runCodeSpecificToBot() {
		// Write the main part of the code here. This does not include reading the 
		// flag of the local EC or making the move at the end. 
	}

	// determine influence based on good slander influences
	static int findBestInfluence(int inputInfluence) {

		int index = 0;
		int distance = Math.abs(GOOD_SLANDERER_INPUT_INFLUENCES[0] - inputInfluence);

		for (int i = 0; i < GOOD_SLANDERER_INPUT_INFLUENCES.length; i ++) {
			int calculatedDistance = Math.abs(GOOD_SLANDERER_INPUT_INFLUENCES[i] - inputInfluence);
			if (calculatedDistance < distance) {

				// round down
				if (GOOD_SLANDERER_INPUT_INFLUENCES[i] > inputInfluence) {

					// check if it will go negative positions for the array, then set to 0 if so
					if (i - 1 < 0) {
						index = 0;
					} else {
						index = i - 1;
					}

					distance = calculatedDistance;
				} else {
					index = i;
					distance = calculatedDistance;
				}

			}
		}
		int finalInfluence = GOOD_SLANDERER_INPUT_INFLUENCES[index];

		return finalInfluence;
	}

}