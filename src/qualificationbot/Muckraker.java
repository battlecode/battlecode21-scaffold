package qualificationbot;
import battlecode.common.*;

public class Muckraker {
    static final int EXPOSE_RADIUS = 12;
    static final int MAX_SCOUT_INFLUENCE = 30;
    static final double SCOUT_INFLUENCE_SCALING = .002;
    static final int SLEUTH_INFLUENCE = 100;
    static MapLocation cornerLoc; 
    static MapLocation ecLoc; 
    static MapLocation[] corners; 
    static boolean staticEcDefense;  

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

    public static void initialize() throws GameActionException { 
        RobotInfo[] nearbyBots = rc.senseNearbyRobots(3); 
        for(int i = nearbyBots.length - 1; i >= 0; i--) {
            if(nearbyBots[i].type == RobotType.ENLIGHTENMENT_CENTER && rc.getTeam() == nearbyBots[i].team) {
                ecLoc = nearbyBots[i].location; 
                corners = new MapLocation[]{
                    ecLoc.add(Direction.NORTH).add(Direction.NORTH),
                    ecLoc.add(Direction.SOUTH).add(Direction.SOUTH),
                    ecLoc.add(Direction.EAST).add(Direction.EAST),
                    ecLoc.add(Direction.WEST).add(Direction.WEST),
                }; 

                break;
            }
        }
        staticEcDefense = false; 
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        // check for nearby enemy slanderers
        if(staticEcDefense) return; 
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
           // if(rc.canDetectLocation(ecLoc)) protectEcCorners(); 
            Pathfinding3.moveToRandomTarget();
        }
    }
    static void protectEcCorners() throws GameActionException {
        

        for(int i = corners.length - 1; i >= 0; i--) {
            while(rc.canDetectLocation(corners[i]) && !rc.isLocationOccupied(corners[i])) {
                Pathfinding3.moveTo(corners[i]);
                if(rc.getLocation() == corners[i]) {
                    staticEcDefense = true; 
                    break; 
                }
            }
            if(staticEcDefense) break;
        }

    } 
}


