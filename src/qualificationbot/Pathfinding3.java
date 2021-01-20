package qualificationbot;
import java.util.Arrays;

import battlecode.common.*;

public class Pathfinding3 {
    public static final int MAX_SENSE_RANGE = 5;

    private static final int MAP_SIZE = 64;
    private static final int MAP_MASK = MAP_SIZE - 1;
    private static double[][] map = new double[MAP_SIZE][MAP_SIZE];

    private static double[][] dist = new double[MAX_SENSE_RANGE + 1][MAX_SENSE_RANGE * 2 + 1];
    private static MapLocation[][] mapLoc = new MapLocation[MAX_SENSE_RANGE + 1][MAX_SENSE_RANGE * 2 + 1];
    private static int[][] par = new int[MAX_SENSE_RANGE + 1][MAX_SENSE_RANGE * 2 + 1];

    public static boolean moveTo(MapLocation targetLoc) throws GameActionException {
        if (RobotPlayer.rc.getCooldownTurns() >= 1) return true;
        Direction dir = directionTo(targetLoc);
        if (RobotPlayer.rc.canMove(dir)) {
            RobotPlayer.rc.move(dir);
            return true;
        }
        return false;
    }

    static MapLocation randomTargetLoc;

    public static void moveToRandomTarget() throws GameActionException {
        if (randomTargetLoc == null) {
            MapLocation randomSectionLoc = new MapLocation((int)(Math.random() * Communication.NUM_SECTIONS),
                                                           (int)(Math.random() * Communication.NUM_SECTIONS));
            randomTargetLoc = Communication.getSectionCenterLoc(randomSectionLoc);
        }
        if (!Pathfinding3.moveTo(randomTargetLoc)) {
            randomTargetLoc = null;
        }
    }

    public static void stickToTarget(MapLocation targetLoc) throws GameActionException {
        MapLocation targetAdjLoc = getOpenAdjacentLoc(targetLoc);
        if (targetAdjLoc != null) {
            moveTo(targetAdjLoc);
        }
    }

    public static MapLocation getOpenAdjacentLoc(MapLocation loc) {
        MapLocation curLoc = RobotPlayer.rc.getLocation();
        if (curLoc.isWithinDistanceSquared(loc, 2)) {
            return curLoc;
        }
        Direction dir = loc.directionTo(curLoc);
        for (int i = 7; i >= 0; i--) {
            MapLocation adjLoc = loc.add(dir);
            if (RobotPlayer.rc.canSenseLocation(adjLoc) && !RobotPlayer.rc.isLocationOccupied(adjLoc)) {
                return adjLoc;
            }
            dir = dir.rotateLeft();
        }
        return null;
    }

    private static Direction directionTo(MapLocation targetLoc) throws GameActionException {
        MapLocation startLoc = RobotPlayer.rc.getLocation();
        if (startLoc.equals(targetLoc)) return Direction.CENTER;
        
        Direction straightDir = startLoc.directionTo(targetLoc);
        Direction leftDir = straightDir.rotateLeft();
        Direction rightDir = straightDir.rotateRight();

        int dirIdx = getDirectionIdx(straightDir);

        int level = MAX_SENSE_RANGE;
        
        // compute distances
        dist[0][0] = 0;
        mapLoc[0][0] = startLoc;
        for (int l = 1; l <= level; ++l) {
            int prevLevel = l - 1;
            for (int i = 0; i <= l * 2; ++i) {
                // find best parent
                int[] pars = parent[dirIdx][l][i];
                double prevDist = dist[prevLevel][pars[0]];
                par[l][i] = pars[0];
                for (int p = 1; p < pars.length; ++p) {
                    if (dist[prevLevel][pars[p]] < prevDist) {
                        prevDist = dist[prevLevel][pars[p]];
                        par[l][i] = pars[p];
                    }
                }

                // find direction from 0th parent
                int dirRotateIdx = dirFromFirstParent[l][i];
                Direction dir;
                switch (dirRotateIdx) {
                    case -1:
                        dir = leftDir;
                        break;
                    case 1:
                        dir = rightDir;
                        break;
                    default:
                        dir = straightDir;
                        break;
                }

                // set map location using 0th parent loc and dir and get distance
                mapLoc[l][i] = mapLoc[prevLevel][pars[0]].add(dir);
                dist[l][i] = getDistTo(mapLoc[l][i], prevDist);

                // stop the search if target loc is reached
                if (mapLoc[l][i].equals(targetLoc)) {
                    level = l;
                }
            }
        }

        int bestIdx = -1;
        double closestTotDist = Double.MAX_VALUE;
        for (int i = 0; i <= level * 2; ++i) {
            if (mapLoc[level][i].equals(targetLoc)) {
                bestIdx = i;
                break;
            }
            double totDist = dist[level][i] + getEstimatedDist(mapLoc[level][i], targetLoc);
            if (totDist < closestTotDist) {
                bestIdx = i;
                closestTotDist = totDist;
            }
        }

        // if none of the edge locations are reachable, pick the middle one
        if (bestIdx == -1) bestIdx = level;

        // backtrack to find direction and display path for debugging
        int idx = bestIdx;
        for (int l = level; l > 1; --l) {
            RobotPlayer.rc.setIndicatorLine(mapLoc[l][idx], mapLoc[l - 1][par[l][idx]], 255, 0, 255);
            idx = par[l][idx];
        }

        switch (idx) {
            case 0:
                return leftDir;
            case 1:
                return straightDir;
            case 2:
                return rightDir;
            default:
                return null;
        }
    }

