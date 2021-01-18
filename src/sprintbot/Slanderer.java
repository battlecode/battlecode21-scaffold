package sprintbot;
import battlecode.common.*;

public class Slanderer {
    static RobotController rc;
    
    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() {
        throw new UnsupportedOperationException();
    }
    
    public static void executeTurn(int turnNumber) throws GameActionException {
        MapLocation loc = new MapLocation(0,0)
        hideAtLocation(loc);

        throw new UnsupportedOperationException();
    }
    public static void hideAtLocation(int turnNumber, MapLocation loc) throws GameActionException  {
        if(loc.compareTo(getLocation())){
            Pathfinding3.moveTo(loc);
            return false;
        } else return true;    

        
            //update flag with new location?
        
    
    }
}
