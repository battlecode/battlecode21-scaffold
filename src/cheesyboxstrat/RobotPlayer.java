package cheesyboxstrat;
import battlecode.common.*;

public strictfp class RobotPlayer {
    public static final MapLocation slandererBoxCornerOffset = new MapLocation(-5, -4);
    // box near corner offset is (0, 0)
    public static final MapLocation muckrakerBoxFarCornerOffset = new MapLocation(-9, -8);
    public static final Direction muckrackerBoxOuterDiagonalDir = Direction.NORTHEAST;

    static RobotController rc;

    static MapLocation startLoc;
    static MapLocation enlightenmentCenterLoc;

    static int turnCount;
    static int slanderersBuilt;
    static int lastSlandererInfluence;

    static int muckrakerBoxLevel;
    
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        initialize(rc);

        while (true) {
            try {
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }
                Clock.yield();
                turnCount += 1;
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void initialize(RobotController rc) {
        RobotPlayer.rc = rc;

        RobotPlayer.startLoc = rc.getLocation();
        RobotInfo enlightenmentCenter = getClosestEnlightenmentCenter();
        if (enlightenmentCenter != null) {
            RobotPlayer.enlightenmentCenterLoc = enlightenmentCenter.getLocation();
        }

        RobotPlayer.turnCount = 0;
        RobotPlayer.slanderersBuilt = 0;

        if (rc.getType() == RobotType.MUCKRAKER) {
            RobotPlayer.muckrakerBoxLevel = 0;
        }
    }

    static RobotInfo getClosestEnlightenmentCenter() {
        RobotInfo closestEnlightenmentCenter = null;
        int closestDist = Integer.MAX_VALUE;
        for (RobotInfo ri : rc.senseNearbyRobots()) {
            if (ri.getType() == RobotType.ENLIGHTENMENT_CENTER &&
                    squaredDist(ri.getLocation(), rc.getLocation()) < closestDist) {
                closestEnlightenmentCenter = ri;
                closestDist = squaredDist(ri.getLocation(), rc.getLocation());
            }
        }
        return closestEnlightenmentCenter;
    }

    static void runEnlightenmentCenter() throws GameActionException {
        // System.out.println("influence : " + rc.getInfluence());
        int spaceLeft = Integer.MAX_VALUE - rc.getInfluence();
        int passiveIncome = 1000000 + (int)(lastSlandererInfluence * GameConstants.PASSIVE_INFLUENCE_RATIO_SLANDERER);
        if (passiveIncome > spaceLeft) {
            // System.out.println("big bid : " + (passiveIncome - spaceLeft));
            rc.bid(passiveIncome - spaceLeft);
        } else {
            rc.bid(2);
        }

        // Build a slanderer every 50 turns
        int influence = rc.getInfluence();
        Direction slandererBuildDir = muckrackerBoxOuterDiagonalDir.opposite();
        if (slanderersBuilt <= turnCount / 50 && rc.canBuildRobot(RobotType.SLANDERER, slandererBuildDir, influence)) {
            rc.buildRobot(RobotType.SLANDERER, slandererBuildDir, influence);
            slanderersBuilt++;
            lastSlandererInfluence = influence;
            return;
        }

        Direction outerBoxBuildDir = muckrackerBoxOuterDiagonalDir;
        Direction innerBoxLeftBuildDir = muckrackerBoxOuterDiagonalDir.rotateLeft().rotateLeft().rotateLeft();
        Direction innerBoxRightBuildDir = muckrackerBoxOuterDiagonalDir.rotateRight().rotateRight().rotateRight();

        // Otherwise, add to the box of muckrakers
        if (rc.canBuildRobot(RobotType.MUCKRAKER, innerBoxLeftBuildDir, 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, innerBoxLeftBuildDir, 1);
        } else if (rc.canBuildRobot(RobotType.MUCKRAKER, innerBoxRightBuildDir, 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, innerBoxRightBuildDir, 1);
        } else if (rc.canBuildRobot(RobotType.MUCKRAKER, outerBoxBuildDir, 1)) {
            rc.buildRobot(RobotType.MUCKRAKER, outerBoxBuildDir, 1);
        }
    }
    
    static void runPolitician() throws GameActionException {
        rc.empower(0);
    }

    static void runSlanderer() throws GameActionException {
        MapLocation slandererBoxCorner = addLocs(enlightenmentCenterLoc, slandererBoxCornerOffset);
        moveTo(slandererBoxCorner, true);
    }

    static void runMuckraker() throws GameActionException {
        MapLocation innerBoxNearCorner = enlightenmentCenterLoc;
        MapLocation innerBoxFarCorner = addLocs(enlightenmentCenterLoc, muckrakerBoxFarCornerOffset);

        // when on diagonal, move outwards until there is space found along box
        int dx = rc.getLocation().x - enlightenmentCenterLoc.x;
        int dy = rc.getLocation().y - enlightenmentCenterLoc.y;
        if (Math.abs(dx) == Math.abs(dy) &&
                enlightenmentCenterLoc.directionTo(rc.getLocation()) == muckrackerBoxOuterDiagonalDir) {
            if (muckrakerBoxLevel == 0) muckrakerBoxLevel = 1;
            Direction leftBackDir = muckrackerBoxOuterDiagonalDir.rotateLeft().rotateLeft().rotateLeft();
            Direction rightBackDir = muckrackerBoxOuterDiagonalDir.rotateRight().rotateRight().rotateRight();
            if (rc.canMove(leftBackDir)) {
                rc.move(leftBackDir);
            } else if (rc.canMove(rightBackDir)) {
                rc.move(rightBackDir);
            } else if (rc.canMove(muckrackerBoxOuterDiagonalDir)) {
                rc.move(muckrackerBoxOuterDiagonalDir);
                muckrakerBoxLevel++;
            }
            return;
        }

        MapLocation boxNearCorner = enlightenmentCenterLoc;
        MapLocation boxFarCorner = addLocs(enlightenmentCenterLoc, muckrakerBoxFarCornerOffset);
        for (int i = 0; i < muckrakerBoxLevel; i++) {
            boxNearCorner = boxNearCorner.add(muckrackerBoxOuterDiagonalDir);
            boxFarCorner = boxFarCorner.add(muckrackerBoxOuterDiagonalDir.opposite());
        }    

        // otherwise, move around box
        Direction boxSide = getRobotBoxSide(boxNearCorner, boxFarCorner);
        if (boxSide == Direction.NORTH || boxSide == Direction.WEST) {
            moveAlongBox(boxFarCorner, true);
        } else if (boxSide == Direction.SOUTH || boxSide == Direction.EAST) {
            moveAlongBox(boxFarCorner, false);
        }
    }

    static MapLocation getBoxFarCornerOffset(int level) {
        if (muckrackerBoxOuterDiagonalDir == Direction.NORTHEAST) {
            return new MapLocation(muckrakerBoxFarCornerOffset.x - level, muckrakerBoxFarCornerOffset.y - level);
        } else if (muckrackerBoxOuterDiagonalDir == Direction.NORTHWEST) {
            return new MapLocation(muckrakerBoxFarCornerOffset.x + level, muckrakerBoxFarCornerOffset.y - level);
        } else if (muckrackerBoxOuterDiagonalDir == Direction.SOUTHEAST) {
            return new MapLocation(muckrakerBoxFarCornerOffset.x - level, muckrakerBoxFarCornerOffset.y + level);
        } else if (muckrackerBoxOuterDiagonalDir == Direction.SOUTHWEST) {
            return new MapLocation(muckrakerBoxFarCornerOffset.x + level, muckrakerBoxFarCornerOffset.y + level);
        }
        
        // this should not happen as direction should be diagonal
        return null;
    }

    /** Movement Methods */

    static void moveAlongBox(MapLocation boxCorner, boolean horizFirst) throws GameActionException {
        Direction horizDir = boxCorner.x > rc.getLocation().x ? Direction.EAST : Direction.WEST;
        Direction vertDir = boxCorner.y > rc.getLocation().y ? Direction.NORTH : Direction.SOUTH;
        if (rc.canMove(horizDir) && rc.getLocation().x != boxCorner.x &&
                (horizFirst || boxCorner.y == rc.getLocation().y)) {
            rc.move(horizDir);
        } else if (rc.canMove(vertDir) && rc.getLocation().y != boxCorner.y &&
                (!horizFirst || boxCorner.x == rc.getLocation().x)) {
            rc.move(vertDir);
        }
    }

    static void moveTo(MapLocation loc, boolean adjustDiagDir) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(loc);
        if (rc.canMove(dir)) {
            rc.move(dir);
        } else if (adjustDiagDir && !isCardinalDirection(dir)) {
            if (rc.canMove(dir.rotateLeft())) {
                rc.move(dir.rotateLeft());
            } else if (rc.canMove(dir.rotateRight())) {
                rc.move(dir.rotateRight());
            }
        }
    }

    /** Helper Methods */

    static boolean isCardinalDirection(Direction dir) {
        return dir.dx == 0 || dir.dy == 0;
    }

    static int squaredDist(MapLocation l1, MapLocation l2) {
        return (l1.x - l2.x)*(l1.x - l2.x) + (l1.y - l2.y)*(l1.y - l2.y);
    }

    static MapLocation addLocs(MapLocation l1, MapLocation l2) {
        return new MapLocation(l1.x + l2.x, l1.y + l2.y);
    }

    static Direction getRobotBoxSide(MapLocation nearCorner, MapLocation farCorner) {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        int minX = Math.min(nearCorner.x, farCorner.x);
        int maxX = Math.max(nearCorner.x, farCorner.x);
        int minY = Math.min(nearCorner.y, farCorner.y);
        int maxY = Math.max(nearCorner.y, farCorner.y);

        if (x < minX || x > maxX || y < minY || y > maxY) return Direction.CENTER;

        if (x == minX) return Direction.WEST;
        if (x == maxX) return Direction.EAST;
        if (y == minY) return Direction.SOUTH;
        if (y == maxY) return Direction.NORTH;

        return Direction.CENTER;
    }
}
