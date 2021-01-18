package sprintbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int SLANDERER_INFLUENCE = 100;
    static final int POLITICIAN_INFLUENCE = 50;

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
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        Communication.updateNearbyRobots();
        Communication.readFlags();
        switch ((int)(Math.random() * 3)) {
            case 0:
                if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.NORTH, MUCKRAKER_INFLUENCE)) {
                    rc.buildRobot(RobotType.MUCKRAKER, Direction.NORTH, MUCKRAKER_INFLUENCE);
                }
                break;
            case 1:
                if (rc.canBuildRobot(RobotType.POLITICIAN, Direction.SOUTH, POLITICIAN_INFLUENCE)) {
                    rc.buildRobot(RobotType.POLITICIAN, Direction.SOUTH, POLITICIAN_INFLUENCE);
                }
                break;
            case 2:
                if (rc.canBuildRobot(RobotType.SLANDERER, Direction.WEST, SLANDERER_INFLUENCE)) {
                    rc.buildRobot(RobotType.SLANDERER, Direction.WEST, SLANDERER_INFLUENCE);
                }
                break;
            default:
                break;
        }
        int oppTeamNum = rc.getTeam() == Team.A ? Communication.TEAM_NUM_B : Communication.TEAM_NUM_A;
        if (Communication.closestRobotLocs[oppTeamNum][])
    }
}
