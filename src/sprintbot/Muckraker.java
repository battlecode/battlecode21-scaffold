package sprintbot;
import battlecode.common.*;

public class Muckraker {
    static RobotController rc;
    
    static MapLocation startLoc;

    static MapLocation northBound;
    static MapLocation southBound;
    static MapLocation eastBound;
    static MapLocation westBound;
    static MapLocation northEastBound;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() throws GameActionException {
        startLoc = rc.getLocation();
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        northBound = new MapLocation(x, y + 64);
        southBound = new MapLocation(x, y - 64);
        eastBound = new MapLocation(x + 64, y);
        westBound = new MapLocation(x - 64, y);
        northEastBound = new MapLocation(x + 64, y + 64);
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        // if (Pathfinding2.lowerXBound == Integer.MIN_VALUE) {
        //     Pathfinding2.moveTo(westBound);
        // } else if (Pathfinding2.upperXBound == Integer.MAX_VALUE) {
        //     Pathfinding2.moveTo(eastBound);
        // } else if (Pathfinding2.lowerYBound == Integer.MIN_VALUE) {
        //     Pathfinding2.moveTo(southBound);
        // } else if (Pathfinding2.upperYBound == Integer.MAX_VALUE) {
        //     Pathfinding2.moveTo(northBound);
        // } else {
        //     Pathfinding2.moveTo(startLoc);
        // }
        // if (rc.getRoundNum() <= 2) {
            Pathfinding3.moveTo(northEastBound);
        // }
    }
}
