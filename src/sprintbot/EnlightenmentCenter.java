package sprintbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.1;
    static final int MAX_SLANDERER_INFLUENCE = 949;
    public static final double[] BID_PERCENTAGES = new double[] {.05, .1, .125, .15, .2};
    static final int NUM_ROUNDS = 1500
    static int slanderersBuilt; 
    static int politiciansBuilt; 
    static Direction slandererBuildDir; 
    static Direction poliBuildDir; 
    static MapLocation ecLoc; 
    static final Direction[] MUCKRACKER_BUILD_DIRECTIONS = new Direction[]{DIRECTION.NORTHEAST, DIRECTION.NORTHWEST, DIRECTION.WEST, DIRECTION.SOUTHWEST, DIRECTION.SOUTH, DIRECTION.SOUTHEAST}

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

    static int[] flags = new int[10];

    public static void initialize() {
        // throw new UnsupportedOperationException();
        slanderersBuilt = 0; 
        politiciansBuilt = 0; 
        slandererBuildDir = Direction.EAST;
        poliBuildDir = Direction.NORTH; 
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
      //  buildRandomRobot();
        buildRobot(); 
        bid(); 
        // TODO send missions
    }

    private static void bid() {
        if(rc.getTeamVotes() < NUM_ROUNDS / 2 + 1) {
            if(rc.canBid(rc.getInfluence() * BID_PERCENTAGES[rc.getRoundNum() / BID_PERCENTAGES.length]){
                rc.bid(rc.getInfluence() * BID_PERCENTAGES[rc.getRoundNum() / BID_PERCENTAGES.length])
            }
                
        }
    
    }
    private static void buildRobot() {
        //build a slanderer every 50 turns with half influence, up to MAX_SLANDERER_INFLUENCE
        if (slanderersBuilt <= turnCount / 50 && rc.canBuildRobot(RobotType.SLANDERER, slandererBuildDir, 1)) { 
            int influence = Math.min(MAX_SLANDERER_INFLUENCE, rc.getInfluence() / 2);
            rc.buildRobot(RobotType.SLANDERER, slandererBuildDir, influence);
            slanderersBuilt++;
            return;
        }
        //build a politician every 20 turns with 20% of our influence. 
        else if (politiciansBuilt <= turnCount / 20 && rc.canBuildRobot(RobotType.SLANDERER, poliBuildDir, 1 ) ){
            int influence = rc.getInfluence() / 5; 
            rc.buildRobot(RobotType.POLITICIAN, poliBuildDir, influence); 
            politiciansBuilt++; 
            return; 
        }
        //if we are not building a slanderer or poli we should always be producing muckrakers. 
        else {
            for(int i = MUCKRACKER_BUILD_DIRECTIONS.length - 1; i >= 0; i--){
                if(rc.canBuildRobot(RobotType.MUCKRAKER, MUCKRACKER_BUILD_DIRECTIONS[i], 1)) {
                    rc.buildRobot(RobotType.MUCKRAKER, MUCKRACKER_BUILD_DIRECTIONS[i], 1); 
                    break;
                }
            }
        }

    
    }

    private static void buildRandomRobot() throws GameActionException {
        int rand = (int)(Math.random() * 3);
        switch (rand) {
            case 0:
                buildRobot(RobotType.MUCKRAKER);
                break;
            case 1:
                buildRobot(RobotType.SLANDERER);
                break;
            case 2:
                buildRobot(RobotType.POLITICIAN);
                break;
            default:
                break;
        }
    }

    private static boolean buildRobot(RobotType type) throws GameActionException {
        Direction[] allDirections = Direction.allDirections();
        for (int i = allDirections.length - 1; i >= 0; --i) {
            Direction buildDir = allDirections[i];
            int influence = getInvestedInfluence(type);
            if (rc.canBuildRobot(type, buildDir, influence)) {
                rc.buildRobot(type, buildDir, influence);
                return true;
            }
        }
        return false;
    }

    private static int getInvestedInfluence(RobotType type) {
        switch (type) {
            case MUCKRAKER:
                return MUCKRAKER_INFLUENCE;
            case POLITICIAN:
                return POLITICIAN_INFLUENCE;
            case SLANDERER:
                return Math.max((int)(rc.getInfluence() * SLANDERER_INFLUENCE_PERCENTAGE), MAX_SLANDERER_INFLUENCE);
            default:
                return 0;
        }
    }
}
