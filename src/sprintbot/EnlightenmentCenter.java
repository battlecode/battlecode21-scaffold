package sprintbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.1;
    static final int MAX_SLANDERER_INFLUENCE = 949;
    static final double[] BID_PERCENTAGES = new double[] {.05, .1, .125, .15, .2};

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

    public static void initialize() {}


    public static void executeTurn(int turnNumber) throws GameActionException {
        buildRandomRobot();
        bid(); 
        sendMission();
    }

    private static void bid() throws GameActionException {
        if(rc.getTeamVotes() < 751) {
            int bidAmount = (int)(rc.getInfluence() * BID_PERCENTAGES[rc.getRoundNum() / BID_PERCENTAGES.length]);
            if(rc.canBid(bidAmount)) {
                rc.bid(bidAmount);
            }
        }
    }

    private static boolean sendMission() throws GameActionException {
        // TODO add scouting missions
        Team enemyTeam = rc.getTeam().opponent();
        MapLocation curLoc = rc.getLocation();
        int roundNum = rc.getRoundNum();

        int closestSquaredDist = Integer.MAX_VALUE;
        MapLocation closestSectionLoc = null;
        for (int i = Communication.sectionsWithRobotsSize - 1; i >= 0; i--) {
            MapLocation sectionLoc = Communication.sectionsWithRobots[i];

            boolean isPotentialMissionSection;
            switch (roundNum % 3) {
                case 0:
                    isPotentialMissionSection = Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.SLANDERER) ||
                                                Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.POLITICIAN);
                    break;
                case 1:
                    isPotentialMissionSection = Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.ENLIGHTENMENT_CENTER) ||
                                                Communication.isRobotTeamAndTypeInSection(sectionLoc, Team.NEUTRAL, RobotType.ENLIGHTENMENT_CENTER) ||
                                                Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.MUCKRAKER);
                    break;
                case 2:
                    isPotentialMissionSection = Communication.sectionOnEdge[sectionLoc.x][sectionLoc.y];
                    break;
                default:
                    isPotentialMissionSection = false;
            }

            if (isPotentialMissionSection) {
                MapLocation sectionCenterLoc = Communication.getSectionCenterLoc(sectionLoc);
                if (curLoc.isWithinDistanceSquared(sectionCenterLoc, closestSquaredDist - 1)) {
                    closestSectionLoc = sectionLoc;
                    closestSquaredDist = curLoc.distanceSquaredTo(sectionCenterLoc);
                }
            }
        }

        if (closestSectionLoc != null) {
            switch (roundNum % 3) {
                case 0:
                    if (Communication.isRobotTeamAndTypeInSection(closestSectionLoc, enemyTeam, RobotType.SLANDERER)) {
                        Communication.sendMissionInfo(closestSectionLoc, Communication.MISSION_TYPE_SLEUTH);
                    } else if (Communication.isRobotTeamAndTypeInSection(closestSectionLoc, enemyTeam, RobotType.POLITICIAN)) {
                        Communication.sendMissionInfo(closestSectionLoc, Communication.MISSION_TYPE_STICK);
                    }
                    break;
                case 1:
                    if (Communication.isRobotTeamAndTypeInSection(closestSectionLoc, enemyTeam, RobotType.ENLIGHTENMENT_CENTER) ||
                        Communication.isRobotTeamAndTypeInSection(closestSectionLoc, Team.NEUTRAL, RobotType.ENLIGHTENMENT_CENTER)) {
                        Communication.sendMissionInfo(closestSectionLoc, Communication.MISSION_TYPE_SIEGE);
                    } else if (Communication.isRobotTeamAndTypeInSection(closestSectionLoc, enemyTeam, RobotType.MUCKRAKER)) {
                        Communication.sendMissionInfo(closestSectionLoc, Communication.MISSION_TYPE_DEMUCK);
                    }
                    break;
                case 2:
                    Communication.sendMissionInfo(closestSectionLoc, Communication.MISSION_TYPE_HIDE);
                    break;
                default:
                    break;
            }
            return true;
        }
        return false;
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
                return Math.max((int)(rc.getInfluence() * SLANDERER_INFLUENCE_PERCENTAGE), MAX_SLANDERER_INFLUENCE);
            default:
                return 0;
        }
    }
}
