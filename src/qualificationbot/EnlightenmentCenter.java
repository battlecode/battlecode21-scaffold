package qualificationbot;
import battlecode.common.*;

public class EnlightenmentCenter {

    static final int MUCKRAKER_INFLUENCE = 1;
    static final int POLITICIAN_INFLUENCE = 50;
    static final double SLANDERER_INFLUENCE_PERCENTAGE = 0.1;
    static final int MAX_SLANDERER_INFLUENCE = 949;
    public static final double[] BID_PERCENTAGES = new double[] {.005, .0075, .02, .04, .08};
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
        if (slanderersBuilt <= turnCount / 30) { 
            int influence = Math.min(MAX_SLANDERER_INFLUENCE, rc.getInfluence() / 2);
            if (buildRobot(RobotType.SLANDERER, influence)) {
                slanderersBuilt++;
            }
        }
        //build a politician every 20 turns with 20% of our influence. 
        else if (politiciansBuilt <= turnCount / 50) {
            int influence = rc.getInfluence() / 4; 
            if (buildRobot(RobotType.POLITICIAN, influence)) {
                politiciansBuilt++; 
            }
        }
        //if we are not building a slanderer or poli we should always be producing muckrakers. 
        else if (turnCount % 3 == 0) {
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
        Team enemyTeam = rc.getTeam().opponent();
        MapLocation curLoc = rc.getLocation();
        int roundNum = rc.getRoundNum();

        int closestSquaredDist = Integer.MAX_VALUE;
        MapLocation closestSectionLoc = null;
        for (int i = Communication.MAX_NUM_SECTIONS_WITH_ROBOTS - 1; i >= 0; i--) {
            MapLocation sectionLoc = Communication.sectionsWithRobots[i];
            if (sectionLoc == null) continue;

            boolean isPotentialMissionSection;
            switch ((roundNum % 6) + 1) {
                case Communication.MISSION_TYPE_SLEUTH:
                    isPotentialMissionSection = Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.SLANDERER);
                    // isPotentialMissionSection = false;
                    break;
                case Communication.MISSION_TYPE_STICK:
                    // isPotentialMissionSection = Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.POLITICIAN);
                    isPotentialMissionSection = false;
                    break;
                case Communication.MISSION_TYPE_SIEGE:
                    isPotentialMissionSection = Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.ENLIGHTENMENT_CENTER) ||
                                                Communication.isRobotTeamAndTypeInSection(sectionLoc, Team.NEUTRAL, RobotType.ENLIGHTENMENT_CENTER);
                    break;
                case Communication.MISSION_TYPE_SCOUT:
                    // TODO add scouting missions
                    isPotentialMissionSection = false;
                    break;
                case Communication.MISSION_TYPE_DEMUCK:
                    isPotentialMissionSection = Communication.isRobotTeamAndTypeInSection(sectionLoc, enemyTeam, RobotType.MUCKRAKER);
                    // isPotentialMissionSection = false;
                    break;
                case Communication.MISSION_TYPE_HIDE:
                    isPotentialMissionSection = !sectionLoc.equals(Communication.getCurrentSection()) && Communication.sectionOnEdge[sectionLoc.x][sectionLoc.y];
                    break;
                default:
                    isPotentialMissionSection = false;
                    break;
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
            Communication.sendMissionInfo(closestSectionLoc, (roundNum % 6) + 1);
            return true;
        }
        return false;
    }
}
