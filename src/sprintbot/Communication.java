package sprintbot;
import battlecode.common.*;

public class Communication {

    // TODO check if section is on an edge (one of edge locations is off the map) and send as part of section info

    // ID Storing

    static final int MAX_ID = 4096;

    static final int MAX_NUM_FRIENDLY_MUCKRAKERS = 128;
    static int[] friendlyMuckrakerIDs = new int[MAX_NUM_FRIENDLY_MUCKRAKERS];
    static int friendlyMuckrakerIdx = 0;
    static boolean[] friendlyMuckrakerAdded = new boolean[MAX_ID];

    static final int MAX_NUM_FRIENDLY_ECS = 8;
    static int[] friendlyEnlighenmentCenterIDs = new int[MAX_NUM_FRIENDLY_ECS];
    static int friendlyEnlighenmentCenterIdx = 0;
    static boolean[] friendlyEnlighenmentCenterAdded = new boolean[MAX_ID];

    // called by all units to update id lists
    public static void updateIDList(boolean isEnlightenmentCenter) {
        Team friendlyTeam = RobotPlayer.rc.getTeam();
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots();
        for (int i = sensedRobots.length - 1; i >= 0; --i) {
            if (sensedRobots[i].getTeam() == friendlyTeam) {
                int id = sensedRobots[i].getID();
                switch (sensedRobots[i].getType()) {
                    case ENLIGHTENMENT_CENTER:
                        // only non-ec's care about ec's
                        if (!isEnlightenmentCenter && !friendlyEnlighenmentCenterAdded[id % MAX_ID]) {
                            friendlyEnlighenmentCenterAdded[id % MAX_ID] = true;
                            friendlyEnlighenmentCenterIDs[friendlyEnlighenmentCenterIdx] = id;
                            friendlyEnlighenmentCenterIdx = (friendlyEnlighenmentCenterIdx + 1) % MAX_NUM_FRIENDLY_ECS;
                        }
                        break;
                    case MUCKRAKER:
                        // only ec's care about muckrakers
                        if (isEnlightenmentCenter && !friendlyMuckrakerAdded[id % MAX_ID]) {
                            friendlyMuckrakerAdded[id % MAX_ID] = true;
                            friendlyMuckrakerIDs[friendlyMuckrakerIdx] = id;
                            friendlyMuckrakerIdx = (friendlyMuckrakerIdx + 1) % MAX_NUM_FRIENDLY_MUCKRAKERS;
                            System.out.println("found a friendly muckraker: " + id);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // Sections

    static final int MAP_SIZE = 128;
    static final int SECTION_SIZE = 4;
    static final int NUM_SECTIONS = MAP_SIZE / SECTION_SIZE;

    // Section Robot Info

    static final int MAX_SECTION_RANGE = (SECTION_SIZE - 1) * (SECTION_SIZE - 1) * 2;
    static int[][] sectionRobotInfo = new int[NUM_SECTIONS][NUM_SECTIONS];

    static final int TEAM_A = 0;
    static final int TEAM_B = 1;
    static final int TEAM_NEUTRAL = 2;

    static final int TYPE_ENLIGHTENMENT_CENTER = 0;
    static final int TYPE_MUCKRAKER = 1;
    static final int TYPE_POLITICIAN = 2;
    static final int TYPE_SLANDERER = 3;

    // called by ec, uses muckraker flags to get section info
    public static void updateSectionRobotInfo() throws GameActionException {
        for (int i = MAX_NUM_FRIENDLY_MUCKRAKERS - 1; i >= 0; i--) {
            if (friendlyMuckrakerIDs[i] != 0 && RobotPlayer.rc.canGetFlag(friendlyMuckrakerIDs[i])) {
                int flag = RobotPlayer.rc.getFlag(friendlyMuckrakerIDs[i]);
                int sectionLocNum   = flag & 0x3FF; // first 10 bits
                int sectionInfo     = flag >> 10;   // last 14 bits (only 9 bits used)
                sectionRobotInfo[sectionLocNum % NUM_SECTIONS][sectionLocNum / NUM_SECTIONS] = sectionInfo;
            }
        }
    }

    // called by muckraker to send section info back to ec
    public static void sendSectionInfo() throws GameActionException {
        MapLocation robotLoc = RobotPlayer.rc.getLocation();
        int sectionX = (robotLoc.x % MAP_SIZE) / SECTION_SIZE;
        int sectionY = (robotLoc.y % MAP_SIZE) / SECTION_SIZE;
        int sectionLocNum = sectionX | (sectionY << 5); // first 10 bits

        int sectionInfo = 0;
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots(MAX_SECTION_RANGE);
        for (int i = sensedRobots.length - 1; i >= 0; --i) {
            MapLocation loc = sensedRobots[i].getLocation();
            // check if in section
            if ((loc.x % MAP_SIZE) / SECTION_SIZE == sectionX && (loc.y % MAP_SIZE) / SECTION_SIZE == sectionY) {
                // add robot team and type info to section num
                int idx = getRobotTeamAndTypeIndex(sensedRobots[i].getTeam(), sensedRobots[i].getType());
                sectionInfo |= (1 << idx);
            }
        }

        RobotPlayer.rc.setFlag(sectionLocNum | (sectionInfo << 10));
    }

    // return the actual center location of a given section
    public static MapLocation getSectionCenterLoc(int sectionX, int sectionY) {
        int moddedSectionCenterX = sectionX * SECTION_SIZE + (SECTION_SIZE / 2);
        int moddedSectionCenterY = sectionY * SECTION_SIZE + (SECTION_SIZE / 2);
        return getLocationFromModded(moddedSectionCenterX, moddedSectionCenterY);
    }

    public static MapLocation getCurrentSection() {
        MapLocation robotLoc = RobotPlayer.rc.getLocation();
        int sectionX = (robotLoc.x % MAP_SIZE) / SECTION_SIZE;
        int sectionY = (robotLoc.y % MAP_SIZE) / SECTION_SIZE;
        return new MapLocation(sectionX, sectionY);
    }

    public static boolean isRobotTeamAndTypeInSection(int sectionX, int sectionY, Team team, RobotType type) {
        int sectionInfo = sectionRobotInfo[sectionX][sectionY];
        int idx = getRobotTeamAndTypeIndex(team, type);
        return (sectionInfo & (1 << idx)) != 0;
    }

    private static int getRobotTeamAndTypeIndex(Team team, RobotType type) {
        int teamNum = getTeamNum(team);
        int typeNum = getTypeNum(type);
        return teamNum * 3 + typeNum; // max of 8 (neutral ec)
    }

    private static int getTeamNum(Team team) {
        switch (team) {
            case A:
                return TEAM_A;
            case B:
                return TEAM_B;
            case NEUTRAL:
                return TEAM_NEUTRAL;
            default:
                return -1;
        }
    }

    private static int getTypeNum(RobotType type) {
        switch (type) {
            case ENLIGHTENMENT_CENTER:
                return TYPE_ENLIGHTENMENT_CENTER;
            case MUCKRAKER:
                return TYPE_MUCKRAKER;
            case SLANDERER:
                return TYPE_SLANDERER;
            case POLITICIAN:
                return TYPE_POLITICIAN;
            default:
                return -1;
        }
    }

    // Section Mission Info

    static final int MISSION_DURATION = 500;
    
    // mission types - should have no more than 16
    static final int MISSION_TYPE_UNKNOWN = 0;
    static final int MISSION_TYPE_SLEUTH = 1; //kill slanderer
    static final int MISSION_TYPE_SIEGE = 2;  //attack EC
    static final int MISSION_TYPE_DEMUCK = 3; //kill muckraker
    static final int MISSION_TYPE_STICK = 4;  
    static final int MISSION_TYPE_HIDE = 5;  //hid slanderer
    static final int MISSION_TYPE_SCOUT = 6; 

    static int[][] sectionMissionInfo = new int[NUM_SECTIONS][NUM_SECTIONS];
    static int[][] roundMissionAssigned = new int[NUM_SECTIONS][NUM_SECTIONS];

    // called by any non-ec unit, uses ec flags to get mission info
    public static void updateSectionMissionInfo() throws GameActionException {
        for (int i = MAX_NUM_FRIENDLY_ECS - 1; i >= 0; --i) {
            if (friendlyEnlighenmentCenterIDs[i] != 0 && RobotPlayer.rc.canGetFlag(friendlyEnlighenmentCenterIDs[i])) {
                int flag = RobotPlayer.rc.getFlag(friendlyEnlighenmentCenterIDs[i]);
                int sectionLocNum   = flag & 0x3FF; // first 10 bits
                int sectionInfo     = flag >> 10; // last 14 bits (only 4 used)
                int sectionX = sectionLocNum % NUM_SECTIONS;
                int sectionY = sectionLocNum / NUM_SECTIONS;
                sectionMissionInfo[sectionX][sectionY] = sectionInfo;
                roundMissionAssigned[sectionX][sectionY] = RobotPlayer.rc.getRoundNum();
            }
        }
    }

    public static void sendMissionInfo(int sectionX, int sectionY, int missionType) throws GameActionException {
        sectionMissionInfo[sectionX][sectionY] = missionType;
        roundMissionAssigned[sectionX][sectionY] = RobotPlayer.rc.getRoundNum();
        int sectionLocNum = sectionX | (sectionY << 5); // first 10 bits
        RobotPlayer.rc.setFlag(sectionLocNum | (missionType << 10));
    }

    public static int getMissionTypeInSection(int sectionX, int sectionY) {
        if (RobotPlayer.rc.getRoundNum() - roundMissionAssigned[sectionX][sectionY] > MISSION_DURATION) {
            return MISSION_TYPE_UNKNOWN;
        }
        return sectionMissionInfo[sectionX][sectionY];
    }

    // Utilities

    private static MapLocation getLocationFromModded(int moddedX, int moddedY) {
        MapLocation curLoc = RobotPlayer.rc.getLocation();

        int curXModded = curLoc.x % MAP_SIZE;
        int curYModded = curLoc.y % MAP_SIZE;

        int targetX;
        int xDiff = (moddedX - curXModded + MAP_SIZE) % MAP_SIZE;
        if (xDiff < MAP_SIZE / 2) {
            targetX = curLoc.x + xDiff;
        } else {
            targetX = curLoc.x - (MAP_SIZE - xDiff);
        }

        int targetY;
        int yDiff = (moddedY - curYModded + MAP_SIZE) % MAP_SIZE;
        if (yDiff < MAP_SIZE / 2) {
            targetY = curLoc.y + yDiff;
        } else {
            targetY = curLoc.y - (MAP_SIZE - yDiff);
        }

        return new MapLocation(targetX, targetY);
    }
}
