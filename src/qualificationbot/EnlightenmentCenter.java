package qualificationbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.5;
    static final int MAX_SLANDERER_INFLUENCE = 949;
    public static final double[] BID_PERCENTAGES = new double[] {.007, .014, .025, .05, .2};
    static final int NUM_ROUNDS = 1500;
    static final int MIN_SLANDERER_INFLUENCE = 130; 
    static final int EARLY_GAME_ROUNDS = NUM_ROUNDS / 3;  

    static int slanderersBuilt; 
    static int politiciansBuilt; 
    static boolean earlyGame; 

    static int missionType = Communication.MISSION_TYPE_SLEUTH;

    static RobotController rc;

    public static void run() throws GameActionException {
        rc = RobotPlayer.rc;
        int turn = rc.getRoundNum();
        initialize();
        while (true) {
            Communication.ecUpdateIDList();
            Communication.ecUpdateMapInfo();
            if(turn > EARLY_GAME_ROUNDS) earlyGame = false; 
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() {
        slanderersBuilt = 0; 
        politiciansBuilt = 0; 
        earlyGame = true;
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

    static final int ROUNDS_BETWEEN_SIEGE_POLITICIANS = 20;
    static final int ROUNDS_BETWEEN_DEMUCKING_POLITICIANS = 10;
    static final int ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS = 50;
    
    static final int ROUNDS_BETWEEN_SLANDERERS_LATE = 20;
    static final int ROUNDS_BETWEEN_SLANDERERS_MIDDLE = 15;
    static final int ROUNDS_BETWEEN_SLANDERERS_EARLY = 10;
    static final int ROUNDS_BETWEEN_SLANDERERS_INITIAL = 5;

    static final int SLEUTHING_MUCKRAKER_INFLUENCE = 100;
    static final int DEMUCKING_POLITICIAN_INFLUENCE = Politician.DEMUCK_INF;

    static int lastRoundBuiltSiegePolitician = -ROUNDS_BETWEEN_SIEGE_POLITICIANS;
    static int lastRoundBuiltDemuckPolitician = -ROUNDS_BETWEEN_DEMUCKING_POLITICIANS;
    static int lastRoundBuiltSleuthingMuckraker = -ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS;
    static int lastRoundBuiltSlanderer = -ROUNDS_BETWEEN_SLANDERERS_INITIAL;

    private static void buildRobot() throws GameActionException {
        int roundNum = rc.getRoundNum();
        int slandererGap = (roundNum < 100) ?  ROUNDS_BETWEEN_SLANDERERS_INITIAL : (roundNum < 500) ? ROUNDS_BETWEEN_SLANDERERS_EARLY : (roundNum < 1000) ? ROUNDS_BETWEEN_SLANDERERS_MIDDLE : ROUNDS_BETWEEN_SLANDERERS_LATE;
        boolean neutral = true; 
        MapLocation attackECLoc = Communication.getClosestSiegeableEC(true);
        if (attackECLoc == null) {
            attackECLoc = Communication.getClosestSiegeableEC(false);
            neutral = false; 
        }

        if (attackECLoc != null && roundNum - lastRoundBuiltSiegePolitician > ROUNDS_BETWEEN_SIEGE_POLITICIANS) {
            int ecInfluence = 0; 
            if(neutral) ecInfluence = Communication.ecInfluence[attackECLoc.x % 128][attackECLoc.y % 128] * 1.25; 

            else{
                ecInfluence = (rc.getInfluence() * .75 >  Communication.ecInfluence[attackECLoc.x % 128][attackECLoc.y % 128] * 1.5) ? Communication.ecInfluence[attackECLoc.x % 128][attackECLoc.y % 128] : (int)(rc.getInfluence() * .1); 
            }
            if (buildRobot(RobotType.POLITICIAN, ecInfluence)) {
                lastRoundBuiltSiegePolitician = roundNum;
                return;
            }
        }
        if (roundNum - lastRoundBuiltSlanderer > slandererGap &&
            buildRobot(RobotType.SLANDERER, (int)Math.min(MAX_SLANDERER_INFLUENCE, Math.max(MIN_SLANDERER_INFLUENCE, rc.getInfluence() * SLANDERER_INFLUENCE_PERCENTAGE)))) {
            lastRoundBuiltSlanderer = roundNum;
            return;
        }

        MapLocation sleuthSectionLoc = Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_SLANDERER);
        if (sleuthSectionLoc != null && roundNum - lastRoundBuiltSleuthingMuckraker > ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS) {
            if (buildRobot(RobotType.MUCKRAKER, SLEUTHING_MUCKRAKER_INFLUENCE)) {
                lastRoundBuiltSleuthingMuckraker = roundNum;
                return;
            }
        }

        

        if (roundNum > 25 && roundNum - lastRoundBuiltDemuckPolitician > ROUNDS_BETWEEN_DEMUCKING_POLITICIANS && 
            buildRobot(RobotType.POLITICIAN, DEMUCKING_POLITICIAN_INFLUENCE)) {
            lastRoundBuiltDemuckPolitician = roundNum;
            return;
        }

        buildRobot(RobotType.MUCKRAKER, 1);
    }

    private static boolean buildRobot(RobotType type, int influence) throws GameActionException {
        // TODO: use closest units for this instead
        if(type == RobotType.SLANDERER) {
            if(Communication.getClosestEnemyUnitOfType(ENEMY_TYPE_MUCKRAKER) != null && Communication.getClosestEnemyUnitOfType(ENEMY_TYPE_MUCKRAKER).distanceSquaredTo(rc.getLocation()) < 50) {
                buildRobot(RobotType.POLITICIAN, DEMUCK_INF); 
                return false; 
            }
            else {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(); 
                for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                    if(nearbyRobots[i].type = RobotType.MUCKRAKER && rc.getTeam() != nearbyRobots[i].team) {
                        buildRobot(RobotType.POLITICIAN, DEMUCK_INF); 
                        return false; 
                    }
                }
            }
        }
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

        switch (missionType) {
            case Communication.MISSION_TYPE_SLEUTH:
                Communication.sendMissionInfo(Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_SLANDERER),
                                              missionType);
                break;
            case Communication.MISSION_TYPE_SIEGE:
                Communication.sendMissionInfo(Communication.getClosestSiegeableEC(false),
                                              missionType);
                break;
            case Communication.MISSION_TYPE_SETTLE:
                Communication.sendMissionInfo(Communication.getClosestSiegeableEC(true),
                                              missionType);
                break;
            case Communication.MISSION_TYPE_DEMUCK:
                Communication.sendMissionInfo(Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_MUCKRAKER),
                                              missionType);
                break;
            case Communication.MISSION_TYPE_DEPOLI:
                Communication.sendMissionInfo(Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_SLANDERER),
                                              missionType);
                break;
            default:
                break;
        }

        missionType = (missionType + 1) % Communication.NUM_MISSION_TYPES;
    }
}
