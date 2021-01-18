package sprintbot;
import javax.sound.midi.Receiver;

import battlecode.common.*;

public class Pathfinding {
    static double[][] map = new double[64][64];

    static int[] receiverStartIdx = new int[4096];
    static int[][] receiverFlags = new int[4096][9];

    public static void updateMapWithReceiverFlag(int receiverID, int flag) {
        int moddedID = receiverID % 4096;
        int moddedRoundNum = RobotPlayer.rc.getRoundNum() % 9;
        int startIdx = receiverStartIdx[moddedID];
        receiverFlags[moddedID][moddedRoundNum] = flag;
        if (startIdx == 0) {
            receiverStartIdx[moddedID] = moddedRoundNum + 1;
        } else if ((moddedRoundNum + 1) % 9 + 1 == startIdx) {
            ReceiverCommunication.parseFlags(receiverFlags[moddedID]);
            MapLocation receiverLoc = getMapLocationFromModded(ReceiverCommunication.moddedLocation);
            for (int i = 48; i >= 0; --i) {
                int x = receiverLoc.x - 3 + (i % 7);
                int y = receiverLoc.y - 3 + (i / 7);
                map[x][y] = ReceiverCommunication.surroundings[i];
            }
        }
    }

    public static void updateMapWithBroadcasterFlag(int broadcasterId, int flag) {
        BroadcasterCommunication.parseFlag(flag);
        if (!BroadcasterCommunication.isPassibilityFlag) return;
        int roundNum = RobotPlayer.rc.getRoundNum(); // TODO: add even/odd check here
        int startLocIdx = broadcasterId + roundNum;
        for (int i = 6; i >= 0; --i) {
            int x = (startLocIdx + i) & MOD_BY_64_MASK;
            int y = ((startLocIdx + i) / 64) & MOD_BY_64_MASK;
            map[x][y] = BroadcasterCommunication.passibilityValues[i];
        }
    }

    public static MapLocation getMapLocationFromModded(MapLocation moddedLoc) {
        return null; // TODO: implement
    }

    /** A* Implementation */

    private static final int BYTECODE_PADDING = 2000;
    private static final int UNKNOWN_OR_OFF_MAP_MOVE_COST = 128;

    private static final int DIVIDE_BY_32768_SHIFT = 15;
    private static final int MOD_BY_32768_MASK = 0x7FFF;
    private static final int MOD_BY_64_MASK = 0x3F;

    static int[] heap = new int[8192];
    static int heapSize = 0;
    static int[][] indexInHeap = new int[64][64];

    static double[][] dist = new double[64][64];
    static Direction[][] directionFromStart = new Direction[64][64];

    static int targetX;
    static int targetY;

    // Heap

