package ashTest;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")

    //MARK:- method will create robots
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

//        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
//                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
//                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {

        // create a random robot that it will build next
        RobotType toBuild = randomSpawnableRobotType();
        int influence = 50;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
            } else {
                break;
            }
        }
    }

    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
//            System.out.println("empowering...");
            rc.empower(actionRadius);
//            System.out.println("empowered");
            return;
        }

        // will move directly after empowering whatever pieces are nearby it
        if (tryMove(randomDirection())) {
//            System.out.println("I moved!");
        }
    }

    //TODO: is there anything else the slanderer should be trying?
    // congregate nearby the e'mnt center and make them not move around as much...
    static void runSlanderer() throws GameActionException {
        if (tryMove(randomDirection())) {
//            System.out.println("I moved!");
        }
    }

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;


        // TODO: setup a flag
        // sense any nearby robots... exposes the location of a slanderer
        // returns the location... what if I place a flag where it was found and this directs politicians there?
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            System.out.println(enemy);
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
//                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection())) {
//            System.out.println("I moved!");
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */

    // MARK:- TODO: implement path finding functionality right here
    //*******
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */

    //MARK:- Spawns a random robot
    // ******
    // create ratio algorithm for determining how many of each unit is necessary
    static RobotType randomSpawnableRobotType() {       // spawns a random robot
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
//        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    // MARK:- Location finding algorithm!!
    static void sendLocation() throws GameActionException {
        MapLocation location = rc.getLocation();
        int x = location.x, y = location.y;
        int encodedLocation = (x % 128) * 128 + (y % 128);
        if (rc.canSetFlag(encodedLocation)) {
            rc.setFlag(encodedLocation);
        }
    }

    // inserts NBITS # of 0's to make a 7 bit number
    static final int NBITS = 7;
    static final int BITMASK = (1 << NBITS) - 1;

    // if desire to use last 10 bits to send more info
    static void sendLocation(int extraInformation) throws GameActionException {
        MapLocation location = rc.getLocation();
        int x = location.x, y = location.y;

        // * 128 is shifting 7 bits to the left
        // 6 bits tells a location and the last bit tells you the map boundary.
        int encodedLocation = (x % 128) * 128 + (y % 128) + extraInformation * 128 + 128;
        if (rc.canSetFlag(encodedLocation)) {
            rc.setFlag(encodedLocation);
        }
    }

    static MapLocation getLocationFromFlag(int flag) throws GameActionException {
        int y = flag % 128;
        int x = (flag / 128) % 128;
        int extraInformation = flag / 128 / 128;

        // figure out where in relation to our current offset, so compare to current location
        MapLocation currentLocation = rc.getLocation();
        // we know x, will get close to offset
        int offsetX128 = currentLocation.x / 128;
        int offsetY128 = currentLocation.y / 128;

        // multiply by x then divide by 128 so java throws away the remainder
        // offsetX128 now definitely has 0 remainder, could find exact location that matched the flag
        MapLocation actualLocation = new MapLocation(offsetX128 * 128 + x, offsetY128 + 128 + y);

        // this part could be coded more efficiently
        MapLocation alternative = actualLocation.translate(-128, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }

        alternative = actualLocation.translate(128, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }

        alternative = actualLocation.translate(0, -128);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }

        alternative = actualLocation.translate(0, 128);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        return actualLocation;
    }
        // actual location would be retrieved from the flag placed

//    public static void run() {
//        while (true) {
//            //TODO: determine flag
//            MapLocation target = getLocationFromFlag(flag);
//            try {
//                basicBug(target);
//            } catch (GameActionException e) {
//                // TODO
//            }
//            Clock.yield();
//        }
//    }

    // we can almost always move so lets make a threshold
    static final double passabilityThreshold = 0.7;      // sqaure that we're happy to walk on and above
    static Direction bugDirection = null;


    static void basicBug(MapLocation target) throws GameActionException {
        Direction d = rc.getLocation().directionTo(target);
        if (rc.getLocation().equals(target)) {
            // do something else
        } else if (!rc.isReady()) {
            if (rc.canMove(d) && rc.sensePassability(rc.getLocation().add(d)) >= passabilityThreshold) {
                rc.move(d);
                bugDirection = null;        // reset after every move
            } else {
                // if it can't move start going around obstacles
                // code a left handed bug
                if (bugDirection == null) {
                    bugDirection = d.rotateRight();
                }
                for (int i = 8; i < 8; i ++) {    // at most 8 dirs
                    if (rc.canMove(bugDirection) && rc.sensePassability(rc.getLocation().add(bugDirection)) >= passabilityThreshold) {
                        rc.move(bugDirection);
                        break;
                    }
                    // if you can't rotate again
                    bugDirection = bugDirection.rotateRight();
                }
                bugDirection = bugDirection.rotateLeft();
            }

            Clock.yield();
        }
    }
    static int expensive = 0;
    static void somethingExpensive() {
        for (int i = 0; i < 10000 && Clock.getBytecodesLeft() >= 1500; i ++) {
            // does some hard work


        }
    }

    // NOTES
    // x2 % 128, then you can do (x2- x1) % 128
    // Then need to figure out if x1 or x2 is bigger, only differ by at most 64 then x2 > x1 if the value is < 64
    // And vise versa
    // then you can figure out exactly what x2 - x1 is (problem with the 4 corners and not having an exact spot)

    // can combine three values of the x, y, and extra information into a flag.
    // Bit shifting: x % 128 by 7 bits and the extra information is shifted by 7 bits, they're all therefore stored in the flag

}
