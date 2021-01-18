package sprintbot;
import battlecode.common.*;

public class Muckraker {
    static final int EXPOSE_RADIUS = 12; 
    static final int MAX_SENSE_RADIUS = 30; 

    static RobotController rc;
    static MapLocation startLoc;

    static int foundPoli = -1; 
    
    static int missionType;
    static MapLocation missionSectionLoc; 


    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(false);
            Communication.updateSectionMissionInfo();
            Communication.sendSectionInfo();
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

    public static void initialize() throws GameActionException {}

    public static void executeTurn(int turnNumber) throws GameActionException {
        MapLocation targetLoc = missionSectionLoc != null ? Communication.getSectionCenterLoc(missionSectionLoc) : null;
        boolean missionComplete = false;

        // System.out.println("mission type: " + missionType);

        //States: sluething, scouting, sticking
        switch(missionType) {
            case Communication.MISSION_TYPE_SLEUTH: 
                missionComplete = huntSlanderer(targetLoc);
                break;
            case Communication.MISSION_TYPE_STICK: 
                missionComplete = stickToPoli(targetLoc);
                break;
            case Communication.MISSION_TYPE_SCOUT: 
                missionComplete = scouting(targetLoc);
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

    private static boolean scouting(MapLocation loc) throws GameActionException {
        if(!rc.getLocation().equals(loc)) {
            Pathfinding3.moveTo(loc); 
            return false; 
        }
        else {
            return true;
        }
    
    }
    private static boolean huntSlanderer(MapLocation loc) throws GameActionException {
        if(rc.getLocation().compareTo(loc) != 0) {
            Pathfinding3.moveTo(loc);
            if(rc.getRoundNum() % 3 == 0) {
                senseAndExpose(); 
            }
            return false; 
        }
        
        else {
            senseAndExpose();
            return true;
        }
    }
    private static void senseAndExpose() throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(EXPOSE_RADIUS);
        for(int i = nearbyRobots.length - 1; i >= 0; i--) {
            RobotInfo robot = nearbyRobots[i]; 
            if(robot.type == RobotType.SLANDERER && robot.team != rc.getTeam() && rc.canExpose(robot.location)) {
                rc.expose(robot.location); 
            }
        }
    }
    //Muckraker moves towards a location, and when it gets there, checks to see if there is a politician in a 10 r^2 area. 
    //If there is, it always attempts to move towards it until it can no longer sense it (either dead or out of range)
    private static boolean stickToPoli(MapLocation loc) throws GameActionException {
        if(!Communication.getCurrentSection().equals(missionSectionLoc) ) {
            Pathfinding3.moveTo(loc); 
            return false; 
        } else {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(MAX_SENSE_RADIUS / 3); 
            for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                RobotInfo robot = nearbyRobots[i]; 
                if(robot.type == RobotType.POLITICIAN && robot.team != rc.getTeam()) {
                    foundPoli = robot.ID; 
                    return trackPolitician();           
                }
            }
    
            return true; 
            
        }
    
    }
    private static boolean trackPolitician() throws GameActionException {
        if(rc.canSenseRobot(foundPoli)) {
            RobotInfo mark = rc.senseRobot(foundPoli);
            Pathfinding3.moveTo(mark.location); 
            return false; 
        }
        else {
            foundPoli = -1;
            return true; 
        }
    }
}