    private static void addToHeap(int x, int y) {
        heap[++heapSize] = x | (y << DIVIDE_BY_32768_SHIFT);
        indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK] = heapSize;
        heapifyUp(x, y);
    }

    private static int removeFromHeap() {
        int ret = heap[1];
        heap[1] = heap[heapSize--];
        heapifyDown(heap[1] & MOD_BY_32768_MASK, heap[1] >> DIVIDE_BY_32768_SHIFT);
        return ret;
    }

    private static boolean inHeap(int x, int y) {
        return indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK] <= heapSize && heap[indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK]] == (x | (y << DIVIDE_BY_32768_SHIFT));
    }

    private static void heapifyUp(int x, int y) {
        int locNum = x | (y << DIVIDE_BY_32768_SHIFT);
        double value = value(x, y);
        int idx = indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK];
        while (idx > 1) {
            int pIdx = idx / 2;
            int px = heap[pIdx] & MOD_BY_32768_MASK;
            int py = heap[pIdx] >> DIVIDE_BY_32768_SHIFT;
            double pValue = value(px, py);

            // stop if value is greater than the parent value
            if (value >= pValue) break;

            // swap with parent
            heap[idx] = heap[pIdx];
            heap[pIdx] = locNum;
            indexInHeap[px & MOD_BY_64_MASK][py & MOD_BY_64_MASK] = idx;
            indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK] = pIdx;
            idx = pIdx;
        }
    }

    private static void heapifyDown(int x, int y) {
        int locNum = x | (y << DIVIDE_BY_32768_SHIFT);
        double value = value(x, y);
        int idx = indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK];
        while (idx * 2 <= heapSize) {
            int c1Idx = idx * 2;
            int c2Idx = c1Idx + 1;
            int c1x = heap[c1Idx] & MOD_BY_32768_MASK;
            int c1y = heap[c1Idx] >> DIVIDE_BY_32768_SHIFT;
            int c2x = heap[c2Idx] & MOD_BY_32768_MASK;
            int c2y = heap[c2Idx] >> DIVIDE_BY_32768_SHIFT;
            double c1Value = value(c1x, c1y);
            double c2Value = value(c2x, c2y);

            // stop if value is less than both child values
            if (value <= c1Value && (c2Idx > heapSize || value <= c2Value)) break;

            // swap with smaller child
            if (c2Idx > heapSize || c1Value < c2Value) {
                heap[idx] = heap[c1Idx];
                heap[c1Idx] = locNum;
                indexInHeap[c1x & MOD_BY_64_MASK][c1y & MOD_BY_64_MASK] = idx;
                indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK] = c1Idx;
                idx = c1Idx;
            } else {
                heap[idx] = heap[c2Idx];
                heap[c2Idx] = locNum;
                indexInHeap[c2x & MOD_BY_64_MASK][c2y & MOD_BY_64_MASK] = idx;
                indexInHeap[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK] = c2Idx;
                idx = c2Idx;
            }
        }
    }


    // A* Methods

    private static double value(int x, int y) {
        return dist[x & MOD_BY_64_MASK][y & MOD_BY_64_MASK] + estimatedDistFromTarget(x, y);
    }

    private static int estimatedDistFromTarget(int x, int y) {
        return 2 * Math.max(Math.abs(targetX - x), Math.abs(targetY - y));
    }

    // TODO: add boundary sensing
    public static void moveTo(MapLocation targetLoc, int bytecodesLeft) throws GameActionException {
        heapSize = 0;
        targetX = targetLoc.x;
        targetY = targetLoc.y;

        // add start loc to heap
        MapLocation startLoc = RobotPlayer.rc.getLocation();
        int startX = startLoc.x;
        int startY = startLoc.y;
        dist[startX & MOD_BY_64_MASK][startY & MOD_BY_64_MASK] = 0;
        directionFromStart[startX & MOD_BY_64_MASK][startY & MOD_BY_64_MASK] = Direction.CENTER;
        addToHeap(startX, startY);

        int closestEstimatedDist = Integer.MAX_VALUE;
        Direction moveDir = Direction.CENTER;

        while (Clock.getBytecodesLeft() > bytecodesLeft + BYTECODE_PADDING) {
            // pull min value location from heap
            int locNum = removeFromHeap();

            int x = locNum & MOD_BY_32768_MASK;
            int y = locNum >> DIVIDE_BY_32768_SHIFT;
            int xModded = x & MOD_BY_64_MASK;
            int yModded = y & MOD_BY_64_MASK;

            // save direction to closest estimated dist just in case we don't finish A*
            int estimatedDist = estimatedDistFromTarget(x, y);
            if (estimatedDist < closestEstimatedDist) {
                moveDir = directionFromStart[xModded][yModded];
                closestEstimatedDist = estimatedDist;
            }

            // break if target loc is found
            if (x == targetX && y == targetY) break;

            MapLocation loc = new MapLocation(x, y);

            // add adjacent locs to priority queue
            Direction dir = Direction.NORTH;
            for (int i = 7; i >= 0; --i) {
                MapLocation adjLoc = loc.add(dir);
                int adjXModded = adjLoc.x & MOD_BY_64_MASK;
                int adjYModded = adjLoc.y & MOD_BY_64_MASK;

                // update map value at location if necessary/possible
                double moveCost = map[adjXModded][adjYModded];
                if (moveCost == ReceiverCommunication.UNKNOWN_OR_OFF_MAP) {
                    if (RobotPlayer.rc.canSenseLocation(adjLoc)) {
                        map[adjXModded][adjYModded] = 1 / RobotPlayer.rc.sensePassability(adjLoc);
                        moveCost = map[adjXModded][adjYModded];
                    } else {
                        moveCost = UNKNOWN_OR_OFF_MAP_MOVE_COST;
                    }
                }

                double newDist = dist[xModded][yModded] + moveCost;

                // TODO: should check if is visited or added to heap instead of just if added to heap
                boolean alreadyInHeap = inHeap(adjLoc.x, adjLoc.y);
                if (newDist < dist[adjXModded][adjYModded] || !alreadyInHeap) {
                    dist[adjXModded][adjYModded] = newDist;
                    
                    // pass on direction to adj child
                    if (directionFromStart[xModded][yModded] == Direction.CENTER) {
                        directionFromStart[adjXModded][adjYModded] = loc.directionTo(adjLoc);
                    } else {
                        directionFromStart[adjXModded][adjYModded] = directionFromStart[xModded][yModded];
                    }

                    // add adj child to heap
                    if (alreadyInHeap) {
                        heapifyUp(adjLoc.x, adjLoc.y);
                    } else {
                        addToHeap(adjLoc.x, adjLoc.y);
                    }
                }
                dir = dir.rotateRight();
            }
        }

        // try to move in given direction
        if (RobotPlayer.rc.canMove(moveDir)) {
            RobotPlayer.rc.move(moveDir);
        }
    }

    public static int getIntFromDoublePassability(double p) {
        /**
         * 1. range is [0.1, 1] meaning cd range is [1, 10]
         * 2. subtract 1 from cd range to make range [0, 9]
         * 3. normalize, scale by 7 and round to nearest integer
         * 4. add one to offset from UNKNOWN_OR_OFF_MAP const
         */
        return (int)Math.round(7 * (1 - p) / (9 * p)) + 1;
    }

}
