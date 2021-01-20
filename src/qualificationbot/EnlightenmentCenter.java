package qualificationbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.1;
    static final int MAX_SLANDERER_INFLUENCE = 949;
    public static final double[] BID_PERCENTAGES = new double[] {.005, .0075, .02, .04, .08};
    static final int NUM_ROUNDS = 1500;

    static int slanderersBuilt; 
    static int politiciansBuilt; 

    static int missionType = Communication.MISSION_TYPE_UNKNOWN;

    static RobotController rc;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(true);
            Communication.updateSectionRobotInfo();
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() {
        slanderersBuilt = 0; 
        politiciansBuilt = 0; 
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        buildRobot(); 
        bid(); 
        sendMission();
    }

    private static void bid() throws GameActionException {
        if(rc.getTeamVotes() < NUM_ROUNDS / 2 + 1) {
            int influence = (int)(rc.getInfluence() * BID_PERCENTAGES[(rc.getRoundNum() - 1) / (NUM_ROUNDS / BID_PERCENTAGES.length)]);
            if (rc.canBid(influence)) {
                rc.bid(influence);
            }
        }
    
    }

    private static void buildRobot() throws GameActionException {
        int turnCount = RobotPlayer.rc.getRoundNum();

        //build a slanderer every 50 turns with half influence, up to MAX_SLANDERER_INFLUENCE
        if (slanderersBuilt <= turnCount / 30) { 
            int influence = Math.min(MAX_SLANDERER_INFLUENCE, rc.getInfluence() / 2);
            if (buildRobot(RobotType.SLANDERER, influence)) {
                slanderersBuilt++;
            }
        }
        //build a politician every 20 turns with 20% of our influence. 
        else if (politiciansBuilt <= turnCount / 50) {
            int influence = rc.getInfluence() / 4; 
            if (buildRobot(RobotType.POLITICIAN, influence)) {
                politiciansBuilt++; 
            }
        }
        //if we are not building a slanderer or poli we should always be producing muckrakers. 
        else if (turnCount % 3 == 0) {
            buildRobot(RobotType.MUCKRAKER, 1);
        }
    }

    private static boolean buildRobot(RobotType type, int influence) throws GameActionException {
        Direction[] allDirections = Direction.allDirections();
        for (int i = allDirections.length - 1; i >= 0; --i) {
            Direction buildDir = allDirections[i];
            if (rc.canBuildRobot(type, buildDir, influence)) {
                rc.buildRobot(type, buildDir, influence);
                return true;
            }
        }
        return false;
    }

    private static void sendMission() throws GameActionException {
        // TODO add hide missions

        RobotType targetRobotType;
        switch (missionType) {
            case Communication.MISSION_TYPE_SLEUTH:
                targetRobotType = RobotType.SLANDERER;
                break;
            case Communication.MISSION_TYPE_SIEGE:
                targetRobotType = RobotType.ENLIGHTENMENT_CENTER;
                break;
            default:
                targetRobotType = null;
                break;
        }

        if (targetRobotType == null) return;

        MapLocation targetSection = Communication.getClosestSectionWithType(targetRobotType);
        Communication.sendMissionInfo(targetSection, missionType);

        missionType = (missionType + 1) % Communication.NUM_MISSION_TYPES;
        if (missionType == 0) {
            missionType++;
        }
    }
}
