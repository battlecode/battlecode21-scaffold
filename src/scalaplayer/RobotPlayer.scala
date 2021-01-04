package scalaplayer

import battlecode.common._
import battlecode.common.RobotType._

object RobotPlayer {
  private var rc: RobotController = _
  private val directions = Array(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
  private val spawnedByMiner = Array(RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN)
  private var turnCount = 0

  /**
   * run() is the method that is called when a robot is instantiated in the Battlecode world.
   * If this method returns, the robot dies!
   **/
  @SuppressWarnings(Array("unused"))
  @throws[GameActionException]
  def run(rc: RobotController): Unit = { // This is the RobotController object. You use it to perform actions from this robot,
    // and to get information on its current status.
    RobotPlayer.rc = rc
    turnCount = 0
    System.out.println("I'm a " + rc.getType + " and I just got created!")
    while (true) {
      turnCount += 1

      // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
      try { // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        System.out.println("I'm a " + rc.getType + "! Location " + rc.getLocation)
        rc.getType match {
          case HQ => runHQ()
          case MINER => runMiner()
          case REFINERY => runRefinery()
          case VAPORATOR => runVaporator()
          case DESIGN_SCHOOL => runDesignSchool()
          case FULFILLMENT_CENTER => runFulfillmentCenter()
          case LANDSCAPER => runLandscaper()
          case DELIVERY_DRONE => runDeliveryDrone()
          case NET_GUN => runNetGun()
        }
        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.`yield`()
      } catch {
        case e: Exception =>
          System.out.println(rc.getType + " Exception")
          e.printStackTrace()
      }
    }
  }

  @throws[GameActionException]
  private def runHQ(): Unit = {
    for (dir <- directions) {
      tryBuild(RobotType.MINER, dir)
    }
  }

  @throws[GameActionException]
  private def runMiner(): Unit = {
    tryBlockchain()
    tryMove(randomDirection)
    if (tryMove(randomDirection)) System.out.println("I moved!")
    // tryBuild(randomSpawnedByMiner(), randomDirection());
    for (dir <- directions) {
      tryBuild(RobotType.FULFILLMENT_CENTER, dir)
    }
    for (dir <- directions) {
      if (tryRefine(dir)) System.out.println("I refined soup! " + rc.getTeamSoup)
    }
    for (dir <- directions) {
      if (tryMine(dir)) System.out.println("I mined soup! " + rc.getSoupCarrying)
    }
  }

  @throws[GameActionException]
  private def runRefinery(): Unit = {
    // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
  }

  @throws[GameActionException]
  private def runVaporator(): Unit = {
  }

  @throws[GameActionException]
  private def runDesignSchool(): Unit = {
  }

  @throws[GameActionException]
  private def runFulfillmentCenter(): Unit = {
    for (dir <- directions) {
      tryBuild(RobotType.DELIVERY_DRONE, dir)
    }
  }

  @throws[GameActionException]
  private def runLandscaper(): Unit = {
  }

  @throws[GameActionException]
  private def runDeliveryDrone(): Unit = {
    val enemy = rc.getTeam.opponent
    if (!rc.isCurrentlyHoldingUnit) { // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
      val robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy)
      if (robots.nonEmpty) { // Pick up a first robot within range
        rc.pickUpUnit(robots(0).getID)
        System.out.println("I picked up " + robots(0).getID + "!")
      }
    }
    else { // No close robots, so search for robots within sight radius
      tryMove(randomDirection)
    }
  }

  @throws[GameActionException]
  private def runNetGun(): Unit = {
  }

  /**
   * Returns a random Direction.
   *
   * @return a random Direction
   */
  private def randomDirection = directions((Math.random * directions.length).toInt)

  /**
   * Returns a random RobotType spawned by miners.
   *
   * @return a random RobotType
   */
  private def randomSpawnedByMiner = spawnedByMiner((Math.random * spawnedByMiner.length).toInt)

  @throws[GameActionException]
  private def tryMove: Boolean = {
    for (dir <- directions) {
      if (tryMove(dir)) return true
    }
    false
  }

  /**
   * Attempts to move in a given direction.
   *
   * @param dir The intended direction of movement
   * @return true if a move was performed
   * @throws GameActionException
   */
  @throws[GameActionException]
  private def tryMove(dir: Direction) = { // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
    if (rc.isReady && rc.canMove(dir)) {
      rc.move(dir)
      true
    }
    else false
  }

  /**
   * Attempts to build a given robot in a given direction.
   *
   * @param type The type of the robot to build
   * @param dir  The intended direction of movement
   * @return true if a move was performed
   * @throws GameActionException
   */
  @throws[GameActionException]
  private def tryBuild(`type`: RobotType, dir: Direction) = if (rc.isReady && rc.canBuildRobot(`type`, dir)) {
    rc.buildRobot(`type`, dir)
    true
  }
  else false

  /**
   * Attempts to mine soup in a given direction.
   *
   * @param dir The intended direction of mining
   * @return true if a move was performed
   * @throws GameActionException
   */
  @throws[GameActionException]
  private def tryMine(dir: Direction) = if (rc.isReady && rc.canMineSoup(dir)) {
    rc.mineSoup(dir)
    true
  }
  else false

  /**
   * Attempts to refine soup in a given direction.
   *
   * @param dir The intended direction of refining
   * @return true if a move was performed
   * @throws GameActionException
   */
  @throws[GameActionException]
  private def tryRefine(dir: Direction) = if (rc.isReady && rc.canDepositSoup(dir)) {
    rc.depositSoup(dir, rc.getSoupCarrying)
    true
  }
  else false

  @throws[GameActionException]
  private def tryBlockchain(): Unit = {
    if (turnCount < 3) {
      val message = new Array[Int](7)
      for (i <- 0 until 7) {
        message(i) = 123
      }
      if (rc.canSubmitTransaction(message, 10)) rc.submitTransaction(message, 10)
    }
    // System.out.println(rc.getRoundMessages(turnCount-1));
  }
}
