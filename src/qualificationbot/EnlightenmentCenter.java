package qualificationbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.75;
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

    static final int ROUNDS_BETWEEN_SIEGE_POLITICIANS = 10;
    static final int ROUNDS_BETWEEN_SETTLE_POLITICIANS = 20;
    static final int ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS = 50;
    static final int ROUNDS_BETWEEN_EXPENSIVE_SCOUTING_MUCKRAKERS = 100000;
    static final int ROUNDS_BETWEEN_DEMUCKING_POLITICIANS = 10;
    
    static final int ROUNDS_BETWEEN_SLANDERERS_LATE = 10;
    static final int ROUNDS_BETWEEN_SLANDERERS_MIDDLE = 10;
    static final int ROUNDS_BETWEEN_SLANDERERS_EARLY = 7;
    static final int ROUNDS_BETWEEN_SLANDERERS_INITIAL = 5;

    static final int SLEUTHING_MUCKRAKER_INFLUENCE = Muckraker.SLEUTH_INFLUENCE;
    static final int SETTLE_STARTING_INFLUENCE = MIN_SLANDERER_INFLUENCE;

    static int lastRoundBuiltSiegePolitician = -ROUNDS_BETWEEN_SIEGE_POLITICIANS;
    static int lastRoundBuiltSettlePolitician = -ROUNDS_BETWEEN_SETTLE_POLITICIANS;
    static int lastRoundBuiltDemuckPolitician = 0;
    static int lastRoundBuiltSleuthingMuckraker = 0;
    static int lastRoundBuiltExpensiveScoutingMuckraker = 0;
    static int lastRoundBuiltSlanderer = -ROUNDS_BETWEEN_SLANDERERS_INITIAL;

    private static void buildRobot() throws GameActionException {
        int roundNum = rc.getRoundNum();
        int slandererGap = (roundNum < 100) ?  ROUNDS_BETWEEN_SLANDERERS_INITIAL : (roundNum < 500) ? ROUNDS_BETWEEN_SLANDERERS_EARLY : (roundNum < 1000) ? ROUNDS_BETWEEN_SLANDERERS_MIDDLE : ROUNDS_BETWEEN_SLANDERERS_LATE;

        MapLocation settleECLoc = Communication.getClosestSiegeableEC(true);
        if (settleECLoc != null && roundNum - lastRoundBuiltSettlePolitician > ROUNDS_BETWEEN_SETTLE_POLITICIANS) {
            int neutralECInfluence = Communication.ecInfluence[settleECLoc.x % 128][settleECLoc.y % 128];
            int poliInfluence = neutralECInfluence + SETTLE_STARTING_INFLUENCE + 10;
            poliInfluence |= 1; // odd influence
            if (buildRobot(RobotType.POLITICIAN, poliInfluence)) {
                lastRoundBuiltSettlePolitician = roundNum;
                return;
            }
        }

        MapLocation siegeECLoc = Communication.getClosestSiegeableEC(false);
        if (siegeECLoc != null && roundNum - lastRoundBuiltSiegePolitician > ROUNDS_BETWEEN_SIEGE_POLITICIANS) {
            int enemyECInfluence = Communication.ecInfluence[siegeECLoc.x % 128][siegeECLoc.y % 128];
            int poliInfluence = Math.min(rc.getInfluence() / 3, Math.max(enemyECInfluence / 3, Politician.MIN_SIEGE_MISSION_CONVICTION));
            poliInfluence |= 1; // odd influence
            if (poliInfluence > Politician.MIN_SIEGE_MISSION_CONVICTION && buildRobot(RobotType.POLITICIAN, poliInfluence)) {
                lastRoundBuiltSiegePolitician = roundNum;
                return;
            }
        }

        if (roundNum - lastRoundBuiltSlanderer > slandererGap && safeToBuildSlanderer()) {
            if (buildRobot(RobotType.SLANDERER, (int)Math.min(MAX_SLANDERER_INFLUENCE, Math.max(MIN_SLANDERER_INFLUENCE, rc.getInfluence() * SLANDERER_INFLUENCE_PERCENTAGE)))) {
                lastRoundBuiltSlanderer = roundNum;
            } else {
                buildRobot(RobotType.MUCKRAKER, 1);
            }
            return;
        }

        MapLocation closestEnemyMuckraker = Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_MUCKRAKER);
        if (roundNum - lastRoundBuiltDemuckPolitician > ROUNDS_BETWEEN_DEMUCKING_POLITICIANS) {
            int demuckInfluence = 30;
            if (closestEnemyMuckraker != null) {
                demuckInfluence = (Communication.closestEnemyInfluence[Communication.ENEMY_TYPE_MUCKRAKER] + 10) / 2 * 2; // even influence
            }
            if (buildRobot(RobotType.POLITICIAN, demuckInfluence)) {
                lastRoundBuiltDemuckPolitician = roundNum;
                return;
            }
        }

        // boolean savingForAttack = siegeECLoc != null || settleECLoc != null;

        MapLocation sleuthLocation = Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_SLANDERER);
        if (sleuthLocation != null &&
            roundNum - lastRoundBuiltSleuthingMuckraker > ROUNDS_BETWEEN_SLEUTHING_MUCKRAKERS) {
            if (buildRobot(RobotType.MUCKRAKER, SLEUTHING_MUCKRAKER_INFLUENCE)) {
                lastRoundBuiltSleuthingMuckraker = roundNum;
                return;
            }
        }

        if (Communication.hasAvailableUnitSlots) {
            if (roundNum - lastRoundBuiltExpensiveScoutingMuckraker > ROUNDS_BETWEEN_EXPENSIVE_SCOUTING_MUCKRAKERS) {
                if (buildRobot(RobotType.MUCKRAKER, Muckraker.MAX_SCOUT_INFLUENCE)) {
                    lastRoundBuiltExpensiveScoutingMuckraker = roundNum;
                    return;
                }
            }
            buildRobot(RobotType.MUCKRAKER, 1);
        }
    }

    private static final int SAFE_SLANDERER_RANGE = 200;

    private static boolean safeToBuildSlanderer() {
        if(Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_MUCKRAKER) != null && Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_MUCKRAKER).distanceSquaredTo(rc.getLocation()) < SAFE_SLANDERER_RANGE) {
            return false; 
        }
        else {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(); 
            for(int i = nearbyRobots.length - 1; i >= 0; i--) {
                if(nearbyRobots[i].type == RobotType.MUCKRAKER && rc.getTeam() != nearbyRobots[i].team) {
                    return false; 
                }
            }
        }
        return true;
    }

    private static boolean buildRobot(RobotType type, int influence) throws GameActionException {
        if(type == RobotType.SLANDERER) {
            influence |= 1; // make it odd
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
                MapLocation enemyMuckrakerLocation = Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_MUCKRAKER);
                Communication.sendMissionInfo(enemyMuckrakerLocation, missionType);
                break;
            case Communication.MISSION_TYPE_DEPOLI:
                MapLocation enemyPoliticianLocation = Communication.getClosestEnemyUnitOfType(Communication.ENEMY_TYPE_POLITICIAN);
                Communication.sendMissionInfo(enemyPoliticianLocation, missionType);
                break;
            default:
                break;
        }

        missionType = (missionType + 1) % Communication.NUM_MISSION_TYPES;
    }
}
