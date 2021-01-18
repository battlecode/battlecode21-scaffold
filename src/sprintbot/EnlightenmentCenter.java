package sprintbot;
import java.util.Arrays;

import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.1;
    static final int MAX_SLANDERER_INFLUENCE = 949;

    static RobotController rc;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            Communication.updateIDList(true);
            Communication.updateSectionRobotInfo();
            if (rc.getRoundNum() == 1000) {
                for (int[] a : Communication.sectionRobotInfo) {
                    System.out.println(Arrays.toString(a));
                }
            }
            executeTurn(turn++);
            Clock.yield();
        }
    }

    static int[] flags = new int[10];

    public static void initialize() {
        // throw new UnsupportedOperationException();
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        buildRandomRobot();
        // TODO send missions
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
                return Math.min((int)(rc.getInfluence() * SLANDERER_INFLUENCE_PERCENTAGE), MAX_SLANDERER_INFLUENCE);
            default:
                return 0;
        }
    }
}
