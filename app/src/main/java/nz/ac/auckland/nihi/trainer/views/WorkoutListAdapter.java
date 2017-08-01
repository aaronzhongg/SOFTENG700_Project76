package nz.ac.auckland.nihi.trainer.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.vision.text.Text;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.data.ExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.RCExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.SummaryDataChunk;

/**
 * Created by Aaron on 29/07/17.
 */

public class WorkoutListAdapter extends BaseAdapter {
    Context context;
    List<RCExerciseSummary> data;
    private static LayoutInflater inflater = null;

    public WorkoutListAdapter(Context context, List<RCExerciseSummary> data) {
        // TODO Auto-generated constructor stub
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View vi = view;
        if (vi == null) {
            vi = inflater.inflate(R.layout.workout_summary_row, null);
        }
        RCExerciseSummary s = (RCExerciseSummary) getItem(i);
        TextView workoutTimestamp = (TextView) vi.findViewById(R.id.workout_text_row);

//        Date date = new Date(s);
//        Format format = new SimpleDateFormat("dd MM yyyy HH:mm");
        workoutTimestamp.setText(s.getDate().toString());
        return vi;
    }
}
