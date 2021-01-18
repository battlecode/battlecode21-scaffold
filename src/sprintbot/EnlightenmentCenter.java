package sprintbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.1;
    static final int MAX_SLANDERER_INFLUENCE = 949;
    public static final double[] BID_PERCENTAGES = new double[] {.05, .1, .125, .15, .2};
    static final int NUM_ROUNDS = 1500;

    static int slanderersBuilt; 
    static int politiciansBuilt; 

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

    public static void initialize() {
        slanderersBuilt = 0; 
        politiciansBuilt = 0; 
    }

    public static void executeTurn(int turnNumber) throws GameActionException {
        buildRobot(); 
        bid(); 
        sendMission();
    }

    private static void bid() throws GameActionException {
        if(rc.getTeamVotes() < NUM_ROUNDS / 2 + 1) {
            int influence = (int)(rc.getInfluence() * BID_PERCENTAGES[(rc.getRoundNum() - 1) / (NUM_ROUNDS / BID_PERCENTAGES.length)]);
            if (rc.canBid(influence)) {
                rc.bid(influence);
            }
        }
    
    }

    private static void buildRobot() throws GameActionException {
        int turnCount = RobotPlayer.rc.getRoundNum();

        //build a slanderer every 50 turns with half influence, up to MAX_SLANDERER_INFLUENCE
        if (slanderersBuilt <= turnCount / 50) { 
            int influence = Math.min(MAX_SLANDERER_INFLUENCE, rc.getInfluence() / 2);
            if (buildRobot(RobotType.POLITICIAN, influence)) {
                slanderersBuilt++;
            }
        }
        //build a politician every 20 turns with 20% of our influence. 
        else if (politiciansBuilt <= turnCount / 20) {
            int influence = rc.getInfluence() / 5; 
            if (buildRobot(RobotType.POLITICIAN, influence)) {
                politiciansBuilt++; 
            }
        }
        //if we are not building a slanderer or poli we should always be producing muckrakers. 
        else {
            buildRobot(RobotType.MUCKRAKER, 1);
        }
    }

    private static boolean buildRobot(RobotType type, int influence) throws GameActionException {
        Direction[] allDirections = Direction.allDirections();
        for (int i = allDirections.length - 1; i >= 0; --i) {
            Direction buildDir = allDirections[i];
            if (rc.canBuildRobot(type, buildDir, influence)) {
                rc.buildRobot(type, buildDir, influence);
                return true;
            }
        }
        return false;
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
}
