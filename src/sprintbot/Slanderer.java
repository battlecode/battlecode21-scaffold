package sprintbot;
import battlecode.common.*;

public class Slanderer {
    static RobotController rc;

    public static void run() throws GameActionException {
        int turn = 0;
        rc = RobotPlayer.rc;
        initialize();
        while (true) {
            executeTurn(turn++);
            Clock.yield();
        }
    }

    public static void initialize() {
        throw new UnsupportedOperationException();
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        throw new UnsupportedOperationException();
    }
}
