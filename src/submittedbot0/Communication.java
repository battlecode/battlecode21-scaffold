package submittedbot0;

import battlecode.common.*;
import java.util.*;

public class Communication {

    // ID Storing

    static final int MAX_ID = 4096;

    static final int MAX_NUM_FRIENDLY_UNITS = 100;
    static int[] friendlyUnitIDs = new int[MAX_NUM_FRIENDLY_UNITS];
    static boolean[] friendlyUnitAdded = new boolean[MAX_ID];

    static final int MAX_NUM_FRIENDLY_ECS = 30;
    static MapLocation[] friendlyECLocations = new MapLocation[MAX_NUM_FRIENDLY_ECS];
    static int[] friendlyECIDs = new int[MAX_NUM_FRIENDLY_ECS];
    static int friendlyECIdx = 0;
    static boolean[] friendlyECAdded = new boolean[MAX_ID];

    static boolean hasAvailableUnitSlots = true;

    // called by ec to update id lists
    public static void ecUpdateIDList() {
        Team friendlyTeam = RobotPlayer.rc.getTeam();
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots();
        int nextIdx = 0;
        hasAvailableUnitSlots = true;
        for (int i = sensedRobots.length - 1; i >= 0; --i) {
            int id = sensedRobots[i].getID();
            if (sensedRobots[i].getTeam() == friendlyTeam &&
                (sensedRobots[i].getType() == RobotType.MUCKRAKER || sensedRobots[i].getType() == RobotType.POLITICIAN) &&
                !friendlyUnitAdded[id % MAX_ID]) {
                while (nextIdx < MAX_NUM_FRIENDLY_UNITS && RobotPlayer.rc.canGetFlag(friendlyUnitIDs[nextIdx])) {
                    nextIdx++;
                }
                if (nextIdx == MAX_NUM_FRIENDLY_UNITS) {
                    hasAvailableUnitSlots = false;
                    break;
                }

                friendlyUnitAdded[id % MAX_ID] = true;
                friendlyUnitIDs[nextIdx++] = id;
            }
        }
    }

    public static void updateIDList() {
        Team friendlyTeam = RobotPlayer.rc.getTeam();
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots();
        for (int i = sensedRobots.length - 1; i >= 0; --i) {
            int id = sensedRobots[i].getID();
            if (sensedRobots[i].getTeam() == friendlyTeam &&
                sensedRobots[i].getType() == RobotType.ENLIGHTENMENT_CENTER &&
                !friendlyECAdded[id % MAX_ID]) {
                    friendlyECLocations[friendlyECIdx] = sensedRobots[i].getLocation();
                    friendlyECIDs[friendlyECIdx] = id;
                    friendlyECIdx++; 
                    friendlyECAdded[id % MAX_ID] = true;
            }
        }
    }

    // Section Robot Info

    static final int MAP_SIZE = 128;
    static final int MAX_NUM_SIEGE_LOCATIONS = 12;

    static final int EC_INFLUENCE_SCALE = 50;
    static final int ENEMY_INFLUENCE_SCALE = 10;
    static final int MAX_INFLUENCE_INFO_STORED = 0x7F;

    static final int EC_TYPE_UNKNOWN = 0;
    static final int EC_TYPE_FRIENDLY = 1;
    static final int EC_TYPE_NEUTRAL = 2;
    static final int EC_TYPE_ENEMY = 3;

    static MapLocation[] siegeLocations = new MapLocation[MAX_NUM_SIEGE_LOCATIONS];
    static int siegeLocationsIdx = 0;
    static int[][] siegeableECAtLocation = new int[MAP_SIZE][MAP_SIZE];
    static int[][] ecInfluence = new int[MAP_SIZE][MAP_SIZE];

    static final int ENEMY_TYPE_UNKNOWN = 0;
    static final int ENEMY_TYPE_MUCKRAKER = 1;
    static final int ENEMY_TYPE_POLITICIAN = 2;
    static final int ENEMY_TYPE_SLANDERER = 3;

    static final int NUM_ENEMY_UNIT_TYPES = 4;

