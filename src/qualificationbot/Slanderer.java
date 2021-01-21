package qualificationbot;
import battlecode.common.*;

public class Slanderer {
    static final int MAX_SQUARED_DIST_FROM_START = 30;

    static RobotController rc;
    static MapLocation startLoc;
    
    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            if (rc.getType() == RobotType.POLITICIAN) {
                
                while(rc.getEmpowerFactor(rc.getTeam(), 10) > 3) {
                    Pathfinding3.moveTo(startLoc);
                    if (rc.getLocation().equals(startLoc) && rc.canEmpower(2)) {
                        rc.empower(2);
                    }
                }
                Politician.run(); 

            }
            Communication.updateSectionMissionInfo();
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() {
        startLoc = rc.getLocation();
        Communication.updateIDList(false);
    }
    
    public static void executeTurn(int turnNumber) throws GameActionException {
        // MapLocation targetLoc = missionSectionLoc != null ? Communication.getSectionCenterLoc(missionSectionLoc) : null;

        // switch (missionType) {
        //     case Communication.MISSION_TYPE_HIDE:
        //         hideAtLocation(targetLoc);
        //         break;
        //     default:
        //         roamCloseToStart();
        //         break;
        // }
        if(turnNumber < 260) {
            roamCloseToStart();
        }
        else {
            if(rc.getLocation().distanceSquaredTo(startLoc) > 20) Pathfinding3.moveTo(startLoc);
        }
        
    }

    private static void roamCloseToStart() throws GameActionException {
        Direction[] allDirections = Direction.allDirections();
        for (int i = allDirections.length - 1; i >= 0; i--) {
            Direction nextDir = allDirections[i];
            MapLocation nextLoc = rc.getLocation().add(nextDir);
            if (nextLoc.isWithinDistanceSquared(startLoc, MAX_SQUARED_DIST_FROM_START) && rc.canMove(nextDir)) {
                rc.move(nextDir);
            }
        }
    }

    public static void hideAtLocation(MapLocation loc) throws GameActionException  {
        if(!loc.equals(rc.getLocation())){
            Pathfinding3.moveTo(loc);
        }
    }
}
