package com.odin.android.bioharness.data;

import java.sql.SQLException;

import nz.ac.auckland.cs.ormlite.AbstractDBSectionHelper;

import org.apache.log4j.Logger;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Helper class that knows how to create database structures required by the Bioharness. Also contains the ability to
 * get DAO objects to perform CRUD operations on Bioharness-related tables.
 * 
 * @author Andrew Meads
 * 
 */
public class BioharnessDBHelper extends AbstractDBSectionHelper {

	private static final Logger logger = Logger.getLogger(BioharnessDBHelper.class);

	/**
	 * Cached DAO for {@link GeneralData}.
	 */
	private Dao<GeneralData, Integer> generalDataDao;

	/**
	 * Cached DAO for {@link ECGWaveformData}.
	 */
	private Dao<ECGWaveformData, Integer> ecgDataDao;

	/**
	 * Cached DAO for {@link SummaryData}.
	 */
	private Dao<SummaryData, Integer> summaryDataDao;

	/**
	 * Creates a new {@link BioharnessDBHelper}.
	 * 
	 * @param dbHelper
	 *            the helper class that can create DAO objects.
	 */
	public BioharnessDBHelper(OrmLiteSqliteOpenHelper dbHelper) {
		super(dbHelper);
	}

	/**
	 * Creates all Bioharness-related database structures.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			logger.info("onCreate(): Creating Bioharness Database tables.");
			TableUtils.createTable(connectionSource, GeneralData.class);
			TableUtils.createTable(connectionSource, ECGWaveformData.class);
			TableUtils.createTable(connectionSource, SummaryData.class);
		} catch (SQLException e) {
			logger.error("onCreate(): Can't create Bioharness Database tables.", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Upgrades all Bioharness-related structures to the latest version. Currently, this just deletes any old tables and
	 * adds new ones in their place.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {

		// We don't need to drop and re-create tables if, specifically, this case is true.
		if ((oldVersion >= 1 && oldVersion <= 2) && (newVersion >= 2 && newVersion <= 3)) {
			return;
		}

		else if (oldVersion == 3 && newVersion == 4) {
			return;
		}

		else {

			try {
				logger.info("onUpgrade(): dropping existing Bioharness tables");
				TableUtils.dropTable(connectionSource, GeneralData.class, true);
				TableUtils.dropTable(connectionSource, ECGWaveformData.class, true);
				TableUtils.dropTable(connectionSource, SummaryData.class, true);
			} catch (SQLException e) {
				logger.error("onUpgrade(): could not drop Bioharness tables", e);
				throw new RuntimeException(e);
			}
			onCreate(db, connectionSource);
		}
	}

	/**
	 * Gets a DAO that can be used to perform CRUD operations on {@link GeneralData}.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Dao<GeneralData, Integer> getGeneralDataDAO() throws SQLException {
		if (generalDataDao == null) {
			generalDataDao = dbHelper.getDao(GeneralData.class);
		}
		return generalDataDao;
	}

	/**
	 * Gets a DAO that can be used to perform CRUD operations on {@link SummaryData}.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Dao<SummaryData, Integer> getSummaryDataDAO() throws SQLException {
		if (summaryDataDao == null) {
			summaryDataDao = dbHelper.getDao(SummaryData.class);
		}
		return summaryDataDao;
	}

	/**
	 * Gets a DAO that can be used to perform CRUD operations on {@link ECGWaveformData}.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Dao<ECGWaveformData, Integer> getECGDataDAO() throws SQLException {
		if (ecgDataDao == null) {
			ecgDataDao = dbHelper.getDao(ECGWaveformData.class);
		}
		return ecgDataDao;
	}

	/**
	 * When the DB is closed, release all DAO objects.
	 */
	@Override
	public void dbClosed() {
		generalDataDao = null;
		ecgDataDao = null;
	}

}
