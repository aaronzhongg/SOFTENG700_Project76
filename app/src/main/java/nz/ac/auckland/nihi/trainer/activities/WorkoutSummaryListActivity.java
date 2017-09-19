package nz.ac.auckland.nihi.trainer.activities;

import android.content.Intent;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nz.ac.auckland.cs.ormlite.DatabaseManager;
import nz.ac.auckland.cs.ormlite.LocalDatabaseHelper;
import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.DatabaseHelper;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.RCExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.SummaryDataChunk;
import nz.ac.auckland.nihi.trainer.data.session.ExerciseSessionData;
import nz.ac.auckland.nihi.trainer.views.WorkoutListAdapter;

public class WorkoutSummaryListActivity extends FragmentActivity {

    /**
     * The helper allowing us to access the database.
     */
    private DatabaseHelper dbHelper;
    private ListView listView;
    WorkoutListAdapter adapter;
    private List<RCExerciseSummary> exerciseSummaries = new ArrayList<RCExerciseSummary>();

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
        setContentView(R.layout.activity_workout_summary_list);
        loadWorkoutList();
        listView = (ListView) findViewById(R.id.summary_list);
        adapter = new WorkoutListAdapter(this, exerciseSummaries);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                RCExerciseSummary s = exerciseSummaries.get(position);
                Intent intent = new Intent(WorkoutSummaryListActivity.this, WorkoutSummaryActivity.class);
                intent.putExtra("workout_id", s.getId());
                startActivity(intent);
                }
            });

    }

    protected void loadWorkoutList() {
        try {
            Dao<RCExerciseSummary, String> exerciseSummaryDao = getHelper().getExerciseSummaryDAO();
            exerciseSummaries = exerciseSummaryDao.queryForAll();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        startActivity(new Intent(WorkoutSummaryListActivity.this, HomeScreenActivity.class));
        finish();

    }
}
