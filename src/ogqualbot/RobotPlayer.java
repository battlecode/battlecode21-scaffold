package ogqualbot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final int SMALL_MUCKRAKER_INFLUENCE = 1;

    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        try {
            switch (rc.getType()) {
                case ENLIGHTENMENT_CENTER:  EnlightenmentCenter.run();  break;
                case POLITICIAN:            Politician.run();           break;
                case SLANDERER:             Slanderer.run();            break;
                case MUCKRAKER:
                    if (rc.getInfluence() == SMALL_MUCKRAKER_INFLUENCE) {
                        ScoutingMuckraker.run();
                    } else {
                        SleuthingMuckraker.run();
                    }
                    break;
                default:                                                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
