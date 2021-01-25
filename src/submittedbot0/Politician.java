package submittedbot0;
import battlecode.common.*;

public class Politician {
    static final int MIN_SIEGE_MISSION_CONVICTION = 50;
    static final int MAX_EMPOWER_RADIUS = 12;
    static final int SMALL_EMPOWER_RADIUS = 2;

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

    public static void initialize() {}

    public static void executeTurn(int turnNumber) throws GameActionException {
        // check for nearby enemy ec's or muckrakers
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        RobotInfo closestMuckraker = null;
        RobotInfo closestEnlightenmentCenter = null;
        int closestMuckrakerDist = Integer.MAX_VALUE;
        int closestEnlightenmentCenterDist = Integer.MAX_VALUE;
        for(int i = nearbyRobots.length - 1; i >= 0; i--) {
            RobotInfo ri = nearbyRobots[i];
            if (ri.getTeam() == rc.getTeam()) continue;
            switch (ri.getType()) {
                case MUCKRAKER:
                    if (ri.getLocation().isWithinDistanceSquared(rc.getLocation(), closestMuckrakerDist - 1)) {
                        closestMuckraker = ri;
                        closestMuckrakerDist = rc.getLocation().distanceSquaredTo(ri.getLocation());
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    if (ri.getLocation().isWithinDistanceSquared(rc.getLocation(), closestEnlightenmentCenterDist - 1)) {
                        closestEnlightenmentCenter = ri;
                        closestEnlightenmentCenterDist = rc.getLocation().distanceSquaredTo(ri.getLocation());
                    }
                    break;
                default:
                    break;
            }
        }
        
        // if one is found, move to and expose it, otherwise search using mission location
        if (closestEnlightenmentCenter != null) {
            // move to enemy/neutral ec until you can't, then empower
            if (!Pathfinding3.moveTo(closestEnlightenmentCenter.getLocation()) &&
                closestEnlightenmentCenterDist <= MAX_EMPOWER_RADIUS &&
                rc.canEmpower(closestEnlightenmentCenterDist)) {
                    rc.empower(closestEnlightenmentCenterDist);
            }
        } else if (closestMuckraker != null && (rc.getInfluence() & 1) == 0) {
            // move within small range of muckraker (or until you cant move anymore) and expose
            if (closestMuckrakerDist <= SMALL_EMPOWER_RADIUS &&
                rc.canEmpower(closestMuckrakerDist)) {
                    rc.empower(closestMuckrakerDist);
            } else {
                Pathfinding3.moveTo(closestMuckraker.getLocation());
            }
        } else if (rc.getConviction() >= MIN_SIEGE_MISSION_CONVICTION) {
            // if big, look for siege/settle missions
            MapLocation siegeMissionLocation = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_SIEGE);
            MapLocation settleMissionLocation = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_SETTLE);
            if (settleMissionLocation != null) {
                Pathfinding3.moveTo(settleMissionLocation);
            } else if (siegeMissionLocation != null) {
                Pathfinding3.moveTo(siegeMissionLocation);
            } else {
                Pathfinding3.scout();
            }
        } else {
            // if small, look for demuck missions
            MapLocation demuckMissionLocation = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_DEMUCK);
            MapLocation siegeMissionLocation = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_SIEGE);
            if (demuckMissionLocation != null) {
                Pathfinding3.moveTo(demuckMissionLocation);
            } else if (siegeMissionLocation != null) {
                // this will allow smaller politicians to clear out muckrakers near enemy ec
                Pathfinding3.moveTo(siegeMissionLocation);
            } else {
                Pathfinding3.scout();
            }
        }
    }
    
}
