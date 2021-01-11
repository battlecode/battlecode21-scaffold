package mainrobot;

import battlecode.common.MapLocation;


public class Communication {
	
	// Team Code, always start with a 1, otherwise problems!
	private final static String TEAM_CODE = "1011";

	// Group Codes
	private final static String CREATED_BOTS_GROUP = "000";
	public final static String MUCKRAKER_GROUP = "001";
	public final static String POLITICIAN_GROUP = "010";
	public final static String SLANDERER_GROUP = "011";
	
	// Subject Codes
	private final static String DEFAULT_SUBJECT = "000";
	public final static String ENEMY_EC_SUBJECT = "001";
	public final static String ENEMY_ROBOT_SUBJECT = "010";
	public final static String NEUTRAL_EC_SUBJECT = "011";
	
	
	/*//////////////////////////////////////////////////////////////
	 * 
	 * 		The following are for making a flag.
	 * 
	 */////////////////////////////////////////////////////////////
	
	
	/**
	 * Takes a group to assign the made bot and a location to send it to. Returns
	 * the flag that should be displayed the next round to setup the bot.
	 * 
	 * @param assignedBotGroup String, group to make the bot join
	 * @param assignedLocation MapLocation, location for the bot to go to
	 * @return an int with the information contained for the flag to display
	 */
	public static int makeFlagForCreatedBot(String assignedBotGroup, MapLocation assignedLocation) {
		
		String flagString = TEAM_CODE // 4 Bits
				+ CREATED_BOTS_GROUP // 3 Bits
				+ assignedBotGroup // 3 Bits
				+ make14BitStringFromMapLocation(assignedLocation); // 14 Bits
		return Integer.parseInt(flagString, 2);
		
	}
	
	/**
	 * Takes a group code of the bots to control and a MapLocation of where they 
	 * should go. Outputs the integer that the flag should be set to.
	 * 
	 * @param botGroup String, the group to be commanded
	 * @param locationCommand MapLocation, location for the group to go to
	 * @return an int with the information that adequately controls the robots
	 */
	public static int makeFlagToCommandGroup(String botGroup, MapLocation locationCommand) {
		
		String flagString = TEAM_CODE // 4 Bits
				+ botGroup // 3 Bits
				+ "000" // 3 Bits, placeholder because the bot does not need a subject
				+ make14BitStringFromMapLocation(locationCommand); // 14 Bits
		return Integer.parseInt(flagString, 2);
		
	}
	
	/**
	 * Takes a subject of what a location is and the location of what the robot wishes
	 * to report. Returns an integer of the flag to use to convey this information.
	 * 
	 * @param subject String, the subject of what the location is that the bot is reporting
	 * @param location MapLocation, the location of what the robot is reporting
	 * @return an int with the information that will be reported to the EC
	 */
	public static int makeFlagForEC(String subject, MapLocation location) {
		
		String flagString = TEAM_CODE // 4 Bits
				+ "000" // 3 Bits, no 'to' field of the group the info is meant for, EC will get it anyway
				+ subject // 3 Bits
				+ make14BitStringFromMapLocation(location); // 14 Bits
		return Integer.parseInt(flagString, 2);
		
	}
	
	/**
	 * Returns the flag to be used when there is no other flag being used.
	 * WARNING: If this is not used and the flag is 0, it will no longer be checked 
	 * by the EC as its ID will be removed from the list.
	 * 
	 * @return a flag that will be defaulted to when no information is being reported
	 */
	public static int makeDefaultFlag() {
		
		String flagString = TEAM_CODE // 4 Bits
				+ "000" // 3 Bits
				+ DEFAULT_SUBJECT // 3 Bits
				+ "00000000000000"; // 14 Bits
		return Integer.parseInt(flagString, 2);
		
	}
	
	
	/*//////////////////////////////////////////////////////////////
	 * 
	 * 		The following are for reading a flag.
	 * 
	 */////////////////////////////////////////////////////////////
	
	
	/**
	 * Verifies that the robot is still on the friendly team by checking 
	 * the flag team code to make sure it matches. True or False. Used by the 
	 * EC when it is looping through its bank of robot ID's
	 * 
	 * @param flag int, the flag that the controlled robot displays
	 * @return boolean whether the flag has the correct team code.
	 */
	public static boolean verifyTeamCode(int flag) {
		
		String stringFlag = Integer.toBinaryString(flag);
		return stringFlag.substring(0, 4).equals(TEAM_CODE);
		
	}
	
