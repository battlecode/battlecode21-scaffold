package sprintbot;
import battlecode.common.*;

public class Politician {
    static RobotController rc;

    static int trackedMuck = -1;
    static int trackedEC = -1;

    static MapLocation missionSectionLoc;
    static int missionType;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(false);
            Communication.updateSectionMissionInfo();
            if (missionType == Communication.MISSION_TYPE_UNKNOWN) {
                missionSectionLoc = Communication.getClosestMission();
                if (missionSectionLoc != null) {
                    missionType = Communication.sectionMissionInfo[missionSectionLoc.x][missionSectionLoc.y];
                }
            }
            executeTurn(turn++);            
            Clock.yield();
        }
    }

    public static void initialize() {}

    public static void executeTurn(int turnNumber) throws GameActionException {
        MapLocation targetLoc = missionSectionLoc != null ? Communication.getSectionCenterLoc(missionSectionLoc) : null;
        boolean missionComplete = false;

        switch (missionType) {
            case Communication.MISSION_TYPE_DEMUCK:
                missionComplete = huntMuck(targetLoc);
                break;
            case Communication.MISSION_TYPE_SIEGE:
                missionComplete = siegeEC(targetLoc);
                break;
            default:
                Pathfinding3.moveToRandomTarget();
                break;
        }

        if (missionComplete) {
            Communication.setMissionComplete(missionSectionLoc);
            missionType = Communication.MISSION_TYPE_UNKNOWN;
            missionSectionLoc = null;
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

        // if near target loc and has no tracked ec, return true
        return rc.getLocation().isWithinDistanceSquared(loc, 6) && trackedEC == -1;
    }

    // hunt for a muck defensively at a given location
    // return false if a muck is not found
    public static boolean huntMuck(MapLocation loc) throws GameActionException {
        // have not yet identified muck to destroy
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(25); 
        boolean tracking = false; 

        for(int i = nearbyRobots.length - 1; i >= 0; i--) { 
            RobotInfo robot = nearbyRobots[i]; 
            if(robot.type == RobotType.MUCKRAKER && robot.team != rc.getTeam()) {
                if(robot.location.distanceSquaredTo(rc.getLocation()) <= 9)
                    rc.empower(robot.location.distanceSquaredTo(rc.getLocation()));
                else {
                    Pathfinding3.moveTo(robot.location); 
                    tracking = true; 
                    break;
                }
                
            }

        }

        if(!tracking) Pathfinding3.moveTo(loc); 

    /*    if(trackedMuck == -1) {
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
        } */

        // if near target loc and has no tracked muck, return true
        return rc.getLocation().isWithinDistanceSquared(loc, 6) && trackedMuck == -1;
    }
    
}