    private static double AVERAGE_COOLDOWN = 2;

    private static double getEstimatedDist(MapLocation loc1, MapLocation loc2) {
        return AVERAGE_COOLDOWN * (Math.abs(loc1.x - loc2.x) + Math.abs(loc1.y - loc2.y));
    }

    private static boolean inSensorRange(MapLocation loc) {
        return RobotPlayer.rc.canSenseRadiusSquared(loc.distanceSquaredTo(RobotPlayer.rc.getLocation()));
    }

    private static double getDistTo(MapLocation loc, double prevDist) throws GameActionException {
        int xModded = loc.x & MAP_MASK;
        int yModded = loc.y & MAP_MASK;
        if (inSensorRange(loc)) {
            if (!RobotPlayer.rc.onTheMap(loc)) return Double.MAX_VALUE;
            map[xModded][yModded] = map[xModded][yModded] == 0 ? 1 / RobotPlayer.rc.sensePassability(loc) : map[xModded][yModded];
            if (RobotPlayer.rc.isLocationOccupied(loc)) {
                return Double.MAX_VALUE;
            }
        } else {
            return prevDist + AVERAGE_COOLDOWN;
        }
        return prevDist + map[xModded][yModded];
    }

    private static final int DIAG_IDX = 1;
    private static final int CARD_IDX = 0;

    private static int getDirectionIdx(Direction dir) {
        switch (dir) {
            case NORTH:
                return CARD_IDX;
            case SOUTH:
                return CARD_IDX;
            case EAST:
                return CARD_IDX;
            case WEST:
                return CARD_IDX;
            case NORTHEAST:
                return DIAG_IDX;
            case NORTHWEST:
                return DIAG_IDX;
            case SOUTHEAST:
                return DIAG_IDX;
            case SOUTHWEST:
                return DIAG_IDX;
            default:
                return -1;
        }
    }

    private static final int[][][][] parent = new int[][][][] {
        // cardinal
        new int[][][] {
            // level 0
            new int[][] {},
            // level 1
            new int[][] {
                new int[] {0},
                new int[] {0},
                new int[] {0},
            },
            // level 2
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1, 0, 2},
                new int[] {2, 1},
                new int[] {2},
            },
            // level 3
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1, 0, 2},
                new int[] {2, 1, 3},
                new int[] {3, 2, 4},
                new int[] {4, 3},
                new int[] {4},
            },
            // level 4
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1, 0, 2},
                new int[] {2, 1, 3},
                new int[] {3, 2, 4},
                new int[] {4, 3, 5},
                new int[] {5, 4, 6},
                new int[] {6, 5},
                new int[] {6},
            },
            // level 5
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1, 0, 2},
                new int[] {2, 1, 3},
                new int[] {3, 2, 4},
                new int[] {4, 3, 5},
                new int[] {5, 4, 6},
                new int[] {6, 5, 7},
                new int[] {7, 6, 8},
                new int[] {8, 7},
                new int[] {8},
            }
        },
        // diagonal
        new int[][][] {
            // level 0
            new int[][] {},
            // level 1
            new int[][] {
                new int[] {0},
                new int[] {0},
                new int[] {0},
            },
            // level 2
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1},
                new int[] {2, 1},
                new int[] {2},
            },
            // level 3
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1, 2},
                new int[] {2},
                new int[] {3, 2},
                new int[] {4, 3},
                new int[] {4}
            },
            // level 4
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1, 2},
                new int[] {2, 3},
                new int[] {3},
                new int[] {4, 3},
                new int[] {5, 4},
                new int[] {6, 5},
                new int[] {6}
            },
            // level 5
            new int[][] {
                new int[] {0},
                new int[] {0, 1},
                new int[] {1, 2},
                new int[] {2, 3},
                new int[] {3, 4},
                new int[] {4},
                new int[] {5, 4},
                new int[] {6, 5},
                new int[] {7, 6},
                new int[] {8, 7},
                new int[] {8}
            },
        }
    };

    private static final int[][] dirFromFirstParent = new int[][] {
        // level 0
        new int[] {},
        // level 1
        new int[] {-1, 0, 1},
        // level 2
        new int[] {-1, 0, 0, 0, 1},
        // level 3
        new int[] {-1, 0, 0, 0, 0, 0, 1},
        // level 4
        new int[] {-1, 0, 0, 0, 0, 0, 0, 0, 1},
        // level 5
        new int[] {-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}
    };
}
