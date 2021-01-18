package sprintbot;
import battlecode.common.*;

public class Communication {

    private static final int MAX_SIZE = 4096;

    static int[] friendlyRobotIDs = new int[MAX_SIZE];
    static int numFriendlyRobotIDs = 0;
    static boolean[] added = new boolean[MAX_SIZE];

    static void updateNearbyRobots() {
        Team friendlyTeam = RobotPlayer.rc.getTeam();
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots();
        for (int i = sensedRobots.length; i >= 0; --i) {
            int id = sensedRobots[i].getID();
            if (sensedRobots[i].getTeam() == friendlyTeam && !added[id % MAX_SIZE]) {
                added[id % MAX_SIZE] = true;
                friendlyRobotIDs[numFriendlyRobotIDs++] = id;
            }
            updateNearbyRobot(sensedRobots[i].getLocation(), sensedRobots[i].getTeam(), sensedRobots[i].getType());
        }
    }

    private static void updateNearbyRobot(MapLocation robotLoc, Team robotTeam, RobotType robotType) {
        int teamNum = getRobotTeamNum(robotTeam);
        int typeNum = getRobotTypeNum(robotType);
        
        MapLocation closestRobotLoc = closestRobotLocs[teamNum][typeNum];
        MapLocation curLoc = RobotPlayer.rc.getLocation();
        if (closestRobotLoc == null || robotLoc.distanceSquaredTo(curLoc) < closestRobotLoc.distanceSquaredTo(curLoc)) {
            closestRobotLocs[teamNum][typeNum] = robotLoc;
        }
    }

    static MapLocation[][] closestRobotLocs = new MapLocation[3][4];

    public static final int TYPE_NUM_ENLIGHTENMENT_CENTER = 0;
    public static final int TYPE_NUM_MUCKRAKER = 1;
    public static final int TYPE_NUM_POLITICIAN = 2;
    public static final int TYPE_NUM_SLANDERER = 3;

    public static final int TEAM_NUM_A = 0;
    public static final int TEAM_NUM_B = 1;
    public static final int TEAM_NUM_NEUTRAL = 2;

    private static final int BIT_SIZE_TARGET_LOC = 14;
    private static final int BIT_SIZE_COORD = BIT_SIZE_TARGET_LOC / 2;
    private static final int BIT_MASK_TARGET_LOC = (1 << BIT_SIZE_TARGET_LOC) - 1;
    private static final int BIT_MASK_COORD = (1 << BIT_SIZE_COORD) - 1;

    private static final int BIT_SIZE_TARGET_TEAM = 2;
    private static final int BIT_MASK_TARGET_TEAM = (1 << BIT_SIZE_TARGET_TEAM) - 1;

    private static final int MAX_MAP_SIZE = 64;

    // Read Methods

    static void readFlags() throws GameActionException {
        for (int i = numFriendlyRobotIDs - 1; i >= 0; --i) {
            if (RobotPlayer.rc.canGetFlag(friendlyRobotIDs[i])) {
                readFlag(RobotPlayer.rc.getFlag(friendlyRobotIDs[i]));
            }
        }
    }

    private static void readFlag(int flag) {
        MapLocation targetLoc = getRobotLocation(flag & BIT_MASK_TARGET_LOC);
        Team targetTeam = getRobotTeam((flag >> BIT_SIZE_TARGET_LOC) & BIT_MASK_TARGET_TEAM);
        RobotType targetRobotType = getRobotType(flag >> (BIT_SIZE_TARGET_LOC + BIT_SIZE_TARGET_TEAM));
        updateNearbyRobot(targetLoc, targetTeam, targetRobotType);
    }

    private static Team getRobotTeam(int teamNum) {
        switch (teamNum) {
            case TEAM_NUM_A:
                return Team.A;
            case TEAM_NUM_B:
                return Team.B;
            case TEAM_NUM_NEUTRAL:
                return Team.NEUTRAL;
            default:
                return null;
        }
    }

    private static RobotType getRobotType(int robotTypeNum) {
        switch (robotTypeNum) {
            case TYPE_NUM_ENLIGHTENMENT_CENTER:
                return RobotType.ENLIGHTENMENT_CENTER;
            case TYPE_NUM_MUCKRAKER:
                return RobotType.MUCKRAKER;
            case TYPE_NUM_POLITICIAN:
                return RobotType.POLITICIAN;
            case TYPE_NUM_SLANDERER:
                return RobotType.SLANDERER;
            default:
                return null;
        }
    }

    private static MapLocation getRobotLocation(int targetLocNum) {
        MapLocation curLoc = RobotPlayer.rc.getLocation();

        int xModded = targetLocNum & 0x7F;
        int yModded = targetLocNum >> 7;
        int curXModded = curLoc.x & BIT_MASK_COORD;
        int curYModded = curLoc.y & BIT_MASK_COORD;

        int targetX;
        int xDiff = (xModded - curXModded + (MAX_MAP_SIZE / 2)) & BIT_MASK_COORD;
        if (xDiff < MAX_MAP_SIZE) {
            targetX = curLoc.x + xDiff;
        } else {
            targetX = curLoc.x - ((MAX_MAP_SIZE / 2) - xDiff);
        }

        int targetY;
        int yDiff = (yModded - curYModded + (MAX_MAP_SIZE / 2)) & BIT_MASK_COORD;
        if (yDiff < MAX_MAP_SIZE) {
            targetY = curLoc.y + yDiff;
        } else {
            targetY = curLoc.y - ((MAX_MAP_SIZE / 2) - yDiff);
        }

        return new MapLocation(targetX, targetY);
    }

    // Send Methods

    static void sendTargetFlag(MapLocation targetLoc, Team targetTeam, RobotType targetRobotType) throws GameActionException {
        int targetLocNum = getRobotLocNum(targetLoc);
        int targetTeamNum = getRobotTeamNum(targetTeam);
        int targetRobotTypeNum = getRobotTypeNum(targetRobotType);
        int flag = targetLocNum | (targetTeamNum << BIT_SIZE_TARGET_LOC) | (targetRobotTypeNum << (BIT_SIZE_TARGET_LOC + BIT_SIZE_TARGET_TEAM));
        RobotPlayer.rc.setFlag(flag);
    }

    private static int getRobotTeamNum(Team targetTeam) {
        switch (targetTeam) {
            case A:
                return TEAM_NUM_A;
            case B:
                return TEAM_NUM_B;
            case NEUTRAL:
                return TEAM_NUM_NEUTRAL;
            default:
                return -1;
        }
    }

    private static int getRobotTypeNum(RobotType targetRobotType) {
        switch (targetRobotType) {
            case ENLIGHTENMENT_CENTER:
                return 0;
            case MUCKRAKER:
                return 1;
            case POLITICIAN:
                return 2;
            case SLANDERER:
                return 3;
            default:
                return -1;
        }
    }

    private static int getRobotLocNum(MapLocation targetLoc) {
        return (targetLoc.x & BIT_MASK_COORD) | ((targetLoc.y & BIT_MASK_COORD) << 7);
    }
}
