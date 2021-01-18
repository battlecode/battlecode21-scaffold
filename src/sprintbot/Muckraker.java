package sprintbot;
import battlecode.common.*;

public class Muckraker {
    static RobotController rc;
    static final int EXPOSE_RADIUS = 12; 
    static final int MAX_SENSE_RADIUS = 30; 
    static int foundPoli = -1; 
    static MapLocation startLoc;
    static MapLocation roamTarget;
    static int state; 
    static final int STATE_UNKNOWN = 0;
    static final int STATE_SLUETH = 1;
    static final int STATE_SCOUT = 2;
    static final int STATE_STICK = 3;
    static MapLocation missionLoc; 


    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(false);
            Communication.updateSectionMissionInfo();
            Communication.sendSectionInfo();
            checkForMissions(); 
            executeTurn(turn++);
            Clock.yield();
        }
    }

    private static boolean checkForMissions() {
        if (state != STATE_UNKNOWN) return false;
        MapLocation sectionLoc = Communication.getCurrentSection();
        for (int xDiff = -3; xDiff <= 3; xDiff++) {
            for (int yDiff = -3; yDiff <= 3; yDiff++) {
                int sectionX = sectionLoc.x + xDiff;
                int sectionY = sectionLoc.y + yDiff;
                if (sectionX < 0 || sectionY < 0 || sectionX >= Communication.NUM_SECTIONS || sectionY >= Communication.NUM_SECTIONS) continue;
                switch(Communication.getMissionTypeInSection(sectionX, sectionY)){
                    case Communication.MISSION_TYPE_SLEUTH:
                        missionLoc = Communication.getSectionCenterLoc(sectionX, sectionY); 
                        state = STATE_SLUETH;
                        return true;
                    case Communication.MISSION_TYPE_SCOUT:
                        missionLoc = Communication.getSectionCenterLoc(sectionX, sectionY); 
                        state = STATE_SCOUT;
                        return true;
                    case Communication.MISSION_TYPE_STICK:
                        missionLoc = Communication.getSectionCenterLoc(sectionX, sectionY); 
                        state = STATE_STICK;
                        return true;
                    default: 
                        break;  
                }
               
            }
        }
        return false;
    }
    

    public static void initialize() throws GameActionException {

    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        //States: sluething, scouting, sticking
        switch(state) {
            case STATE_UNKNOWN:
                roam();
                break;
            case STATE_SLUETH: 
                if(huntSlanderer(missionLoc)) state = STATE_UNKNOWN;
                break;
            case STATE_STICK: 
                if(stickToPoli(missionLoc)) state = STATE_UNKNOWN;
                break;
            case STATE_SCOUT: 
                if(scouting(missionLoc)) state = STATE_UNKNOWN;
                break;
            default: 
                break; 
        
        }
    }

    private static void roam() throws GameActionException {
        if (roamTarget == null) {
            roamTarget = randomMapLocation();
        }
        // System.out.println(roamTarget);
        if (!Pathfinding3.moveTo(roamTarget)) {
            roamTarget = null;
        }
    }

    private static MapLocation randomMapLocation() {
        MapLocation curLoc = rc.getLocation();
        return new MapLocation(curLoc.x + (int)(Math.random() * 128 - 64), curLoc.y + (int)(Math.random() * 128 - 64));
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
        if(rc.getLocation().compareTo(loc) != 0) {
            Pathfinding3.moveTo(loc); 
            return false; 
        } else {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(MAX_SENSE_RADIUS / 3); 
            for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                RobotInfo robot = nearbyRobots[i]; 
                if(robot.type == RobotType.POLITICIAN && robot.team != rc.getTeam()) {
                    foundPoli = robot.ID; 
                    break;                
                }
            }
            return trackPolitician();
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
