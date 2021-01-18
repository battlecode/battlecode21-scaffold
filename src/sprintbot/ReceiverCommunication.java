package sprintbot;

import battlecode.common.*;

public class ReceiverCommunication {
    public static final int UNKNOWN_OR_OFF_MAP  = 0x0;

    public static final int FRIENDLY_EC         = 0x8;
    public static final int FRIENDLY_SLANDERER  = 0x9;
    public static final int FRIENDLY_POLITICIAN = 0xA;
    public static final int FRIENDLY_MUCKRAKER  = 0xB;

    public static final int ENEMY_EC            = 0xC;
    public static final int ENEMY_SLANDERER     = 0xD;
    public static final int ENEMY_POLITICIAN    = 0xE;
    public static final int ENEMY_MUCKRAKER     = 0xF;


    static int[] flags = new int[9];
    // guaranteed to hold 4 bit values, but bit manipulation is faster / easier with int
    static int[] surroundings = new int[49];
    static MapLocation moddedLocation = null;

    // TODO: update Pathfinding map while adding surroundings
    private static void updateSurroundings() throws GameActionException {
        // update passabilities
        MapLocation robotLoc = RobotPlayer.rc.getLocation();
        for (int i = 48; i >= 0; --i) {
            MapLocation loc = new MapLocation(robotLoc.x - 3 + (i % 7), robotLoc.y - 3 + (i / 7));
            surroundings[i] = RobotPlayer.rc.onTheMap(loc) ? Pathfinding.getIntFromDoublePassability(RobotPlayer.rc.sensePassability(loc)) : UNKNOWN_OR_OFF_MAP;
        }

        // update nearby robots
        RobotInfo[] riArr = RobotPlayer.rc.senseNearbyRobots(18);
        for (int i = riArr.length - 1; i >= 0; --i) {
            int xDiff = riArr[i].location.x - robotLoc.x;
            int yDiff = riArr[i].location.y - robotLoc.y;
            if (xDiff >= -3 && xDiff <= 3 && yDiff >= -3 && yDiff <= 3) {
                surroundings[(xDiff + 3) + (yDiff + 3) * 7] = getIntFromRobotInfo(riArr[i]);
            }
        }
    }

    private static void updateModdedLocation() {
        moddedLocation = new MapLocation(RobotPlayer.rc.getLocation().x % 128,
                                         RobotPlayer.rc.getLocation().y % 128);
    }

    private static int getIntFromRobotInfo(RobotInfo ri) {
        int res = ri.getTeam() == RobotPlayer.rc.getTeam() ? FRIENDLY_EC : ENEMY_EC;
        switch (ri.getType()) {
            case ENLIGHTENMENT_CENTER:
                res += 0;
                break;
            case SLANDERER:
                res += 1;
                break;
            case POLITICIAN:
                res += 2;
                break;
            case MUCKRAKER:
                res += 3;
                break;
        }
        return res;
    }

    public static void updateFlags() throws GameActionException {
        /**
         * 1. 49 * 4 bits - surrounding passability or robot info
         * 2. 14 bits — robot location
         *  Total: 210 bits < 27 bytes
         */
        updateSurroundings();
        updateModdedLocation();

        for (int i = 7; i >= 0; --i) {
            // 6 locs per flag
            int startIdx = i * 6;
            flags[i] = surroundings[startIdx] | 
                        surroundings[startIdx + 1] << 4 |
                        surroundings[startIdx + 2] << 8 |
                        surroundings[startIdx + 3] << 12 |
                        surroundings[startIdx + 4] << 16 |
                        surroundings[startIdx + 5] << 20;
        }
        
        flags[8] = surroundings[48] |         // last loc
                    (moddedLocation.x << 8) | // x coord
                    (moddedLocation.y << 16); // y coord

        System.out.println("bytes used: " + Clock.getBytecodeNum());
    }

    public static void sendFlag() throws GameActionException {
        RobotPlayer.rc.setFlag(flags[RobotPlayer.rc.getRoundNum() % 9]);
    }

    /** PARSING */

    public static void parseFlags(int[] flags) {
        ReceiverCommunication.flags = flags;

        for (int i = 0; i < 49/6; ++i) {
            int startIdx = i * 6;
            int flag = flags[i];
            surroundings[startIdx]      = (flag & 0x0000000F);
            surroundings[startIdx + 1]  = (flag & 0x000000F0) >> 4;
            surroundings[startIdx + 2]  = (flag & 0x00000F00) >> 8;
            surroundings[startIdx + 3]  = (flag & 0x0000F000) >> 12;
            surroundings[startIdx + 4]  = (flag & 0x000F0000) >> 16;
            surroundings[startIdx + 5]  = (flag & 0x00F00000) >> 20;
        }

        int lastFlag = flags[8];
        surroundings[48]    =                   (lastFlag & 0x0000000F);
        moddedLocation      = new MapLocation(  (lastFlag & 0x0000FF00) >> 8,
                                                (lastFlag & 0x00FF0000) >> 16);
    }
}
