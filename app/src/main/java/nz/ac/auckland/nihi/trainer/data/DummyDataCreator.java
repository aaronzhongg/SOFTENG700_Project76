package nz.ac.auckland.nihi.trainer.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;

import nz.ac.auckland.nihi.trainer.R.drawable;
import nz.ac.auckland.nihi.trainer.util.LocationUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

import com.j256.ormlite.dao.Dao;

public class DummyDataCreator {

	public static final int NUM_DUMMY_DATA = 52;
	public static final int MAX_NUM_SYMPTOMS_PER_SUMMARY = 4;
	public static final int MAX_NUM_NOTIFICATIONS_PER_SUMMARY = 3;
	public static final int YEAR = 2012;

	private static final String[] DUMMY_NOTIFICATIONS = new String[] {

			/**/
			"Resting for a short time will ease your angina. Take a break for two minutes and then get back into it. You're doing really well.",

			/**/
			"You're doing really well. Take another two minute break to ease your angina before continuing."

	};

	private static final double[] DUMMY_COORDINATES = new double[] { -36.853320902918384, 174.75763320922852,
			-36.84555973141201, 174.7609806060791, -36.84411730298869, 174.7635555267334, -36.84470114634267,
			174.76655960083008, -36.85091708446578, 174.76441383361816, -36.84985251214405, 174.7608518600464,
			-36.854076373106125, 174.7584056854248, -36.8537673180227, 174.75733280181885, -36.853320902918384,
			174.75759029388428 };

	// private static DummyDataCreator instance;
	//
	// public static DummyDataCreator getInstance() {
	// if (instance == null) {
	// instance = new DummyDataCreator();
	// }
	// return instance;
	// }

	// public static void killInstance() {
	// instance = null;
	// }

	// private final Map<GregorianCalendar, ExerciseSummary> exerciseData;
	// private final List<GregorianCalendar> validDates;

	private static final String TEST_IMAGE_FILE_NAME = "testmapimage.png";

	public static void makeJonathansDummyData(Context context, Dao<ExerciseSummary, String> exerciseSummaryDAO,
			Dao<Route, Integer> routeDAO) throws SQLException, IOException {

		Random r = new Random();

		// Copy the test image to the correct place in the file system.
		String testImagePath = saveTestMapImage(context);

		addDummyRecord(
				2013,
				9,
				27,
				3812,
				4770,
				7.708,
				120,
				113,
				126,
				4.5f,
				3.67f,
				5.33f,
				Symptom.Angina,
				SymptomStrength.Light,
				1306,
				"You are doing really well! Taking a short break will help to ease your angina. Try to keep your heart rate under 120 bpm",
				1315, context, exerciseSummaryDAO, routeDAO, testImagePath, r);

		addDummyRecord(2013, 9, 25, 2948, 4260, 7.708, 123, 117, 129, 5.2f, 4.37f, 6.03f, null, null, 0, null, 0,
				context, exerciseSummaryDAO, routeDAO, testImagePath, r);

		addDummyRecord(
				2013,
				9,
				23,
				2441,
				3250,
				7.708,
				120,
				114,
				127,
				4.8f,
				3.97f,
				5.63f,
				Symptom.Angina,
				SymptomStrength.Moderate,
				232,
				"Resting for a couple of minutes will ease your angina. Get back into it when the pain disappears, and walk a little slower until you are warmed up",
				247, context, exerciseSummaryDAO, routeDAO, testImagePath, r);

		addDummyRecord(2013, 9, 21, 3072, 5460, 7.708, 138, 131, 144, 6.4f, 5.57f, 7.23f, null, null, 0, null, 0,
				context, exerciseSummaryDAO, routeDAO, testImagePath, r);

		addDummyRecord(2013, 9, 19, 2637, 3880, 7.708, 124, 118, 130, 5.3f, 4.47f, 6.13f, null, null, 0, null, 0,
				context, exerciseSummaryDAO, routeDAO, testImagePath, r);
	}

