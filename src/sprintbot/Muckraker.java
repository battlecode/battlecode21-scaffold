package sprintbot;
import battlecode.common.*;

public class Muckraker {
    static RobotController rc;
    static final int EXPOSE_RADIUS = 12; 
    static final int MAX_SENSE_RADIUS = 30; 
    static int foundPoli = -1; 
    static MapLocation startLoc;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(false);
            Communication.updateSectionMissionInfo();
            Communication.sendSectionInfo();
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() throws GameActionException {

    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        
    }

    public static boolean huntSlanderer(MapLocation loc, int turnNumber) throws GameActionException {
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
    public static void senseAndExpose() throws GameActionException {
        RobotInfo[] nearbyRobots = senseNearbyRobots(EXPOSE_RADIUS);
                for(int i = nearbyRobots.length - 1; x >= 0; x--) {
                    RobotInfo robot = nearbyRobots[i]; 
                    if(getType(robot) == RobotType.SLANDERER && canExpose(robot.location)) {
                        expose(robot.location); 
                    }
    }
    //Muckraker moves towards a location, and when it gets there, checks to see if there is a politician in a 10 r^2 area. 
    //If there is, it always attempts to move towards it until it can no longer sense it (either dead or out of range)
    public static boolean stickToPoli(MapLocation loc, int turnNumber) throws GameActionException {
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
    public static boolean trackPolitician() {
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