	/**
	 * This is meant to be used when the moveable robots are getting the flag 
	 * from the EC they report to. It returns the code within the flag which tell which group 
	 * the flag is meant for. If this.equals(assignedBotGroup), then the information in the flag should
	 * be considered, otherwise the flag should be ignored.
	 * 
	 * @param flag int, flag of the local EC
	 * @return String of the group code that the flag contains
	 */
	public static String getGroupToFromFlag(int flag) {
		
		String stringFlag = Integer.toBinaryString(flag);
		return stringFlag.substring(4, 7);
		
	}
	
	/**
	 * This is meant to be used ONLY for when a bot is being created and 
	 * initialized. It will tell the created bot which group it should join and the group 
	 * should be placed into a variable of the bots.
	 * 
	 * @param flag int, flag given by the local EC
	 * @return the String of the group that the bot should join
	 */
	public static String getAssignedGroup(int flag) {
		
		String stringFlag = Integer.toBinaryString(flag);
		return stringFlag.substring(7, 10);
		
	}
	
	
	/**
	 * This is meant to be used by the EC when looking at the bots that it is 
	 * controlling. If the EC can get the flag and the team code matches, then this 
	 * tells it what information is contained in the flag location.
	 * 
	 * @param flag int, flag gotten from controlled bot
	 * @return the subject string of the flag, see subject codes above for options
	 */
	public static String getSubjectToEC(int flag) {
		
		String stringFlag = Integer.toBinaryString(flag);
		return stringFlag.substring(7, 10);
		
	}
	
	/**
	 * Returns the location contained in the flag. Can be used in any flag becuase 
	 * all flags no matter from what to what have a location (with the exception of the 
	 * default flag.
	 * 
	 * @param flag
	 * @return
	 */
	public static MapLocation getLocation(int flag) {
		
		return getMapLocationFrom14BitString(Integer.toBinaryString(flag).substring(10, 24));
		
	}
	
	
	/*//////////////////////////////////////////////////////////////
	 * 
	 * 		The following are internal private methods to help create those above
	 * 
	 */////////////////////////////////////////////////////////////
	
	
	/**
	 * Takes a MapLocation and returns the location hashed into a 14 
	 * bit representation. The representation is obtained by modulus 128 of 
	 * the binary under the assumption that the location can be interpreted later on.
	 * WARNING: Locations outside of the map radius may not be represented properly!!!
	 * 
	 * @param location MapLocation
	 * @return a 14 bit representation of the location for a flag
	 */
	private static String make14BitStringFromMapLocation(MapLocation location) {
		
		int xLocation = location.x;
		int yLocation = location.y;
		int encodedIntLocation = 128 * (xLocation % 128) + (yLocation % 128);
		return verifyLengthOfBinaryString(Integer.toBinaryString(encodedIntLocation), 14);
		
	}
	
	/**
	 * Takes a communicated location from a flag and returns the most likely
	 * MapLocation that it was trying to convey.
	 * 
	 * @param stringBitLocation String, the location from the flag
	 * @return the MapLocation of the String that is most likely to be on the map
	 */
	private static MapLocation getMapLocationFrom14BitString(String stringBitLocation) {
		
		int x = Integer.parseInt(stringBitLocation.substring(0, 7), 2);
		int y = Integer.parseInt(stringBitLocation.substring(7, 14), 2);
		
		MapLocation robotLocation = RobotPlayer.rc.getLocation();
		int xShift = ((int) robotLocation.x / 128) * 128;
		int yShift = ((int) robotLocation.y / 128) * 128;
		
		MapLocation start = new MapLocation(xShift + x, yShift + y);
		MapLocation currentBest = start;
		
		MapLocation alt = start.translate(-128, 0);
		if (robotLocation.distanceSquaredTo(alt) < robotLocation.distanceSquaredTo(currentBest)) {
			currentBest = alt;
		} else alt = start.translate(128, 0);
		if (robotLocation.distanceSquaredTo(alt) < robotLocation.distanceSquaredTo(currentBest)) {
			currentBest = alt;
		} else 
			alt = start.translate(0, -128);
		if (robotLocation.distanceSquaredTo(alt) < robotLocation.distanceSquaredTo(currentBest)) {
			currentBest = alt;
		} else 
			alt = start.translate(0, 128);
		if (robotLocation.distanceSquaredTo(alt) < robotLocation.distanceSquaredTo(currentBest)) {
			currentBest = alt;
		}
		
		return currentBest;
		
	}
	
	/**
	 * Takes a binary string and a desired length and returns the 
	 * binary string with 0's added to the beginning to achieve 
	 * the desired length for the binary string.
	 * 
	 * @param binary String
	 * @param desiredLength int
	 * @return The binary string but with the desired length
	 */
	private static String verifyLengthOfBinaryString(String binary, int desiredLength) {
		
		for (int i = binary.length() ; i < desiredLength ; i++) {
			binary = "0" + binary;
		} return binary;
		
	}
	
}
