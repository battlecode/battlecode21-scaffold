package sprintbot;

import battlecode.common.*;

public class Pathfinding2 {
    public static final double PASSIBILITY_THRESHOLD = 0.7;
    public static final double ODDS_DISREGARD_BARRIER = 0.7;

    private static final int MAP_SIZE = 64;
    private static final int MAP_MASK = MAP_SIZE - 1;
    
    private static final int COORD_BITS = 15;
    private static final int COORD_MASK = (1 << COORD_BITS) - 1;

    private static Direction[] allDirections = new Direction[] {
        Direction.NORTHEAST,
        Direction.NORTHWEST,
        Direction.SOUTHEAST,
        Direction.SOUTHWEST,
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST
    };


    static boolean[][] map = new boolean[MAP_SIZE][MAP_SIZE];
    static int lowerXBound = Integer.MIN_VALUE;
    static int upperXBound = Integer.MAX_VALUE;
    static int lowerYBound = Integer.MIN_VALUE;
    static int upperYBound = Integer.MAX_VALUE;

    // TODO: incorporate communication flags and such

    public static MapLocation getMapLocationFromModded(MapLocation moddedLoc) {
        return null; // TODO: implement
    }

    // TODO: investigate why getting incorrect bounds
    private static void processEdgeLocation(MapLocation edgeLocation, Direction edge) {
        switch (edge) {
            case NORTH:
                upperYBound = edgeLocation.y;
                break;
            case SOUTH:
                lowerYBound = edgeLocation.y;
                break;
            case EAST:
                upperXBound = edgeLocation.x;
                break;
            case WEST:
                lowerXBound = edgeLocation.x;
                break;
            default:
                break;
        }
    }

    public static boolean isWithinSensorRadius(MapLocation location) {
        return location.isWithinDistanceSquared(RobotPlayer.rc.getLocation(), RobotPlayer.rc.getType().sensorRadiusSquared);
    }

    public static boolean map(MapLocation location, Direction potentialEdge) throws GameActionException {
        if (isWithinSensorRadius(location)) {
            if (RobotPlayer.rc.onTheMap(location)) {
                double passability = RobotPlayer.rc.sensePassability(location);
                boolean robotAtLocation = RobotPlayer.rc.senseRobotAtLocation(location) != null;
                map[location.x & MAP_MASK][location.y & MAP_MASK] = passability < PASSIBILITY_THRESHOLD;
                return robotAtLocation || (map[location.x & MAP_MASK][location.y & MAP_MASK] && Math.random() > ODDS_DISREGARD_BARRIER);
            }
            processEdgeLocation(location, potentialEdge);
            return true;
        }
        if (location.x <= lowerXBound || location.x >= upperXBound || location.y <= lowerYBound || location.y >= upperYBound) {
            return true;
        }
        return map[location.x & MAP_MASK][location.y & MAP_MASK];
    }

    /** BFS Implementation */

    private static final int BYTECODE_PADDING = 5000;

    private static final int QUEUE_SIZE = MAP_SIZE * MAP_SIZE;
    private static final int QUEUE_MASK = QUEUE_SIZE - 1;

    private static int[] queue = new int[MAP_SIZE * MAP_SIZE];
    private static int queueSize = 0;
    private static int first = 0;

    static int[][] lastRoundVisited = new int[MAP_SIZE][MAP_SIZE];
    static int[][] dist = new int[MAP_SIZE][MAP_SIZE];
    static Direction[][] dir = new Direction[MAP_SIZE][MAP_SIZE];

    static int roundNum; // needs to be updated before each search
    static MapLocation targetLocation = null;
    static MapLocation intermediateTargetLocation;

    // BFS Methods

