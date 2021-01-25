package qualificationbot;
import battlecode.common.*;

public class Slanderer {
    static final int SQUARES_AWAY_FROM_CLOSEST_MUCK = 4;
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
        MapLocation closestEnemyMuckrakerLoc = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_DEMUCK);
        if (closestEnemyMuckrakerLoc != null) {
            int dx = closestEnemyMuckrakerLoc.x > startLoc.x ? -SQUARES_AWAY_FROM_CLOSEST_MUCK : SQUARES_AWAY_FROM_CLOSEST_MUCK;
            int dy = closestEnemyMuckrakerLoc.y > startLoc.y ? -SQUARES_AWAY_FROM_CLOSEST_MUCK : SQUARES_AWAY_FROM_CLOSEST_MUCK;
            MapLocation safeLoc = new MapLocation(startLoc.x + dx, startLoc.y + dy);
            Pathfinding3.moveTo(safeLoc);
        }

        int dx = (int)(Math.random() * 17) - 8;
        int dy = (int)(Math.random() * 17) - 8;
        Pathfinding3.moveTo(new MapLocation(startLoc.x + dx, startLoc.y + dy));
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
