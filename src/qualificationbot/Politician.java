package qualificationbot;
import battlecode.common.*;

public class Politician {
    static RobotController rc;

    static int trackedMuck = -1;
    static int trackedEC = -1;

    static MapLocation missionSectionLoc;
    static int missionType;
    static boolean siegeBot; 
    static final int DEMUCK_INF = 20; 

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
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

    public static void initialize() {
        Communication.updateIDList(false);
        siegeBot = rc.getInfluence() > DEMUCK_INF; 
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        
        MapLocation targetLoc = missionSectionLoc != null ? Communication.getSectionCenterLoc(missionSectionLoc) : null;
        boolean missionComplete = false;
        switch(missionType) { 
            case MISSION_TYPE_DEMUCK: 
                missionComplete = huntMuck(targetLoc); 
                break;
            case MISSION_TYPE_SIEGE: 
                missionComplete = siegeEC(targetLoc); 
                break; 
            case MISSION_TYPE_UNKNOWN: 
                missionComplete = defend(); 
                break; 
        }
      
       
        if (missionComplete) {
            Communication.setMissionComplete(missionSectionLoc);
            missionType = Communication.MISSION_TYPE_UNKNOWN;
            missionSectionLoc = null;
        }
    }
    private static boolean defend() {
        if(!siegeBot) {
            muckCount = 0; 
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(9);
            for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                RobotInfo robot = nearbyRobots[i]; 
                if(robot.getTeam() != rc.getTeam() && robot.getType() == RobotType.MUCKRAKER) {
                    muckCount++;
                    if(muckCount > 2) {
                        if(rc.canEmpower(9)) {
                            rc.empower(9); 
                        }
                    } 
                }
            }
            
        }
        return true; 
        


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

        // if in mission section and has no tracked ec, return true
        return inMissionSection() && trackedEC == -1;
    }

    // hunt for a muck defensively at a given location
    // return false if a muck is not found
    public static boolean huntMuck(MapLocation loc) throws GameActionException {
        int closestMuckDist = Integer.MAX_VALUE;
        MapLocation closestMuckLoc = null;
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(); 
        for(int i = nearbyRobots.length - 1; i >= 0; i--) { 
            RobotInfo robot = nearbyRobots[i]; 
            if(robot.type == RobotType.MUCKRAKER && robot.team != rc.getTeam() && rc.getLocation().isWithinDistanceSquared(robot.location, closestMuckDist - 1)) {
                closestMuckLoc = robot.location;
                closestMuckDist = rc.getLocation().distanceSquaredTo(robot.location);
            }
        }

        if (closestMuckLoc != null) {
            if (closestMuckDist <= 9 && rc.canEmpower(closestMuckDist)) {
                rc.empower(closestMuckDist);
                return true;
            } else {
                Pathfinding3.moveTo(closestMuckLoc);
                return false;
            }
        } else {
            Pathfinding3.moveTo(loc);
            return inMissionSection();
        }
    }

    private static boolean inMissionSection() {
        return Communication.getCurrentSection().equals(missionSectionLoc);
    }
    
}
