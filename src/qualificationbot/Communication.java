package qualificationbot;
import battlecode.common.*;

public class Communication {

    // ID Storing

    static final int MAX_ID = 4096;

    static final int MAX_NUM_FRIENDLY_MUCKRAKERS = 128;
    static int[] friendlyMuckrakerIDs = new int[MAX_NUM_FRIENDLY_MUCKRAKERS];
    static int friendlyMuckrakerIdx = 0;
    static boolean[] friendlyMuckrakerAdded = new boolean[MAX_ID];

    static int friendlyECID;

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
                        if (!isEnlightenmentCenter) {
                            friendlyECID = id;
                        }
                        break;
                    case MUCKRAKER:
                        // only ec's care about muckrakers
                        if (isEnlightenmentCenter && !friendlyMuckrakerAdded[id % MAX_ID]) {
                            friendlyMuckrakerAdded[id % MAX_ID] = true;
                            if (friendlyMuckrakerIDs[friendlyMuckrakerIdx] != 0) {
                                // reset old muckraker id availability
                                friendlyMuckrakerAdded[(friendlyMuckrakerIDs[friendlyMuckrakerIdx]) % MAX_ID] = false;
                            }
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

    // Section Robot Info

    static final int MAX_SECTION_RANGE = (SECTION_SIZE - 1) * (SECTION_SIZE - 1) * 2;
    static int[][] sectionRobotInfo = new int[NUM_SECTIONS][NUM_SECTIONS];
    static boolean[][] sectionOnEdge = new boolean[NUM_SECTIONS][NUM_SECTIONS];
    static int[][] ecInfluence = new int[NUM_SECTIONS][NUM_SECTIONS];

    static final int EC_INFLUENCE_SCALE = 50; 

    static final int MAX_NUM_SECTIONS_WITH_ROBOTS = 300;
    static MapLocation[] sectionsWithRobots = new MapLocation[MAX_NUM_SECTIONS_WITH_ROBOTS];
    static boolean[][] inSectionsWithRobotsList = new boolean[NUM_SECTIONS][NUM_SECTIONS];
    static int sectionsWithRobotsIdx = 0;

    static final int TYPE_ENLIGHTENMENT_CENTER = 0;
    static final int TYPE_MUCKRAKER = 1;
    static final int TYPE_POLITICIAN = 2;
    static final int TYPE_SLANDERER = 3;

    // called by ec, uses muckraker flags to get section info
    public static void updateSectionRobotInfo() throws GameActionException {
        for (int i = MAX_NUM_FRIENDLY_MUCKRAKERS - 1; i >= 0; i--) {
            if (friendlyMuckrakerIDs[i] != 0 && RobotPlayer.rc.canGetFlag(friendlyMuckrakerIDs[i])) {
                int flag = RobotPlayer.rc.getFlag(friendlyMuckrakerIDs[i]);
                int sectionLocNum       = flag & 0x3FF;     // first 10 bits
                int sectionOnEdgeNum    = flag >> 10 & 0x1; // next 1 bit
                int sectionInfo         = flag >> 11;       // next 4 bits
                int ecInfluenceInfo     = flag >> 15;       // last 9 bits
                int sectionX = sectionLocNum % NUM_SECTIONS;
                int sectionY = sectionLocNum / NUM_SECTIONS;
                sectionsWithRobots[sectionsWithRobotsIdx] = new MapLocation(sectionX, sectionY);
                sectionsWithRobotsIdx++;
                sectionRobotInfo[sectionX][sectionY] = sectionInfo;
                sectionOnEdge[sectionX][sectionY] = sectionOnEdgeNum == 1;
                ecInfluence[sectionX][sectionY] = ecInfluenceInfo * EC_INFLUENCE_SCALE;
                inSectionsWithRobotsList[sectionX][sectionY] = true;
            }
        }
    }

    // called by muckraker to send section info back to ec
    public static void sendSectionInfo() throws GameActionException {
        MapLocation robotLoc = RobotPlayer.rc.getLocation();
        int sectionX = (robotLoc.x % MAP_SIZE) / SECTION_SIZE;
        int sectionY = (robotLoc.y % MAP_SIZE) / SECTION_SIZE;
        int sectionLocNum = sectionX | (sectionY << 5); // first 10 bits

        int sectionOnEdgeNum = curSectionOnEdge() ? 1 : 0;

        int friendlyMuckrakerIdx = getRobotTeamAndTypeIndex(RobotPlayer.rc.getTeam(), RobotType.MUCKRAKER);
        int sectionInfo = (1 << friendlyMuckrakerIdx);
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

        RobotPlayer.rc.setFlag(sectionLocNum | (sectionOnEdgeNum << 10) | (sectionInfo << 11));
    }

    // return the actual center location of a given section
    public static MapLocation getSectionCenterLoc(MapLocation sectionLoc) {
        int moddedSectionCenterX = sectionLoc.x * SECTION_SIZE + (SECTION_SIZE / 2);
        int moddedSectionCenterY = sectionLoc.y * SECTION_SIZE + (SECTION_SIZE / 2);
        return getLocationFromModded(moddedSectionCenterX, moddedSectionCenterY);
    }

    public static MapLocation getCurrentSection() {
        MapLocation robotLoc = RobotPlayer.rc.getLocation();
        int sectionX = (robotLoc.x % MAP_SIZE) / SECTION_SIZE;
        int sectionY = (robotLoc.y % MAP_SIZE) / SECTION_SIZE;
        return new MapLocation(sectionX, sectionY);
    }

    public static boolean isRobotTeamAndTypeInSection(MapLocation sectionLoc, Team team, RobotType type) {
        int sectionInfo = sectionRobotInfo[sectionLoc.x][sectionLoc.y];
        int idx = getRobotTeamAndTypeIndex(team, type);
        return (sectionInfo & (1 << idx)) != 0;
    }

    private static int getRobotTeamAndTypeIndex(Team team, RobotType type) {
        int teamNum = getTeamNum(team);
        int typeNum = getTypeNum(type);
        return teamNum * 4 + typeNum; // max of 8 (neutral ec)
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

    private static boolean curSectionOnEdge() throws GameActionException {
        MapLocation curLoc = RobotPlayer.rc.getLocation();
        return !RobotPlayer.rc.onTheMap(new MapLocation(curLoc.x + 4, curLoc.y)) ||
               !RobotPlayer.rc.onTheMap(new MapLocation(curLoc.x - 4, curLoc.y)) ||
               !RobotPlayer.rc.onTheMap(new MapLocation(curLoc.x, curLoc.y + 4)) ||
               !RobotPlayer.rc.onTheMap(new MapLocation(curLoc.x, curLoc.y - 4));
        
    }

    // Section Mission Info
    
    // mission types - should have no more than 16
    static final int MISSION_TYPE_UNKNOWN = 0;
    
    // Muckraker Missions
    static final int MISSION_TYPE_SLEUTH = 1; // search for and kill slanderer
    static final int MISSION_TYPE_STICK = 2;  // stick to a politician
    static final int MISSION_TYPE_SCOUT = 3;  // get section info

    // Politician Missions
    static final int MISSION_TYPE_SIEGE = 4;  // attack EC
    static final int MISSION_TYPE_DEMUCK = 5; // kill muckraker

    // Slanderer Missions
    static final int MISSION_TYPE_HIDE = 6;   // hide slanderer

    static final int MAX_NUM_MISSIONS = 50;

    static int[][] sectionMissionInfo = new int[NUM_SECTIONS][NUM_SECTIONS];
    static MapLocation[] missionList = new MapLocation[MAX_NUM_MISSIONS];
    static int missionListIdx = 0;

    // called by any non-ec unit, uses ec flags to get mission info
    public static void updateSectionMissionInfo() throws GameActionException {
        for (int i = MAX_NUM_FRIENDLY_ECS - 1; i >= 0; --i) {
            if (friendlyEnlighenmentCenterIDs[i] != 0 && RobotPlayer.rc.canGetFlag(friendlyEnlighenmentCenterIDs[i])) {
                int flag = RobotPlayer.rc.getFlag(friendlyEnlighenmentCenterIDs[i]);
                int sectionLocNum   = flag & 0x3FF; // first 10 bits
                int sectionInfo     = flag >> 10; // last 14 bits (only 4 used)
                // if (sectionInfo == MISSION_TYPE_DEMUCK) {
                //     System.out.println("RECEIVED DEMUCK MESSAGE");
                // }
                if (!isMissionTypeRelevant(sectionInfo)) continue;
                int sectionX = sectionLocNum % NUM_SECTIONS;
                int sectionY = sectionLocNum / NUM_SECTIONS;
                sectionMissionInfo[sectionX][sectionY] = sectionInfo;
                missionList[missionListIdx] = new MapLocation(sectionX, sectionY);
                missionListIdx = (missionListIdx + 1) % MAX_NUM_MISSIONS;
            }
        }
    }

    public static void sendMissionInfo(MapLocation sectionLoc, int missionType) throws GameActionException {
        sectionMissionInfo[sectionLoc.x][sectionLoc.y] = missionType;
        int sectionLocNum = sectionLoc.x | (sectionLoc.y << 5); // first 10 bits
        // if (missionType == MISSION_TYPE_STICK) {
        //     // System.out.println("SENDING STICK MISSION at " + sectionLoc);
        // } else if (missionType == MISSION_TYPE_SLEUTH) {
        //     System.out.println("SENDING SLEUTH MISSION at " + sectionLoc);
        // } else if (missionType == MISSION_TYPE_SCOUT) {
        //     System.out.println("SENDING SCOUT MISSION at " + sectionLoc);
        // }
        RobotPlayer.rc.setFlag(sectionLocNum | (missionType << 10));
    }

    public static void setMissionComplete(MapLocation missionSectionLoc) {
        sectionMissionInfo[missionSectionLoc.x][missionSectionLoc.y] = MISSION_TYPE_UNKNOWN;
    }

    public static MapLocation getClosestMission() {
        MapLocation curLoc = RobotPlayer.rc.getLocation();

        int closestDist = Integer.MAX_VALUE;
        MapLocation missionSectionLoc = null;
        for (int i = MAX_NUM_MISSIONS - 1; i >= 0; i--) {
            MapLocation sectionLoc = missionList[i];
            if (sectionLoc == null || sectionMissionInfo[sectionLoc.x][sectionLoc.y] == MISSION_TYPE_UNKNOWN) continue;
            MapLocation sectionCenterLoc = getSectionCenterLoc(sectionLoc);
            if (curLoc.isWithinDistanceSquared(sectionCenterLoc, closestDist - 1)) {
                closestDist = curLoc.distanceSquaredTo(sectionCenterLoc);
                missionSectionLoc = sectionLoc;
            }
        }

        return missionSectionLoc;
    }

    private static boolean isMissionTypeRelevant(int missionType) {
        switch (missionType) {
            case MISSION_TYPE_SLEUTH:
            case MISSION_TYPE_SCOUT:
            case MISSION_TYPE_STICK:
                return RobotPlayer.rc.getType() == RobotType.MUCKRAKER;
            case MISSION_TYPE_DEMUCK:
            case MISSION_TYPE_SIEGE:
                return RobotPlayer.rc.getType() == RobotType.POLITICIAN;
            case MISSION_TYPE_HIDE:
                return RobotPlayer.rc.getType() == RobotType.SLANDERER;
            default:
                return false;
        }
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
