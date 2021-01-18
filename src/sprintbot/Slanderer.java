package sprintbot;
import battlecode.common.*;

public class Slanderer {
    static RobotController rc;
    static MapLocation startLoc;
    static MapLocation hideLoc;

    static final int MAX_SQUARED_DIST_FROM_START = 30;

    static int state;

    static final int STATE_UNKNOWN = 0;
    static final int STATE_HIDE = 1;
    
    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(false);
            Communication.updateSectionMissionInfo();
            checkForMissions();
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() {
        startLoc = rc.getLocation();
    }

    public static boolean checkForMissions() {
        if (state != STATE_UNKNOWN) return false;
        MapLocation sectionLoc = Communication.getCurrentSection();
        for (int xDiff = -3; xDiff <= 3; xDiff++) {
            for (int yDiff = -3; yDiff <= 3; yDiff++) {
                int sectionX = sectionLoc.x + xDiff;
                int sectionY = sectionLoc.y + yDiff;
                if (sectionX < 0 || sectionY < 0 || sectionX >= Communication.NUM_SECTIONS || sectionY >= Communication.NUM_SECTIONS) continue;
                if (Communication.getMissionTypeInSection(sectionX, sectionY) == Communication.MISSION_TYPE_HIDE) {
                    hideLoc = Communication.getSectionCenterLoc(sectionX, sectionY);
                    state = STATE_HIDE;
                    return true;
                } 
            }
        }
        return false;
    }
    
    public static void executeTurn(int turnNumber) throws GameActionException {
        switch (state) {
            case STATE_UNKNOWN:
                tryRoam();
                break;
            case STATE_HIDE:
                hideAtLocation(hideLoc);
                break;
            default:
                break;
        }
    }

    private static boolean tryRoam() throws GameActionException {
        Direction[] allDirections = Direction.allDirections();
        for (int i = allDirections.length - 1; i >= 0; i--) {
            Direction nextDir = allDirections[i];
            MapLocation nextLoc = rc.getLocation().add(nextDir);
            if (nextLoc.isWithinDistanceSquared(startLoc, MAX_SQUARED_DIST_FROM_START) && rc.canMove(nextDir)) {
                rc.move(nextDir);
                return true;
            }
        }
        return false;
    }

    public static void hideAtLocation(MapLocation loc) throws GameActionException  {
        if(!loc.equals(rc.getLocation())){
            Pathfinding3.moveTo(loc);
        }
    }
}
