package sprintbot;
import battlecode.common.*;

public class BroadcasterCommunication {

    public static final int ALL_IDS = -1;
    public static final int ALL_IDS_NUM = 0x3F;

    private static final int PASSIBILITY_FLAG_BIT = 0x0;
    private static final int MISSION_FLAG_BIT = 0x1;

    private static final int SLANDERER_TYPE_BIT = 0x1;
    private static final int MUCKRAKER_TYPE_BIT = 0x2;
    private static final int POLITICIAN_TYPE_BIT = 0x4;

    static boolean isPassibilityFlag = false;
     
    // Passibility Flag
    static boolean evenTurn = false;
    static int[] passibilityValues = new int[7];

    // Mission Flag
    static MapLocation targetLoc = null;
    static int friendlyRobotTypesNum;
    static int friendlyRobotIDsNum;

    // Passibility Flag

    public static void sendPassibilityFlag() {
        // TODO: implement
    }

    // Mission Flag

    public static void setMissionTargetLocation(MapLocation targetLoc) {
        BroadcasterCommunication.targetLoc = targetLoc;
    }

    public static void addMissionFriendlyRobotType(RobotType robotType) {
        switch (robotType) {
            case SLANDERER:
                friendlyRobotTypesNum |= SLANDERER_TYPE_BIT;
                break;
            case MUCKRAKER:
                friendlyRobotTypesNum |= MUCKRAKER_TYPE_BIT;
                break;
            case POLITICIAN:
                friendlyRobotTypesNum |= POLITICIAN_TYPE_BIT;
                break;
            default:
                break;
        }
    }
    
    public static void addMissionFriendlyRobotID(int id) {
        if (id == ALL_IDS) {
            friendlyRobotIDsNum = ALL_IDS_NUM; 
        } else {
            friendlyRobotIDsNum |= 0x1 << (id % 6);
        }
    }

    public static void sendMissionFlag() throws GameActionException {
        int flag = MISSION_FLAG_BIT |
                   (targetLoc.x % 128) << 1 |
                   (targetLoc.y % 128) << 8 |
                   friendlyRobotTypesNum << 15 |
                   friendlyRobotIDsNum << 21;
        RobotPlayer.rc.setFlag(flag);

        // reset values
        friendlyRobotIDsNum = 0;
        friendlyRobotTypesNum = 0;
    }

    /** PARSING */

    public static void parseFlag(int flag) {
        // TODO: implement
    }

    // Mission Flag

    public static boolean isRobotTypeInMission(RobotType robotType) {
        switch (robotType) {
            case SLANDERER:
                return (friendlyRobotTypesNum & SLANDERER_TYPE_BIT) != 0;
            case MUCKRAKER:
                return (friendlyRobotTypesNum & MUCKRAKER_TYPE_BIT) != 0;
            case POLITICIAN:
                return (friendlyRobotTypesNum & POLITICIAN_TYPE_BIT) != 0;
            default:
                break;
        }
        return false;
    }

    public static boolean isRobotIDInMission(int id) {
        return (friendlyRobotIDsNum & (0x1 << (id % 6))) != 0;
    }

}