    static int[] closestEnemyDist = new int[NUM_ENEMY_UNIT_TYPES];
    static MapLocation[] closestEnemyLoc = new MapLocation[NUM_ENEMY_UNIT_TYPES];
    static int[] closestEnemyInfluence = new int[NUM_ENEMY_UNIT_TYPES];

    static int numEnemyUnits;
    static MapLocation averageEnemyLocation;

    // called by ec, uses muckraker flags to get map info
    public static void ecUpdateMapInfo() throws GameActionException {
        // reset closest enemy locations (except slanderer, but immediately switch to a new one if found)
        closestEnemyLoc[ENEMY_TYPE_MUCKRAKER] = null;
        closestEnemyLoc[ENEMY_TYPE_POLITICIAN] = null;
        Arrays.fill(closestEnemyDist, Integer.MAX_VALUE);
        Arrays.fill(closestEnemyInfluence, 0);

        numEnemyUnits = 0;
        int totalEnemyX = 0;
        int totalEnemyY = 0;

        MapLocation curLoc = RobotPlayer.rc.getLocation();
        for (int i = friendlyUnitIDs.length - 1; i >= 0; i--) {
            if (friendlyUnitIDs[i] != 0 && RobotPlayer.rc.canGetFlag(friendlyUnitIDs[i])) {
                int flag = RobotPlayer.rc.getFlag(friendlyUnitIDs[i]);
                int locNum              = flag & 0x3FFF;    // first 14 bits
                int isEC                = flag >> 14 & 0x1; // next 1 bit
                int type                = flag >> 15 & 0x3; // next 2 bits
                int influenceInfo       = flag >> 17;       // last 7 bits
                int moddedX = locNum % MAP_SIZE;
                int moddedY = locNum / MAP_SIZE;
                MapLocation loc = getLocationFromModded(moddedX, moddedY);
                if (isEC == 1) {
                    if (siegeableECAtLocation[moddedX][moddedY] == EC_TYPE_UNKNOWN) {
                        siegeLocations[siegeLocationsIdx++] = loc;
                    }
                    siegeableECAtLocation[moddedX][moddedY] = type;
                    ecInfluence[moddedX][moddedY] = influenceInfo * EC_INFLUENCE_SCALE;
                } else  {
                    if (type != ENEMY_TYPE_UNKNOWN) {
                        numEnemyUnits++;
                        totalEnemyX += loc.x;
                        totalEnemyY += loc.y;
                    }
                    if (curLoc.isWithinDistanceSquared(loc, closestEnemyDist[type] - 1)) {
                        closestEnemyLoc[type] = loc;
                        closestEnemyDist[type] = curLoc.distanceSquaredTo(loc);
                        int enemyInfluence = influenceInfo * ENEMY_INFLUENCE_SCALE;
                        closestEnemyInfluence[type] = enemyInfluence;
                    }
                }
            }
        }
        
        averageEnemyLocation = numEnemyUnits > 0 ? new MapLocation(totalEnemyX / numEnemyUnits, totalEnemyY / numEnemyUnits) : null;
    }

    public static MapLocation getClosestSiegeableEC(boolean neutral) {
        MapLocation curLoc = RobotPlayer.rc.getLocation();
        MapLocation closestSiegeLoc = null;
        int closestSiegeLocDist = Integer.MAX_VALUE;
        for (int i = 0; i < siegeLocations.length; i++) {
            if (siegeLocations[i] == null) break;

            int moddedX = siegeLocations[i].x % MAP_SIZE;
            int moddedY = siegeLocations[i].y % MAP_SIZE;
            int targetECType = neutral ? EC_TYPE_NEUTRAL : EC_TYPE_ENEMY;
            if (siegeableECAtLocation[moddedX][moddedY] == targetECType &&
                curLoc.isWithinDistanceSquared(siegeLocations[i], closestSiegeLocDist - 1)) {
                    closestSiegeLoc = siegeLocations[i];
                    closestSiegeLocDist = curLoc.distanceSquaredTo(siegeLocations[i]);
            }
        }
        return closestSiegeLoc;
    }

