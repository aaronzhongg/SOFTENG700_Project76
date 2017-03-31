package nz.ac.auckland.nihi.trainer.util;

import java.util.HashMap;

public class OdinIDUtils {

	private static final HashMap<String, Long> HARDCODED_IDS = new HashMap<String, Long>();

	static {
		HARDCODED_IDS.put("andrew", 12345L);
		HARDCODED_IDS.put("jonathan", 100L);
		HARDCODED_IDS.put("ian", 101L);
	}

	public static long generateID(String userName) {
		if (HARDCODED_IDS.containsKey(userName)) {
			return HARDCODED_IDS.get(userName);
		} else {
			return userName.hashCode();
		}
	}
}
