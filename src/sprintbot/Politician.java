package sprintbot;
import battlecode.common.*;

public class Politician {
    static RobotController rc;
    static int trackedMuck = -1;
    static int trackedEC = -1;

    static MapLocation missionLoc;
    static MapLocation roamTarget;

    static int state;

    static final int STATE_UNKNOWN = 0;
    static final int STATE_DEMUCK = 1;
    static final int STATE_SEIGE = 2;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(false);
            Communication.updateSectionMissionInfo();            
            executeTurn(turn++);            
            Clock.yield();
        }
    }

    public static void initialize() {
        throw new UnsupportedOperationException();
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        switch (state) {
            case STATE_UNKNOWN:
                roam();
                break;
            case STATE_DEMUCK:
                if (!huntMuck(missionLoc)) {
                    state = STATE_UNKNOWN;
                }
                break;
            case STATE_SEIGE:
                if (!siegeEC(missionLoc)) {
                    state = STATE_UNKNOWN;
                }
                break;
            default:
                break;
        }
    }

    public static void roam() throws GameActionException {
        if (roamTarget == null) {
            roamTarget = randomMapLocation();
        }
        if (!Pathfinding3.moveTo(roamTarget)) {
            roamTarget = null;
        }
    }

    private static boolean siegeEC(MapLocation loc) throws GameActionException {
        // have not yet identified ec to destroy
        if(trackedEC == -1) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                RobotInfo robot = nearbyRobots[i]; 
                if(robot.getTeam() != rc.getTeam() && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    trackedEC = robot.ID;
                    break;
                }
            }
        }
        if(trackedEC != -1 && rc.canSenseRobot(trackedEC)) {
            // move toward tracked ec
            RobotInfo ec = rc.senseRobot(trackedEC); 
            if(rc.getLocation().isWithinDistanceSquared(ec.location, 2) && rc.canEmpower(2)) {
                rc.empower(2);
            } else if (!Pathfinding3.moveTo(ec.location) && rc.getLocation().isWithinDistanceSquared(ec.location, 9)) {
                int distSquaredToEC = rc.getLocation().distanceSquaredTo(rc.getLocation());
                if (rc.canEmpower(distSquaredToEC)) {
                    rc.empower(distSquaredToEC);
                }
            }
        } else {
            // reset tracked ec and move toward loc
            trackedEC = -1;
            Pathfinding3.moveTo(loc);
        }

        // if near target loc and has no tracked ec, return false
        return !(rc.getLocation().isWithinDistanceSquared(loc, 6) && trackedEC == -1);
    }

    public static MapLocation randomMapLocation() {
        MapLocation curLoc = rc.getLocation();
        return new MapLocation(curLoc.x + (int)(Math.random() * 128 - 64), curLoc.y + (int)(Math.random() * 128 - 64));
    }

    // hunt for a muck defensively at a given location
    // return false if a muck is not found
    public static boolean huntMuck(MapLocation loc) throws GameActionException {
        // have not yet identified muck to destroy
        if(trackedMuck == -1) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                RobotInfo robot = nearbyRobots[i]; 
                if(robot.getTeam() != rc.getTeam() && robot.getType() == RobotType.MUCKRAKER) {
                    trackedMuck = robot.ID;
                    break;
                }
            }
        }
        if(trackedMuck != -1 && rc.canSenseRobot(trackedMuck)) {
            // move toward tracked muck
            RobotInfo muck = rc.senseRobot(trackedMuck); 
            if(rc.getLocation().isWithinDistanceSquared(muck.location, 2) && rc.canEmpower(2)) {
                rc.empower(2);
            } else {
                Pathfinding3.moveTo(muck.location); 
            }
        } else {
            // reset tracked muck and move toward loc
            trackedMuck = -1;
            Pathfinding3.moveTo(loc);
        }

        // if near target loc and has no tracked muck, return false
        return !(rc.getLocation().isWithinDistanceSquared(loc, 6) && trackedMuck == -1);
    }
    
}
