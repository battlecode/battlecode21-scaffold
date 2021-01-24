package qualificationbot;
import battlecode.common.*;

public class Muckraker {
    static final int EXPOSE_RADIUS = 12;
    static final int MAX_SCOUT_INFLUENCE = 30;
    static final double SCOUT_INFLUENCE_SCALING = .002;
    static final int SLEUTH_INFLUENCE = 100;

    static RobotController rc;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.sendMapInfo();
            Communication.updateIDList();
            Communication.updateSectionMissionInfo();
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() throws GameActionException { }

    public static void executeTurn(int turnNumber) throws GameActionException {
        // check for nearby enemy slanderers
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        RobotInfo closestTarget = null;
        int closestDist = Integer.MAX_VALUE;
        for(int i = nearbyRobots.length - 1; i >= 0; i--) {
            RobotInfo ri = nearbyRobots[i];
            if (ri.getTeam() != rc.getTeam() &&
                ri.getType() == RobotType.SLANDERER &&
                ri.getLocation().isWithinDistanceSquared(rc.getLocation(), closestDist - 1)) {
                    closestTarget = ri;
            }
        }
        
        // if one is found, move to and expose it, otherwise search using mission location
        if (closestTarget != null) {
            if (rc.canExpose(closestTarget.getID())) {
                rc.expose(closestTarget.getID());
            } else {
                Pathfinding3.moveTo(closestTarget.getLocation());
            }
        } else if (rc.getInfluence() >= SLEUTH_INFLUENCE) {
            MapLocation sleuthMissionLoc = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_SLEUTH);
            if (sleuthMissionLoc != null) {
                Pathfinding3.moveTo(sleuthMissionLoc);
            } else {
                Pathfinding3.moveToRandomTarget();
            }
        } else {
            Pathfinding3.moveToRandomTarget();
        }
    }
}