    public static MapLocation getClosestEnemyUnitOfType(int enemyUnitType) {
        return closestEnemyLoc[enemyUnitType];
    }

    // called by muckraker to send info back to ec
    public static void sendMapInfo() throws GameActionException {
        MapLocation curLoc = RobotPlayer.rc.getLocation();
        Team friendlyTeam = RobotPlayer.rc.getTeam();

        RobotInfo closestSiegeableUnit = null;
        RobotInfo closestNonSiegeableUnit = null;
        RobotInfo closestEnemyUnit = null;
        RobotInfo closestSlandererUnit = null;
        int closestSiegeableUnitDist = Integer.MAX_VALUE;
        int closestNonSiegeableUnitDist = Integer.MAX_VALUE;
        int closestEnemyUnitDist = Integer.MAX_VALUE;
        int closestSlandererUnitDist = Integer.MAX_VALUE;
        RobotInfo[] sensedRobots = RobotPlayer.rc.senseNearbyRobots();
        for (int i = sensedRobots.length - 1; i >= 0; --i) {
            Team team = sensedRobots[i].getTeam();
            RobotType type = sensedRobots[i].getType();
            MapLocation robotLoc = sensedRobots[i].getLocation();
            if (team == friendlyTeam && type != RobotType.ENLIGHTENMENT_CENTER) continue;

            if (type != RobotType.ENLIGHTENMENT_CENTER) {
                if (type == RobotType.SLANDERER) {
                    if (curLoc.isWithinDistanceSquared(robotLoc, closestSlandererUnitDist - 1)) {
                        closestSlandererUnit = sensedRobots[i];
                        closestSlandererUnitDist = curLoc.distanceSquaredTo(robotLoc);
                    }
                } else {
                    if (curLoc.isWithinDistanceSquared(robotLoc, closestEnemyUnitDist - 1)) {
                        closestEnemyUnit = sensedRobots[i];
                        closestEnemyUnitDist = curLoc.distanceSquaredTo(robotLoc);
                    }
                }
            } else {
                if (team == friendlyTeam) {
                    if (curLoc.isWithinDistanceSquared(robotLoc, closestNonSiegeableUnitDist - 1)) {
                        closestNonSiegeableUnit = sensedRobots[i];
                        closestNonSiegeableUnitDist = curLoc.distanceSquaredTo(robotLoc);
                    }
                } else {
                    if (curLoc.isWithinDistanceSquared(robotLoc, closestSiegeableUnitDist - 1)) {
                        closestSiegeableUnit = sensedRobots[i];
                        closestSiegeableUnitDist = curLoc.distanceSquaredTo(robotLoc);
                    }
                }
            }
        }

        MapLocation targetLoc = null;
        int isEC = 0;
        int type = 0;
        int influenceInfo = 0;

        // priority list:
        // 1. siegeable/settleable ec's
        // 2. slanderers
        // 3. friendly ec's
        // 4. other enemy units

        if (closestSiegeableUnit != null) {
            targetLoc = closestSiegeableUnit.getLocation();
            isEC = 1;
            type = closestSiegeableUnit.getTeam() == Team.NEUTRAL ? EC_TYPE_NEUTRAL : EC_TYPE_ENEMY;
            influenceInfo = getInfluenceInfo(true, closestSiegeableUnit.getInfluence());
        } else if (closestSlandererUnit != null) {
            targetLoc = closestSlandererUnit.getLocation();
            isEC = 0;
            type = ENEMY_TYPE_SLANDERER;
            influenceInfo = getInfluenceInfo(false, closestSlandererUnit.getInfluence());
        } else if (closestNonSiegeableUnit != null) {
            targetLoc = closestNonSiegeableUnit.getLocation();
            isEC = 1;
            type = EC_TYPE_FRIENDLY;
            influenceInfo = getInfluenceInfo(true, closestNonSiegeableUnit.getInfluence());
        } else if (closestEnemyUnit != null) {
            targetLoc = closestEnemyUnit.getLocation();
            isEC = 0;
            type = closestEnemyUnit.getType() == RobotType.MUCKRAKER ? ENEMY_TYPE_MUCKRAKER : ENEMY_TYPE_POLITICIAN;
            influenceInfo = getInfluenceInfo(false, closestEnemyUnit.getInfluence());
        } else {
            targetLoc = curLoc; // just so it's not null
            isEC = 0;
            type = ENEMY_TYPE_UNKNOWN;
        }

        int moddedX = targetLoc.x % MAP_SIZE;
        int moddedY = targetLoc.y % MAP_SIZE;
        int locNum = moddedX | (moddedY << 7); // first 14 bits

        RobotPlayer.rc.setFlag(locNum |
                              (isEC << 14) |
                              (type << 15) |
                              (influenceInfo  << 17));
    }