    public static void moveTo(MapLocation location) throws GameActionException {
        MapLocation robotLoc = RobotPlayer.rc.getLocation();
        if (robotLoc.equals(location)) return;
        int xModded = robotLoc.x & MAP_MASK;
        int yModded = robotLoc.y & MAP_MASK;
        if (intermediateTargetLocation == null || !targetLocation.equals(location) || map(robotLoc.add(dir[xModded][yModded]), dir[xModded][yModded])) {
            createPath(location);
        }
        if (RobotPlayer.rc.canMove(dir[xModded][yModded])) {
            RobotPlayer.rc.move(dir[xModded][yModded]);
            if (RobotPlayer.rc.getLocation().equals(intermediateTargetLocation)) {
                intermediateTargetLocation = null;
            }
        }
    }

    private static void createPath(MapLocation targetLocation) throws GameActionException {
        // reset values
        queueSize = 0;
        roundNum = RobotPlayer.rc.getRoundNum();
        Pathfinding2.targetLocation = targetLocation;
        intermediateTargetLocation = null;

        // add start loc to queue
        MapLocation startLoc = RobotPlayer.rc.getLocation();
        int startLocXModded = startLoc.x & MAP_MASK;
        int startLocYModded = startLoc.y & MAP_MASK;
        dist[startLocXModded][startLocYModded] = 0;
        dir[startLocXModded][startLocYModded] = Direction.CENTER;
        lastRoundVisited[startLocXModded][startLocYModded] = roundNum;

        queue[(first + queueSize++) & QUEUE_MASK] = startLoc.x | (startLoc.y << COORD_BITS);

        int origEstimatedDist = Math.max(Math.abs(startLoc.x - targetLocation.x), Math.abs(startLoc.y - targetLocation.y));
        int closestEstimatedDist = origEstimatedDist;

        while (queueSize > 0 &&
                (Clock.getBytecodesLeft() > BYTECODE_PADDING ||
                 RobotPlayer.rc.getCooldownTurns() > 1 ||
                 intermediateTargetLocation == null)) {            
            // dequeue loc
            queueSize--;
            int locNum = queue[first++ & QUEUE_MASK];
            MapLocation location = new MapLocation(locNum & COORD_MASK, locNum >> COORD_BITS);

            int xModded = location.x & MAP_MASK;
            int yModded = location.y & MAP_MASK;

            // save direction to closest estimated dist just in case we don't finish BFS
            int estimatedDist = Math.max(Math.abs(location.x - targetLocation.x), Math.abs(location.y - targetLocation.y));
            if (estimatedDist < closestEstimatedDist) {
                intermediateTargetLocation = location;
                closestEstimatedDist = estimatedDist;
            }

            // break if target loc is found
            if (location.equals(targetLocation)) break;

            // add adjacent locs to ring queue
            int pDist = dist[xModded][yModded]; 
            for (int i = 7; i >= 0; --i) {
                MapLocation adjLoc = location.add(allDirections[i]);
                int adjXModded = adjLoc.x & MAP_MASK;
                int adjYModded = adjLoc.y & MAP_MASK;
                if (lastRoundVisited[adjXModded][adjYModded] != roundNum && !map(adjLoc, allDirections[i])) {
                    dist[adjXModded][adjYModded] = pDist + 1;
                    dir[adjXModded][adjYModded] = allDirections[i].opposite();
                    // enque adj loc
                    queue[(first + queueSize++) & QUEUE_MASK] = adjLoc.x | (adjLoc.y << COORD_BITS);
                }
                // only check each loc once
                lastRoundVisited[adjXModded][adjYModded] = roundNum;
            }
        }

        // there is no path to dest -- either oob or robot is surrounded by units
        if (intermediateTargetLocation == null) return;

        // reverse directions from intermediate target to create path
        MapLocation tmp = intermediateTargetLocation;
        Direction lastDir = Direction.CENTER;
        while (!tmp.equals(startLoc)) {
            int xModded = tmp.x & MAP_MASK;
            int yModded = tmp.y & MAP_MASK;
            Direction curDir = dir[xModded][yModded];
            dir[xModded][yModded] = lastDir;
            lastDir = curDir.opposite();
            tmp = tmp.add(curDir);
        }
        dir[startLocXModded][startLocYModded] = lastDir;
    }

}
