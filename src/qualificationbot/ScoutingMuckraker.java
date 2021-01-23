package qualificationbot;
import battlecode.common.*;

public class ScoutingMuckraker {
    static final int EXPOSE_RADIUS = 12;
    static final int BIG_UNIT_MIN_INFLUENCE = 100;

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
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        RobotInfo closestTarget = null;
        int closestDist = Integer.MAX_VALUE;
        for(int i = nearbyRobots.length - 1; i >= 0; i--) {
            RobotInfo ri = nearbyRobots[i];
            if (ri.getTeam() == rc.getTeam() ||
                !ri.getLocation().isWithinDistanceSquared(rc.getLocation(), closestDist - 1)) continue;
            switch (ri.getType()) {
                case SLANDERER:
                    closestTarget = ri;
                    break;
                case MUCKRAKER:
                case POLITICIAN:
                    if (ri.getInfluence() >= BIG_UNIT_MIN_INFLUENCE &&
                        Pathfinding3.getOpenAdjacentLoc(ri.getLocation()) != null) {
                        closestTarget = ri;
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    if (ri.getTeam() != Team.NEUTRAL &&
                        Pathfinding3.getOpenAdjacentLoc(ri.getLocation()) != null) {
                        closestTarget = ri;
                    }
                    break;
                default:
                    break;
            }
        }
        
        // if has no closest target, scout
        if (closestTarget == null) {
            Pathfinding3.moveToRandomTarget();
            return;
        }

        switch (closestTarget.getType()) {
            // for slanderers, expose if possible and otherwise just move to
            case SLANDERER:
                if (rc.canExpose(closestTarget.getID())) {
                    rc.expose(closestTarget.getID());
                } else {
                    Pathfinding3.moveTo(closestTarget.getLocation());
                }
                break;
            case ENLIGHTENMENT_CENTER:
            case MUCKRAKER:
            case POLITICIAN:
                Pathfinding3.stickToTarget(closestTarget.getLocation());
                break;
            default:
                break;
        }
    }
}
