package nz.ac.auckland.nihi.trainer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.apache.log4j.Logger;

import java.sql.SQLException;

import nz.ac.auckland.cs.ormlite.DBSectionHelper;
import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;

/**
 * Created by alex on 6/13/2017.
 */

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final Logger logger = Logger.getLogger(DatabaseHelper.class);

    private Dao<nz.ac.auckland.nihi.trainer.data.Route, String> routesDAO;
    private Dao<SummaryDataChunk, String> summaryDataChunksDAO;
    private Dao<RCExerciseSummary, String> exerciseSummariesDAO;

    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "routes.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    /**
     * Creates all database structures.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            logger.info("onCreate(): Creating Database tables.");
            //For testing, drop tables first
            //TableUtils.dropTable(connectionSource, Route.class, true);
            //TableUtils.dropTable(connectionSource, RouteCoordinate.class, true);

            TableUtils.createTable(connectionSource, Route.class);
            TableUtils.createTable(connectionSource, RouteCoordinate.class);
            TableUtils.createTable(connectionSource, SummaryDataChunk.class);
            TableUtils.createTable(connectionSource, RCExerciseSummary.class);
        } catch (SQLException e) {
            logger.error("onCreate(): Can't create Database tables.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Upgrades all structures to the latest version. Currently, this just deletes any old tables and adds
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
                TableUtils.dropTable(connectionSource, SummaryDataChunk.class, true);
            } catch (SQLException e) {
                logger.error("onUpgrade(): could not drop NIHI tables", e);
                throw new RuntimeException(e);
            }
            onCreate(db, connectionSource);
        }
    }

    public void ResetDB(){
        try {
            logger.info("onCreate(): Creating Database tables.");
            //For testing, drop tables first
            TableUtils.dropTable(connectionSource, Route.class, true);
            TableUtils.dropTable(connectionSource, RouteCoordinate.class, true);
            TableUtils.dropTable(connectionSource, SummaryDataChunk.class, true);

            TableUtils.createTableIfNotExists(connectionSource, Route.class);
            TableUtils.createTableIfNotExists(connectionSource, RouteCoordinate.class);
            TableUtils.createTable(connectionSource, SummaryDataChunk.class);
        } catch (SQLException e) {
            logger.error("onCreate(): Can't create Database tables.", e);
            throw new RuntimeException(e);
        }
    }

    public Dao<Route, String> getRoutesDAO() throws SQLException {
        if (routesDAO == null) {
            routesDAO = getDao(Route.class);
        }
        return routesDAO;
    }

    public Dao<SummaryDataChunk, String> getSummaryDataChunksDAO() throws SQLException{
        if(summaryDataChunksDAO == null){
            summaryDataChunksDAO = getDao(SummaryDataChunk.class);
        }
        return summaryDataChunksDAO;
    }

    public Dao<RCExerciseSummary, String> getExerciseSummaryDAO() throws SQLException{
        if(exerciseSummariesDAO == null){
            exerciseSummariesDAO = getDao(RCExerciseSummary.class);
        }
        return exerciseSummariesDAO;
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
    public void close() {
        super.close();
        routesDAO = null;
    }


}
