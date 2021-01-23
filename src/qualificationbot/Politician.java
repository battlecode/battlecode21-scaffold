package qualificationbot;
import battlecode.common.*;

public class Politician {
    static RobotController rc;

    static int trackedMuck = -1;
    static int trackedEC = -1;

    static boolean siegeBot; 
    static final int DEMUCK_INF = 20; 
    static MapLocation targetLoc;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(); 
            Communication.updateSectionMissionInfo();
            executeTurn(turn++);            
            Clock.yield();
        }
    }

    public static void initialize() {
        siegeBot = rc.getInfluence() > DEMUCK_INF;
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        if(!siegeBot) defend(); 
        else {
            MapLocation missionLoc = Communication.getClosestMissionOfType(Communication.MISSION_TYPE_SIEGE);
            if (missionLoc != null) {
                System.out.println("Siege mission @ " + missionLoc);
                siegeEC(missionLoc);
            } else {
                
                Pathfinding3.moveToRandomTarget();
            }
        }
    }
    
    private static void siegeEC(MapLocation loc) throws GameActionException {
        // have not yet identified ec to destroy
        if(trackedEC == -1) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                RobotInfo robot = nearbyRobots[i]; 
                if(robot.getTeam() != rc.getTeam() && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    trackedEC = robot.ID;
                    break;
                }
            }
        }
        if(trackedEC != -1 && rc.canSenseRobot(trackedEC)) {
            // move toward tracked ec
            RobotInfo ec = rc.senseRobot(trackedEC); 
            if(rc.getLocation().isWithinDistanceSquared(ec.location, 1) && rc.canEmpower(1)) {
                rc.empower(1);
            } else if (!Pathfinding3.moveTo(ec.location) && rc.getLocation().isWithinDistanceSquared(ec.location, 9)) {
                int distSquaredToEC = rc.getLocation().distanceSquaredTo(rc.getLocation());
                if (rc.canEmpower(distSquaredToEC)) {
                    rc.empower(distSquaredToEC);
                }
            }
        } else {
            // reset tracked ec and move toward loc
            trackedEC = -1;
            Pathfinding3.moveTo(loc);
        }
    }

    // hunt for a muck defensively at a given location
    // return false if a muck is not found
    public static void defend() throws GameActionException {
        
        // TODO differentiate between big muckrakers and small muckrakers and try to stick to some of them etc

        int closestDist = Integer.MAX_VALUE;
        RobotInfo closestTarget = null;
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(); 
        for(int i = nearbyRobots.length - 1; i >= 0; i--) { 
            RobotInfo robot = nearbyRobots[i]; 
            if(robot.type == RobotType.MUCKRAKER && robot.team != rc.getTeam() && rc.getLocation().isWithinDistanceSquared(robot.location, closestDist - 1)) {
                closestTarget = robot;
                closestDist = rc.getLocation().distanceSquaredTo(robot.location);
            }
        }

        if (closestTarget != null) {
            if (closestDist <= 2 && rc.canEmpower(9)){
                rc.empower(9);
            } else {
                Pathfinding3.moveTo(closestTarget.getLocation());
            }
        } else {
            Pathfinding3.moveToRandomTarget();
        }
    }
    
}
