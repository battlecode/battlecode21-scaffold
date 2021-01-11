package mainrobot;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] SPAWNABLE_ROBOTS = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] DIRECTIONS = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    
    // Other final variables
    static final int MUCKRAKER_INFLUENCE = 1;

    // Variables relevant to every bot type
    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        while (true) {

            try {

            	// Call the run methods from the different robot classes.
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: EnlightenmentCenter.runEnlightenmentCenter();	break;
                    case POLITICIAN:           Politician.runPolitician();          		   	break;
                    case SLANDERER:            Slanderer.runSlanderer();          				break;
                    case MUCKRAKER:            Muckraker.runMuckraker();         				break;
                }

                turnCount += 1;
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
//                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

}