package nz.ac.auckland.nihi.trainer.activities;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.RCExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.SummaryDataChunk;

public class WorkoutSummaryActivity extends FragmentActivity {

    private DatabaseHelper dbHelper;
    private RCExerciseSummary s;
    Dao<RCExerciseSummary, String> exerciseSummariesDAO;


    /**
     * Lazily creates the {@link #dbHelper} if required, then returns it.
     *
     * @return
     */
    //Make a database helper
    private DatabaseHelper getHelper() {
        if (dbHelper == null) {
            dbHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
        }
        return dbHelper;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_summary);
        try {
            exerciseSummariesDAO = getHelper().getExerciseSummaryDAO();
//            QueryBuilder<SummaryDataChunk, String> queryBuilder = summaryDao.queryBuilder();
//            queryBuilder.where().eq(SummaryDataChunk.id = );
            String workoutId = getIntent().getExtras().getString("workout_id");
            s = exerciseSummariesDAO.queryForId(workoutId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