	private static void addDummyRecord(int year, int month, int day, int duration, int distance,
			double cumulativeImpulse, int avgHR, int minHR, int maxHR, float avgSpeed, float minSpeed, float maxSpeed,
			Symptom symptom, SymptomStrength symptomStrength, int symptomRelTime, String notification,
			int notificationRelTime, Context context, Dao<ExerciseSummary, String> exerciseSummaryDAO,
			Dao<Route, Integer> routeDAO, String testRouteImagePath, Random r) throws SQLException {

		ExerciseSummary summary = new ExerciseSummary();

		// Some percentage chance of this summary having a route.
		if (r.nextBoolean()) {
			Route followedRoute = createDummyRoute(routeDAO, testRouteImagePath);
			summary.setFollowedRoute(followedRoute);
		}

		GregorianCalendar cal = new GregorianCalendar();
		cal.set(year, month, day);
		summary.setDate(cal.getTime());
		summary.setDurationInSeconds(duration);
		summary.setDistanceInMetres(distance);
		summary.setCumulativeImpulse(cumulativeImpulse);
		summary.setAvgHeartRate(avgHR);
		summary.setMinHeartRate(minHR);
		summary.setMaxHeartRate(maxHR);
		summary.setAvgSpeed(avgSpeed);
		summary.setMinSpeed(minSpeed);
		summary.setMaxSpeed(maxSpeed);
		exerciseSummaryDAO.assignEmptyForeignCollection(summary, "symptoms");
		exerciseSummaryDAO.assignEmptyForeignCollection(summary, "notifications");
		exerciseSummaryDAO.create(summary);

		if (symptom != null) {
			SymptomEntry symptomEntry = new SymptomEntry(symptom, symptomStrength, symptomRelTime);
			summary.getSymptoms().add(symptomEntry);
		}

		if (notification != null) {
			ExerciseNotification not = new ExerciseNotification(notification, notificationRelTime);
			summary.getNotifications().add(not);
		}
	}