    private static int getInfluenceInfo(boolean isEC, int origInfluence) {
        int scale = isEC ? EC_INFLUENCE_SCALE : ENEMY_INFLUENCE_SCALE;
        return Math.min((int)Math.ceil((double)origInfluence / scale), MAX_INFLUENCE_INFO_STORED);
    }

    // Section Mission Info
    
    static final int NUM_MISSION_TYPES = 6;

    // mission types
    static final int MISSION_TYPE_SLEUTH = 0;
    static final int MISSION_TYPE_SIEGE = 1;
    static final int MISSION_TYPE_SETTLE = 2;
    static final int MISSION_TYPE_DEMUCK = 3;
    static final int MISSION_TYPE_DEPOLI = 4;
    static final int MISSION_TYPE_SCOUT = 5;

    static final int NO_MISSION_AVAILABLE = 1 << 14;

    static MapLocation[][] latestMissionSectionLoc = new MapLocation[MAX_NUM_FRIENDLY_ECS][NUM_MISSION_TYPES];

    // called by any non-ec unit, uses ec flags to get mission info
    public static void updateSectionMissionInfo() throws GameActionException {
        for (int i = 0; i < MAX_NUM_FRIENDLY_ECS; i++) {
            if (RobotPlayer.rc.canGetFlag(friendlyECIDs[i])) {
                int flag = RobotPlayer.rc.getFlag(friendlyECIDs[i]);
                int missionType         = flag & 0x7;   // first 3 bits
                int missionLocNum       = flag >> 3;    // last 21 bits (only 14 used unless sending a no mission available message)
                latestMissionSectionLoc[i][missionType] = null;
                if (missionLocNum != NO_MISSION_AVAILABLE) {
                    latestMissionSectionLoc[i][missionType] = getLocationFromModded(missionLocNum % MAP_SIZE,
                                                                                    missionLocNum / MAP_SIZE);
                }
            }
        }
    }

    public static void sendMissionInfo(MapLocation missionLoc, int missionType) throws GameActionException {
        int missionLocNum = missionLoc == null ? NO_MISSION_AVAILABLE : (missionLoc.x % MAP_SIZE) | ((missionLoc.y % MAP_SIZE) << 7);
        RobotPlayer.rc.setFlag(missionType | (missionLocNum << 3));
    }

    public static MapLocation getClosestMissionOfType(int missionType) {
        MapLocation curLoc = RobotPlayer.rc.getLocation();
        MapLocation closestMissionLoc = null;
        int closestMissionDist = Integer.MAX_VALUE;
        for (int i = 0; i < MAX_NUM_FRIENDLY_ECS; i++) {
            if (friendlyECIDs[i] != 0 &&
                RobotPlayer.rc.canGetFlag(friendlyECIDs[i]) &&
                latestMissionSectionLoc[i][missionType] != null &&
                curLoc.isWithinDistanceSquared(latestMissionSectionLoc[i][missionType], closestMissionDist - 1)) {
                    closestMissionLoc = latestMissionSectionLoc[i][missionType];
                    closestMissionDist = curLoc.distanceSquaredTo(closestMissionLoc);
            }
        }
        return closestMissionLoc;
    }

    // Utilities

    public static MapLocation getLocationFromModded(int moddedX, int moddedY) {
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

