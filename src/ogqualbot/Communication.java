package ogqualbot;
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
            if (sensedRobots[i].getTeam() != friendlyTeam) continue;
            int id = sensedRobots[i].getID();
            switch (sensedRobots[i].getType()) {
                case ENLIGHTENMENT_CENTER:
                    // only non-ec's care about ec's
                    if (!isEnlightenmentCenter) {
                        friendlyECID = id;
                    }
                    break;
                case POLITICIAN:
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
    static final int MAX_EC_INFLUENCE_STORED = 25550;

    static final int MAX_NUM_SECTIONS_WITH_ROBOTS = 300;
    static MapLocation[] sectionsWithRobots = new MapLocation[MAX_NUM_SECTIONS_WITH_ROBOTS];
    static boolean[][] inSectionsWithRobotsList = new boolean[NUM_SECTIONS][NUM_SECTIONS];
    static int sectionsWithRobotsIdx = 0;

    static final int TYPE_ENLIGHTENMENT_CENTER = 0;
    static final int TYPE_MUCKRAKER = 1;
    static final int TYPE_POLITICIAN = 2;
    static final int TYPE_SLANDERER = 3;

    public static MapLocation getClosestSectionWithType(RobotType type) {
        MapLocation robotLoc = RobotPlayer.rc.getLocation();
        int closestDist = Integer.MAX_VALUE;
        MapLocation closestSectionLocation = null;
        for (int i = sectionsWithRobotsIdx - 1; i >= 0; i--) {
            MapLocation sectionCenterLoc = getSectionCenterLoc(sectionsWithRobots[i]);
            if (isTypeInSection(sectionsWithRobots[i], type) && 
                sectionCenterLoc.isWithinDistanceSquared(robotLoc, closestDist - 1)) {
                closestSectionLocation = sectionsWithRobots[i];
                closestDist = sectionCenterLoc.distanceSquaredTo(robotLoc);
            }
        }
        return closestSectionLocation;
    }

    // called by ec, uses muckraker flags to get section info
    public static void updateSectionRobotInfo() throws GameActionException {
        for (int i = MAX_NUM_FRIENDLY_MUCKRAKERS - 1; i >= 0; i--) {
            if (friendlyMuckrakerIDs[i] != 0 && RobotPlayer.rc.canGetFlag(friendlyMuckrakerIDs[i])) {
                int flag = RobotPlayer.rc.getFlag(friendlyMuckrakerIDs[i]);
                int sectionLocNum       = flag & 0x3FF;     // first 10 bits
                int sectionOnEdgeNum    = flag >> 10 & 0x1; // next 1 bit
                int sectionInfo         = flag >> 11 & 0xF; // next 4 bits
                int ecInfluenceInfo     = flag >> 15;       // last 9 bits
                int sectionX = sectionLocNum % NUM_SECTIONS;
                int sectionY = sectionLocNum / NUM_SECTIONS;
                if (!inSectionsWithRobotsList[sectionX][sectionY]) {
                    sectionsWithRobots[sectionsWithRobotsIdx] = new MapLocation(sectionX, sectionY);
                    sectionsWithRobotsIdx++;
                    inSectionsWithRobotsList[sectionX][sectionY] = true;
                }
                sectionRobotInfo[sectionX][sectionY] = sectionInfo;
                sectionOnEdge[sectionX][sectionY] = sectionOnEdgeNum == 1;
                ecInfluence[sectionX][sectionY] = ecInfluenceInfo * EC_INFLUENCE_SCALE;
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

        int sectionRobotInfo = 0;
        int ecInfluenceInfo = 0;
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots(MAX_SECTION_RANGE);
        for (int i = sensedRobots.length - 1; i >= 0; --i) {
            if (sensedRobots[i].getTeam() == RobotPlayer.rc.getTeam()) continue;

            MapLocation loc = sensedRobots[i].getLocation();
            // check if in section
            if ((loc.x % MAP_SIZE) / SECTION_SIZE == sectionX && (loc.y % MAP_SIZE) / SECTION_SIZE == sectionY) {
                // add robot type info to section num
                int idx = getTypeNum(sensedRobots[i].getType());
                sectionRobotInfo |= (1 << idx);
                if (sensedRobots[i].getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    ecInfluenceInfo = ((Math.min(sensedRobots[i].getInfluence(), MAX_EC_INFLUENCE_STORED) - 1) / 50 + 1);
                }
            }
        }

        RobotPlayer.rc.setFlag(sectionLocNum |
                              (sectionOnEdgeNum << 10) |
                              (sectionRobotInfo << 11) |
                              (ecInfluenceInfo  << 15));
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

    public static boolean isTypeInSection(MapLocation sectionLoc, RobotType type) {
        int idx = getTypeNum(type);
        return (sectionRobotInfo[sectionLoc.x][sectionLoc.y] & (1 << idx)) != 0;
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
    
    static int NUM_MISSION_TYPES = 4;

    // mission types
    static final int MISSION_TYPE_UNKNOWN = 0;
    static final int MISSION_TYPE_SLEUTH = 1; // search for and kill slanderer
    static final int MISSION_TYPE_SIEGE = 2;  // attack EC
    static final int MISSION_TYPE_HIDE = 3;   // hide slanderer

    static final int NO_MISSION_AVAILABLE = 1 << 10;

    static MapLocation[] latestMissionSectionLoc = new MapLocation[NUM_MISSION_TYPES];

    // called by any non-ec unit, uses ec flags to get mission info
    public static void updateSectionMissionInfo() throws GameActionException {
        if (RobotPlayer.rc.canGetFlag(friendlyECID)) {
            int flag = RobotPlayer.rc.getFlag(friendlyECID);

            int missionType         = flag & 0x3;   // first 2 bits (only 2 used)
            int missionSectionNum   = flag >> 2;    // last 22 bits (only 10 used unless sending a no mission available message)
            latestMissionSectionLoc[missionType] = null;
            if (missionSectionNum != NO_MISSION_AVAILABLE) {
                latestMissionSectionLoc[missionType] = new MapLocation(missionSectionNum % NUM_SECTIONS, missionSectionNum / NUM_SECTIONS);
            }
        }
    }

    public static void sendMissionInfo(MapLocation sectionLoc, int missionType) throws GameActionException {
        int sectionLocNum = sectionLoc == null ? NO_MISSION_AVAILABLE : sectionLoc.x | (sectionLoc.y << 5);
        latestMissionSectionLoc[missionType] = sectionLoc;
        RobotPlayer.rc.setFlag(missionType | (sectionLocNum << 2));
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