	public static void makeDummyData(Context context, Dao<ExerciseSummary, String> exerciseSummaryDAO,
			Dao<Route, Integer> routeDAO) throws SQLException, IOException {
		Random r = new Random();

		// Copy the test image to the correct place in the file system.
		String testImagePath = saveTestMapImage(context);

		// Create a bunch of dates on which the dummy data will be held.
		TreeSet<Date> dates = new TreeSet<Date>();
		for (int i = 0; i < NUM_DUMMY_DATA; i++) {

			// Create the date when this dummy data was generated.
			// TODO Maybe change this - there's a bug where this will infinite loop if there's dummy data for all days
			// in a month.
			int month = r.nextInt(12);// + 1;
			GregorianCalendar date = new GregorianCalendar(YEAR, month, 1);
			int day = r.nextInt(date.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)) + 1;
			date.set(YEAR, month, day);
			while (dates.contains(date.getTime())) {
				day = r.nextInt(date.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)) + 1;
				date.set(YEAR, month, day);
			}
			dates.add(date.getTime());

		}

		// Iterate through the set in sorted order to add dummy summaries to the database.
		for (Date date : dates) {
			genFakeSummary(r, date, exerciseSummaryDAO, testImagePath, routeDAO);
		}
	}

	private static String saveTestMapImage(Context context) throws FileNotFoundException, IOException {
		File externalFilesDir = context.getExternalFilesDir(null);
		File savedImagesDir = new File(externalFilesDir, LocationUtils.SAVED_MAP_IMAGE_DIRECTORY);
		if (!savedImagesDir.exists()) {
			savedImagesDir.mkdirs();
		}
		Bitmap image = BitmapFactory.decodeResource(context.getResources(), drawable.testmapthumbnail);
		File imageFile = new File(savedImagesDir, TEST_IMAGE_FILE_NAME);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(imageFile));
		boolean saved = image.compress(CompressFormat.PNG, 100, out);
		out.flush();
		out.close();
		if (!saved) {
			throw new IOException("Could not save map image (image.compress returned false).");
		}
		return imageFile.getPath();
	}

	// public ExerciseSummary generateSummaryForMonth(int year, int month) {
	// List<ExerciseSummary> data = new ArrayList<ExerciseSummary>();
	// for (GregorianCalendar date : validDates) {
	// if (date.get(GregorianCalendar.YEAR) == year && date.get(GregorianCalendar.MONTH) == month) {
	// data.add(exerciseData.get(date));
	// }
	// }
	// return new ExerciseSummary(data);
	// }

	private static ExerciseSummary genFakeSummary(Random r, Date date, Dao<ExerciseSummary, String> dao,
			String testRouteImagePath, Dao<Route, Integer> routeDAO) throws SQLException {

		ExerciseSummary summary = new ExerciseSummary();

		// Some percentage chance of this summary having a route.
		if (r.nextBoolean()) {
			Route followedRoute = createDummyRoute(routeDAO, testRouteImagePath);
			summary.setFollowedRoute(followedRoute);
		}

		summary.setDurationInSeconds(r.nextInt(600) + 600); // between 10 - 20 mins
		summary.setDistanceInMetres(r.nextInt(1000) + 2000); // between 2 - 3 km
		summary.setCumulativeImpulse(r.nextDouble() * 6.0 + 9.0); // 9 - 15
		summary.setMinHeartRate(r.nextInt(20) + 60); // between 60 - 80
		summary.setAvgHeartRate(r.nextInt(20) + 120); // between 120 - 140
		summary.setMaxHeartRate(r.nextInt(15) + 145); // between 145 - 160
		summary.setMinSpeed((r.nextFloat() * 1.0f) + 4.0f); // between 4 - 5 kph
		summary.setAvgSpeed((r.nextFloat() * 1.0f) + 6.0f); // between 6 - 7 kph
		summary.setMaxSpeed((r.nextFloat() * 2.0f) + 8.0f); // between 8 - 10 kph
		summary.setDate(date);
		dao.assignEmptyForeignCollection(summary, "symptoms");
		dao.assignEmptyForeignCollection(summary, "notifications");
		dao.create(summary);

		// Symptoms
		List<SymptomEntry> symptoms = genFakeSymptoms(r);
		Collections.sort(symptoms);
		for (SymptomEntry s : symptoms) {
			s.setSummary(summary);
			// Adding to this collection will automatically cause the symptom to be added to the database.
			summary.getSymptoms().add(s);
		}

		// Notifications
		List<ExerciseNotification> notifications = genFakeNotifications(r);
		Collections.sort(notifications);

		for (ExerciseNotification n : notifications) {
			n.setSummary(summary);
			// Adding to this collection will automatically cause the notification to be added to the database.
			summary.getNotifications().add(n);
		}

		return summary;

	}

	private static Route createDummyRoute(Dao<Route, Integer> routeDAO, String testRouteImagePath) throws SQLException {
		Route route = new Route();
		route.setName("route_" + UUID.randomUUID().toString());
		route.setThumbnailFileName(testRouteImagePath);
		route.setFavorite(false);
		routeDAO.assignEmptyForeignCollection(route, "gpsCoordinates");
		routeDAO.create(route);
		for (int i = 0; i < DUMMY_COORDINATES.length; i += 2) {
			double lat = DUMMY_COORDINATES[i];
			double lng = DUMMY_COORDINATES[i + 1];
			RouteCoordinate coord = new RouteCoordinate();
			coord.setLatitude(lat);
			coord.setLongitude(lng);
			coord.setRoute(route);
			route.getGpsCoordinates().add(coord);
		}
		return route;
	}

	private static List<SymptomEntry> genFakeSymptoms(Random r) {
		List<SymptomEntry> entries = new ArrayList<SymptomEntry>();
		int numSymptoms = r.nextInt(MAX_NUM_SYMPTOMS_PER_SUMMARY + 1);
		for (int i = 0; i < numSymptoms; i++) {

			SymptomEntry entry = new SymptomEntry();
			entry.setSymptom(Symptom.values()[r.nextInt(Symptom.values().length)]);
			entry.setStrength(SymptomStrength.values()[r.nextInt(SymptomStrength.values().length - 1)]);
			entry.setRelativeTimeInSeconds(r.nextInt(3600 * 3));

			entries.add(entry);

		}

		return entries;
	}

	private static List<ExerciseNotification> genFakeNotifications(Random r) {
		List<ExerciseNotification> entries = new ArrayList<ExerciseNotification>();
		int numNotifications = r.nextInt(MAX_NUM_NOTIFICATIONS_PER_SUMMARY + 1);
		for (int i = 0; i < numNotifications; i++) {
			ExerciseNotification notification = new ExerciseNotification();
			notification.setNotification(DUMMY_NOTIFICATIONS[r.nextInt(DUMMY_NOTIFICATIONS.length)]);
			notification.setRelativeTimeInSeconds(r.nextInt(3600 * 3));
			entries.add(notification);
		}
		return entries;
	}

}
