package sprintbot;
import battlecode.common.*;

public class Muckraker {
    static RobotController rc;
    static final int EXPOSE_RADIUS = 12; 
    static final int MAX_SENSE_RADIUS = 30; 
    static int foundPoli = -1; 
    static MapLocation startLoc;
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
        for (int xDiff = -5; xDiff <= 5; xDiff++) {
            for (int yDiff = -5; yDiff <= 5; yDiff++) {
                int sectionX = sectionLoc.x + xDiff;
                int sectionY = sectionLoc.y + yDiff;
                if (sectionX < 0 || sectionY < 0 || sectionX >= Communication.NUM_SECTIONS || sectionY >= Communication.NUM_SECTIONS) continue;
                switch(Communication.getMissionTypeInSection(sectionX, sectionY)){
                    case Communication.MISSION_TYPE_SLEUTH:
                        missionLoc = Communication.getSectionCenter(sectionX, sectionY); 
                        state = STATE_SLUETH
                        return true;
                        break;
                    case Communication.MISSION_TYPE_SCOUT:
                        missionLoc = Communication.getSectionCenter(sectionX, sectionY); 
                        state = STATE_SCOUT
                        return true;
                        break;
                    case Communication.MISSION_TYPE_STICK:
                        missionLoc = Communication.getSectionCenter(sectionX, sectionY); 
                        state = STATE_STICK
                        return true;
                        break;
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
            if(getRoundNum() % 3 == 0) {
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
        RobotInfo[] nearbyRobots = senseNearbyRobots(EXPOSE_RADIUS);
                for(int i = nearbyRobots.length - 1; x >= 0; x--) {
                    RobotInfo robot = nearbyRobots[i]; 
                    if(getType(robot) == RobotType.SLANDERER && canExpose(robot.location)) {
                        expose(robot.location); 
                    }
    }
    //Muckraker moves towards a location, and when it gets there, checks to see if there is a politician in a 10 r^2 area. 
    //If there is, it always attempts to move towards it until it can no longer sense it (either dead or out of range)
    private static boolean stickToPoli(MapLocation loc) throws GameActionException {
        if(rc.getLocation().compareTo(loc) != 0) {
            Pathfinding3.moveTo(loc); 
            return false; 
        } else {
        RobotInfo nearbyRobots = senseNearbyRobots(MAX_SENSE_RADIUS / 3); 
        for(int i = nearbyRobots.length - 1; x >= 0; x--) {
            RobotInfo robot = nearbyRobots[i]; 
            if(getType(robot) = RobotType.POLITICIAN) {
                foundPoli = robot.ID; 
                break;                
            }
        }
        return trackPolitician();
    }
    
    }
    private static boolean trackPolitician() {
        if(canSenseRobot(foundPoli)) {
            RobotInfo mark = senseRobot(foundPoli);
            Pathfinding3.moveTo(mark.location); 
            return false; 
        }
        else {
            foundPoli = -1;
            return true; 
        }
    }
}
