package qualificationbot;
import battlecode.common.*;

public class Slanderer {
    static final int SQUARES_AWAY_FROM_CLOSEST_MUCK = 6;
    static final int MAX_SQUARED_DIST_FROM_START = 30;

    static RobotController rc;
    static MapLocation startLoc;
    
    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            if (rc.getType() == RobotType.POLITICIAN) {
                Politician.run(); 
            }
            Communication.updateSectionMissionInfo();
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() {
        startLoc = rc.getLocation();
        Communication.updateIDList();
    }
    
    public static void executeTurn(int turnNumber) throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        RobotInfo friendlyEC = null;
        for(int i = nearbyRobots.length - 1; i >= 0; i--) {
            RobotInfo robot = nearbyRobots[i]; 
            if (robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                friendlyEC = robot;
                break;
            }
        }
        if(friendlyEC != null) {
            Direction ecDir = rc.getLocation().directionTo(friendlyEC.getLocation()); 
            if (rc.canMove(ecDir.opposite())) {
                rc.move(ecDir.opposite());
            }
            
            return;
        }
        MapLocation closestEnemyMuckrakerLoc = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_DEMUCK);
        if (closestEnemyMuckrakerLoc != null) {
            Direction directionFromECToClosestMuck = startLoc.directionTo(closestEnemyMuckrakerLoc);
            Direction safeDir = directionFromECToClosestMuck.opposite();
            MapLocation safeLoc = new MapLocation(startLoc.x + safeDir.dx * SQUARES_AWAY_FROM_CLOSEST_MUCK,
                                                  startLoc.y + safeDir.dy * SQUARES_AWAY_FROM_CLOSEST_MUCK);
            Pathfinding3.moveTo(safeLoc);
        } else {
            roamCloseToStart();
        }
    }

    private static void roamCloseToStart() throws GameActionException {
        Direction[] allDirections = Direction.allDirections();
        for (int i = allDirections.length - 1; i >= 0; i--) {
            Direction nextDir = allDirections[i];
            MapLocation nextLoc = rc.getLocation().add(nextDir);
            if (nextLoc.isWithinDistanceSquared(startLoc, MAX_SQUARED_DIST_FROM_START) && rc.canMove(nextDir)) {
                rc.move(nextDir);
            }
        }
    }
}
