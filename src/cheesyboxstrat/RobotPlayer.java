package cheesyboxstrat;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int turnCount = 0;
    static int slanderersBuilt = 0;
    static MapLocation startLoc;
    static boolean isBottomMuckraker;
    
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        RobotPlayer.startLoc = rc.getLocation();
        RobotPlayer.isBottomMuckraker = isBottomMuckraker();

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            try {
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }
                Clock.yield();
                turnCount += 1;
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static boolean isBottomMuckraker() {
        for (RobotController other : rc.senseNearbyRobots()) {
            if (other.getType() == RobotType.ENLIGHTENMENT_CENTER && other.getTeam() == rc.getTeam()) {
                return startLoc.y < other.getLocation().y;
            }
        }
        // shouldn't get here — ec should be detected
        return false;
    }

    static void runEnlightenmentCenter() throws GameActionException {
        // Build a slanderer every 50 turns
        if (slanderersBuilt <= turnCount / 50 && rc.canBuild(RobotType.SLANDERER, Direction.SOUTHWEST, rc.getInfluence())) {
            rc.buildRobot(RobotType.SLANDERER, Direction.SOUTHWEST, rc.getInfluence());
            slanderersBuilt++;
            return;
        }

        // Otherwise, try to build box of muckrakers
        if (rc.canBuildRobot(RobotType.MUCKRAKER, Directions.WEST, 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, Directions.WEST, 1);
        } else if (rc.canBuildRobot(RobotType.MUCKRAKER, Directions.SOUTH, 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, Directions.SOUTH, 1);
        }
    }

    static void runSlanderer() throws GameActionException {
        int xDiff = startLoc.x - rc.getLocation().x;
        int yDiff = startLoc.y - rc.getLocation().y;
        if (xDiff < 1) {
            if (rc.canMove(Direction.WEST)) {
                rc.move(Direction.WEST);
            }
        } else if (rc.canMove(Direction.SOUTH) && yDiff < 3) {
            rc.move(Direction.SOUTH);
        } else if (rc.canMove(Direction.WEST) && xDiff < 4) {
            rc.move(Direction.WEST);
        }
    }

    static void runMuckraker() throws GameActionException {
        int xDiff = startLoc.x - rc.getLocation().x;
        int yDiff = startLoc.y - rc.getLocation().y;
        if (isBottomMuckraker) {
            if (yDiff < 7 && rc.canMove(Direction.SOUTH)) {
                rc.move(Direction.SOUTH);
            } else if (yDiff == 7 && xDiff < 9 && rc.canMove(Direction.WEST)) {
                rc.move(Direction.WEST);
            }
        } else {
            if (xDiff < 8 && rc.canMove(Direction.WEST)) {
                rc.move(Direction.WEST);
            } else if (xDiff == 8 && yDiff < 8 && rc.canMove(Direction.SOUTH)) {
                rc.move(Direction.SOUTH);
            }
        }
    }
}
