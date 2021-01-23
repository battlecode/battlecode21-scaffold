package qualificationbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.5;
    static final int MAX_SLANDERER_INFLUENCE = 949;
    public static final double[] BID_PERCENTAGES = new double[] {.007, .014, .025, .05, .2};
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

    static final int ROUNDS_BETWEEN_SIEGE_POLITICIANS = 15;
    static final int ROUNDS_BETWEEN_DEMUCKING_POLITICIANS = 10;
    static final int ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS = 50;
    
    static final int ROUNDS_BETWEEN_SLANDERERS = 20;

    static final int SLEUTHING_MUCKRAKER_INFLUENCE = 100;
    static final int DEMUCKING_POLITICIAN_INFLUENCE = Politician.DEMUCK_INF;

    static int lastRoundBuiltSiegePolitician = -ROUNDS_BETWEEN_SIEGE_POLITICIANS;
    static int lastRoundBuiltDemuckPolitician = -ROUNDS_BETWEEN_DEMUCKING_POLITICIANS;
    static int lastRoundBuiltSleuthingMuckraker = -ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS;
    static int lastRoundBuiltSlanderer = -ROUNDS_BETWEEN_SLANDERERS;

    private static void buildRobot() throws GameActionException {
        int roundNum = rc.getRoundNum();

        MapLocation siegeSectionLoc = Communication.latestMissionSectionLoc[Communication.MISSION_TYPE_SIEGE];
        if (siegeSectionLoc != null && roundNum - lastRoundBuiltSiegePolitician > ROUNDS_BETWEEN_SIEGE_POLITICIANS) {
            int ecInfluence = Communication.ecInfluence[siegeSectionLoc.x][siegeSectionLoc.y];
            if (buildRobot(RobotType.POLITICIAN, ecInfluence * 2)) {
                lastRoundBuiltSiegePolitician = roundNum;
                return;
            }
        }

        MapLocation sleuthSectionLoc = Communication.latestMissionSectionLoc[Communication.MISSION_TYPE_SLEUTH];
        if (sleuthSectionLoc != null && roundNum - lastRoundBuiltSleuthingMuckraker > ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS) {
            if (buildRobot(RobotType.MUCKRAKER, SLEUTHING_MUCKRAKER_INFLUENCE)) {
                lastRoundBuiltSleuthingMuckraker = roundNum;
                return;
            }
        }

        if (roundNum - lastRoundBuiltSlanderer > ROUNDS_BETWEEN_SLANDERERS &&
            buildRobot(RobotType.SLANDERER, (int)Math.min(MAX_SLANDERER_INFLUENCE, rc.getInfluence() * SLANDERER_INFLUENCE_PERCENTAGE))) {
            lastRoundBuiltSlanderer = roundNum;
            return;
        }

        if (roundNum - lastRoundBuiltDemuckPolitician > ROUNDS_BETWEEN_DEMUCKING_POLITICIANS &&
            buildRobot(RobotType.POLITICIAN, DEMUCKING_POLITICIAN_INFLUENCE)) {
            lastRoundBuiltDemuckPolitician = roundNum;
            return;
        }

        buildRobot(RobotType.MUCKRAKER, 1);
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
        missionType = (missionType + 1) % Communication.NUM_MISSION_TYPES;
        if (missionType == 0) {
            missionType++;
        }

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
    }
}
