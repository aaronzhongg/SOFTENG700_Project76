package nz.ac.auckland.nihi.trainer.data;

import java.sql.SQLException;

import nz.ac.auckland.cs.ormlite.AbstractDBSectionHelper;

import org.apache.log4j.Logger;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Helper class that knows how to create database structures required by the NIHI app. Also contains the ability to get
 * DAO objects to perform CRUD operations on NIHI-related tables.
 * 
 * @author Andrew Meads
 * 
 */
public class NihiDBHelper extends AbstractDBSectionHelper {

	private static final Logger logger = Logger.getLogger(NihiDBHelper.class);

	/**
	 * Cached DAO for {@link ExerciseSummary}.
	 */
	private Dao<ExerciseSummary, String> exerciseSummaryDAO;

	/**
	 * Cached DAO for {@link SymptomEntry}.
	 */
	// private Dao<SymptomEntry, Integer> symptomsDAO;

	/**
	 * Cached DAO for {@link ExerciseNotification}.
	 */
	// private Dao<ExerciseNotification, Integer> notificationDAO;

	private Dao<Route, String> routesDAO;

	private Dao<Goal, String> goalsDAO;

	// private Dao<RouteCoordinate, Integer> coordinatesDAO;

	/**
	 * Creates a new {@link NihiDBHelper}.
	 * 
	 * @param dbHelper
	 *            the helper class that can create DAO objects.
	 */
	public NihiDBHelper(OrmLiteSqliteOpenHelper dbHelper) {
		super(dbHelper);
	}

	/**
	 * Creates all NIHI-related database structures.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			logger.info("onCreate(): Creating NIHI Database tables.");
			TableUtils.createTable(connectionSource, Route.class);
			TableUtils.createTable(connectionSource, RouteCoordinate.class);
			TableUtils.createTable(connectionSource, ExerciseSummary.class);
			TableUtils.createTable(connectionSource, SymptomEntry.class);
			TableUtils.createTable(connectionSource, ExerciseNotification.class);
			TableUtils.createTable(connectionSource, Goal.class);
		} catch (SQLException e) {
			logger.error("onCreate(): Can't create NIHI Database tables.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Upgrades all NIHI-related structures to the latest version. Currently, this just deletes any old tables and adds
	 * new ones in their place.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {

		// We don't need to drop and re-create tables if, specifically, this case is true.
		if ((oldVersion >= 1 && oldVersion <= 2) && (newVersion >= 2 && newVersion <= 3)) {
			return;
		}

		// If the old version is 3 and the new version is 4, we need to make one small change: change the datatype of
		// the Notification field to "TEXT".
		else if (oldVersion == 3 && newVersion == 4) {
			logger.info("onUpgrade(): Changing notifications.notification field from VARCHAR to TEXT");
			return;
		}

		// By default, we'll "upgrade" from one version to another by dropping all tables and re-creating. This will,
		// unfortunately, delete all data.
		else {
			try {
				logger.info("onUpgrade(): dropping existing NIHI tables");
				TableUtils.dropTable(connectionSource, Route.class, true);
				TableUtils.dropTable(connectionSource, RouteCoordinate.class, true);
				TableUtils.dropTable(connectionSource, SymptomEntry.class, true);
				TableUtils.dropTable(connectionSource, ExerciseSummary.class, true);
				TableUtils.dropTable(connectionSource, ExerciseNotification.class, true);
				TableUtils.dropTable(connectionSource, Goal.class, true);
			} catch (SQLException e) {
				logger.error("onUpgrade(): could not drop NIHI tables", e);
				throw new RuntimeException(e);
			}
			onCreate(db, connectionSource);
		}
	}

	/**
	 * Gets a DAO that can be used to perform CRUD operations on {@link ExerciseSummary} data.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Dao<ExerciseSummary, String> getExerciseSummaryDAO() throws SQLException {
		if (exerciseSummaryDAO == null) {
			exerciseSummaryDAO = dbHelper.getDao(ExerciseSummary.class);
		}
		return exerciseSummaryDAO;
	}

	/**
	 * Gets a DAO that can be used to perform CRUD operations on {@link SymptomEntry} data.
	 * 
	 * @return
	 * @throws SQLException
	 */
	// public Dao<SymptomEntry, Integer> getSymptomsDAO() throws SQLException {
	// if (symptomsDAO == null) {
	// symptomsDAO = dbHelper.getDao(SymptomEntry.class);
	// }
	// return symptomsDAO;
	// }

	/**
	 * Gets a DAO that can be used to perform CRUD operations on {@link ExerciseNotification} data.
	 * 
	 * @return
	 * @throws SQLException
	 */
	// public Dao<ExerciseNotification, Integer> getNotificationDAO() throws SQLException {
	// if (notificationDAO == null) {
	// notificationDAO = dbHelper.getDao(ExerciseNotification.class);
	// }
	// return notificationDAO;
	// }

	public Dao<Route, String> getRoutesDAO() throws SQLException {
		if (routesDAO == null) {
			routesDAO = dbHelper.getDao(Route.class);
		}
		return routesDAO;
	}

	public Dao<Goal, String> getGoalsDAO() throws SQLException {
		if (goalsDAO == null) {
			goalsDAO = dbHelper.getDao(Goal.class);
		}
		return goalsDAO;
	}

	// public Dao<RouteCoordinate, Integer> getRouteCoordinatesDAO() throws SQLException {
	// if (coordinatesDAO == null) {
	// coordinatesDAO = dbHelper.getDao(RouteCoordinate.class);
	// }
	// return coordinatesDAO;
	// }

	/**
	 * When the DB is closed, release all DAO objects.
	 */
	@Override
	public void dbClosed() {
		// symptomsDAO = null;
		exerciseSummaryDAO = null;
		// notificationDAO = null;
		routesDAO = null;
		goalsDAO = null;
		// coordinatesDAO = null;
	}

}
