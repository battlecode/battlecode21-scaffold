package sprintbot;
import battlecode.common.*;

public class Politician {
    static RobotController rc;
    public static int trackedMuck = -1; 

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
        throw new UnsupportedOperationException();
    }
    //hunt for a muck defensively at a given location
    public static void huntMuck(MapLocation loc) throws GameActionException {
        
        //have not yet identified muck to destroy
        if(trackedMuck == -1){
            if(rc.getLocation().compareTo(loc) != 0) {
                moveTo(loc); 
                return false;
            }
            else {
                RobotInfo[] nearbyRobots = senseNearbyRobots(EXPOSE_RADIUS);
                for(int i = nearbyRobots.length - 1; x >= 0; x--) {
                    RobotInfo robot = nearbyRobots[i]; 
                    if(getType(robot) == RobotType.MUCKRAKER) {
                        trackedMuck = robot.ID; 
                    }
                }
            }
        //have identified muck to destroy
        } else {
            if(rc.canSenseRobot(trackedMuck)) {
                RobotInfo muck = rc.senseRobot(trackedMuck); 
                if(rc.getLocation().distanceSquaredTo(muck.location) == 2 && rc.canEmpower(2)) {
                    rc.empower(2);
                }
                else Pathfinding3.moveTo(muck.location); 
            }
            else trackedMuck = -1; 
    
        }
}
