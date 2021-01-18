package sprintbot;
import battlecode.common.*;

public class Politician {
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
        throw new UnsupportedOperationException();
    }
    //hunt for a muck defensively at a given location
    public static void huntMuck(MapLocation loc) throws GameActionException {
        if(rc.getLocation().compareTo(loc) != 0) {
            moveTo(loc); 
            return false;
        }
        else {

        
        }


    }
}
