package qualificationbot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        try {
            switch (rc.getType()) {
                case ENLIGHTENMENT_CENTER:  EnlightenmentCenter.run();  break;
                case POLITICIAN:            Politician.run();           break;
                case SLANDERER:             Slanderer.run();            break;
                case MUCKRAKER:             Muckraker.run();            break;
                default:                                                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
