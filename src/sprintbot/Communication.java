package sprintbot;
import battlecode.common.*;
import javafx.application.Preloader.StateChangeNotification;

public class Communication {

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

    public static void updateNearbyRobots() {
        Team friendlyTeam = RobotPlayer.rc.getTeam();
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots();
        for (int i = sensedRobots.length; i >= 0; --i) {
            if (sensedRobots[i].getTeam() == friendlyTeam) {
                int id = sensedRobots[i].getID() % MAX_ID;
                switch (sensedRobots[i].getType()) {
                    case ENLIGHTENMENT_CENTER:
                        if (!friendlyEnlighenmentCenterAdded[id]) {
                            friendlyEnlighenmentCenterAdded[id] = true;
                            friendlyEnlighenmentCenterIDs[friendlyEnlighenmentCenterIdx] = id;
                            friendlyEnlighenmentCenterIdx = (friendlyEnlighenmentCenterIdx + 1) % MAX_NUM_FRIENDLY_ECS;
                        }
                        break;
                    case MUCKRAKER:
                        if (!friendlyMuckrakerAdded[id]) {
                            friendlyMuckrakerAdded[id] = true;
                            friendlyMuckrakerIDs[friendlyMuckrakerIdx] = id;
                            friendlyMuckrakerIdx = (friendlyMuckrakerIdx + 1) % MAX_NUM_FRIENDLY_MUCKRAKERS;
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
    static final int MAX_SECTION_RANGE = (SECTION_SIZE - 1) * (SECTION_SIZE - 1) * 2;
    static int[][] sectionRobotInfo = new int[NUM_SECTIONS][NUM_SECTIONS];

    static final int TEAM_A = 0;
    static final int TEAM_B = 1;
    static final int TEAM_NEUTRAL = 2;

    static final int TYPE_ENLIGHTENMENT_CENTER = 0;
    static final int TYPE_MUCKRAKER = 1;
    static final int TYPE_POLITICIAN = 2;
    static final int TYPE_SLANDERER = 3;

    // return the actual center location of a given section
    public static MapLocation getSectionCenterLoc(MapLocation sectionLoc) {
        MapLocation moddedLoc = new MapLocation(sectionLoc.x * SECTION_SIZE + (SECTION_SIZE / 2),
                                                sectionLoc.y * SECTION_SIZE + (SECTION_SIZE / 2));
        return getLocationFromModded(moddedLoc);
    }

    // called by ec, uses muckraker flags to get section info
    public static void updateSections() throws GameActionException {
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
        MapLocation sectionLoc = new MapLocation((robotLoc.x % MAP_SIZE) / SECTION_SIZE, (robotLoc.y % MAP_SIZE) / SECTION_SIZE);
        int sectionLocNum = sectionLoc.x | (sectionLoc.y << 5); // first 10 bits

        int sectionInfo = 0;
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots(MAX_SECTION_RANGE);
        for (int i = sensedRobots.length - 1; i >= 0; --i) {
            MapLocation loc = sensedRobots[i].getLocation();
            // check if in section
            if ((loc.x % MAP_SIZE) / SECTION_SIZE == sectionLoc.x && (loc.y % MAP_SIZE) / SECTION_SIZE == sectionLoc.y) {
                // add robot team and type info to section num
                int idx = getRobotTeamAndTypeIndex(sensedRobots[i].getTeam(), sensedRobots[i].getType());
                sectionInfo |= (1 << idx);
            }
        }

        RobotPlayer.rc.setFlag(sectionLocNum | (sectionInfo << 10));
    }

    public static boolean isRobotTeamAndTypeInSection(MapLocation sectionLoc, Team team, RobotType type) {
        int sectionInfo = sectionRobotInfo[sectionLoc.x][sectionLoc.y];
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

    // Missions

    // called by muckraker, uses ec flags to get mission info
    public static void updateMissions() {

    }

    private static MapLocation getLocationFromModded(MapLocation moddedLoc) {
        MapLocation curLoc = RobotPlayer.rc.getLocation();

        int curXModded = curLoc.x % MAP_SIZE;
        int curYModded = curLoc.y % MAP_SIZE;

        int targetX;
        int xDiff = (moddedLoc.x - curXModded + MAP_SIZE) % MAP_SIZE;
        if (xDiff < MAP_SIZE / 2) {
            targetX = curLoc.x + xDiff;
        } else {
            targetX = curLoc.x - (MAP_SIZE - xDiff);
        }

        int targetY;
        int yDiff = (moddedLoc.y - curYModded + MAP_SIZE) % MAP_SIZE;
        if (yDiff < MAP_SIZE / 2) {
            targetY = curLoc.y + yDiff;
        } else {
            targetY = curLoc.y - (MAP_SIZE - yDiff);
        }

        return new MapLocation(targetX, targetY);
    }
}
