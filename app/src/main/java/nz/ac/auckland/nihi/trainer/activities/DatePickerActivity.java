package nz.ac.auckland.nihi.trainer.activities;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.util.AndroidTextUtils;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * An {@link Activity} allowing the user to select from a list of dates for which there is recorded exercise data.
 * 
 * @author Andrew Meads
 */
public class DatePickerActivity extends Activity {

	public static final String EXTRA_VALID_DATES = DatePickerActivity.class.getName() + ".Request.ValidDates";

	/**
	 * The Action name of the Intent that's returned upon a successful selection of a date by the user.
	 */
	public static final String RESULT_DATE = DatePickerActivity.class.getName() + ".SuccessfulResult";

	/**
	 * The name of the extra that contains the <code>long</code> representation of the date chosen by the user.
	 */
	public static final String EXTRA_DATE = RESULT_DATE + ".Date";

	/**
	 * The name of the extra that contains the selected index into the array passed into this activity.
	 */
	public static final String EXTRA_INDEX = RESULT_DATE + ".Index";

	// UI Controls
	private Spinner spinnerMonth;
	private Spinner spinnerYear;
	private DaysAdapter daysAdapter;
	private ListView lstDays;

	/**
	 * The list of valid dates. Will be created and populated in the {@link #onCreate(Bundle)} method.
	 */
	private List<GregorianCalendar> validDates;

	/**
	 * Creates and populates the list of valid dates.
	 * 
	 * @param savedInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		setContentView(layout.date_picker);

		// Get references to controls
		spinnerMonth = (Spinner) findViewById(id.spinnerMonth);
		spinnerYear = (Spinner) findViewById(id.spinnerYear);
		lstDays = (ListView) findViewById(id.lstDays);

		// Get the list of valid dates from the intent.
		long[] arrValidDates = getIntent().getLongArrayExtra(EXTRA_VALID_DATES);

		// Create a list of GregorianCalendar instances corresponding to those dates.
		validDates = new ArrayList<GregorianCalendar>();
		for (int i = 0; i < arrValidDates.length; i++) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis(arrValidDates[i]);
			validDates.add(cal);
		}

		// From those valid dates, get all distinct years.
		Set<Integer> years = new HashSet<Integer>();
		for (GregorianCalendar cal : validDates) {
			years.add(cal.get(GregorianCalendar.YEAR));
		}

		// Setup the days adapter to pull its data from the list of valid dates
		daysAdapter = new DaysAdapter();
		lstDays.setAdapter(daysAdapter);

		// Populate the "year" spinner with those values and set the selected year
		spinnerYear.setAdapter(new ArrayAdapter<Integer>(this, layout.date_picker_year_month_selector,
				new ArrayList<Integer>(years)));
		// spinnerYear.setSelection(0);

		// Initially populate the "months" spinner with all months available in the currently selected year.
		populateMonthsSpinner();

		// Add listeners to spinners
		spinnerYear.setOnItemSelectedListener(monthOrYearChangeListener);
		spinnerMonth.setOnItemSelectedListener(monthOrYearChangeListener);

		// TODO Apply initial filter.

		// Add listener to the DAYS.
		lstDays.setOnItemClickListener(dayClickedListener);
		// lstDays.setOnItemSelectedListener(daySelectedListener);

		// Exit button
		View btnExit = (View) findViewById(id.btnClose);
		btnExit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}

	private void populateMonthsSpinner() {

		ArrayList<String> monthNames = new ArrayList<String>();

		// Get the currently selected year
		int currentYear = (Integer) spinnerYear.getSelectedItem();

		// For each date for which there is exercise data...
		for (GregorianCalendar date : validDates) {

			// If that date is in the correct year...
			if (date.get(Calendar.YEAR) == currentYear) {

				// ... Get the name of the month..
				String monthName = date.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG,
						Locale.getDefault());

				// ... and add it to the list if its not already there.
				if (!monthNames.contains(monthName)) {
					monthNames.add(monthName);
				}
			}

		}

		// Populate the month spinner with this data
		spinnerMonth.setAdapter(new ArrayAdapter<String>(this, layout.date_picker_year_month_selector, monthNames));
		// spinnerMonth.setSelection(0);
	}

	/**
	 * A listener that responds to a date being clicked by the user. Creates the result intent and finishes the
	 * activity, notifying the caller of a successful date choosing.
	 */
	private final AdapterView.OnItemClickListener dayClickedListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
			GregorianCalendar cal = (GregorianCalendar) lstDays.getItemAtPosition(position);

			Intent result = new Intent(RESULT_DATE);
			result.putExtra(EXTRA_DATE, cal.getTime().getTime());
			result.putExtra(EXTRA_INDEX, validDates.indexOf(cal));
			setResult(RESULT_OK, result);
			finish();
		}
	};

	/**
	 * A listener that responds to the selected month or year being changed. Updates the list of dates shown on-screen
	 * to those within the selected month / year.
	 */
	private final AdapterView.OnItemSelectedListener monthOrYearChangeListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> spinner, View itemView, int position, long id) {

			// If the view that changed is the year view, update the month view.
			if (spinner == spinnerYear) {
				populateMonthsSpinner();
			}

			// Otherwise populate the day view.
			else {

				// Get the year & month
				int year = (Integer) spinnerYear.getSelectedItem();
				String month = (String) spinnerMonth.getSelectedItem();

				// Apply filter
				String sFilter = year + " " + month;
				daysAdapter.getFilter().filter(sFilter);

			}

		}

		@Override
		public void onNothingSelected(AdapterView<?> view) {
			// TODO Auto-generated method stub
		}
	};

	/**
	 * An adapter that displays the list of valid dates to the user. This adapter is capable of being filtered by month
	 * / year.
	 * 
	 * @author Andrew Meads
	 * 
	 */
	private class DaysAdapter extends BaseAdapter implements Filterable {

		private List<GregorianCalendar> filteredList;

		private DaysAdapter() {
			filteredList = new ArrayList<GregorianCalendar>(validDates);
		}

		@Override
		public int getCount() {
			return filteredList.size();
		}

		@Override
		public Object getItem(int position) {
			return filteredList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return filteredList.get(position).getTime().getTime();
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public int getItemViewType(int position) {
			return layout.date_picker_single_item;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View itemView = null;

			if (convertView == null) {
				LayoutInflater li = LayoutInflater.from(parent.getContext());
				itemView = li.inflate(layout.date_picker_single_item, parent, false);
			} else {
				itemView = convertView;
			}

			TextView txtWeekday = (TextView) itemView.findViewById(id.txtWeekDay);
			TextView txtDayOfMonth = (TextView) itemView.findViewById(id.txtDayOfMonth);
			TextView txtPostfix = (TextView) itemView.findViewById(id.txtPostfix);

			GregorianCalendar cal = filteredList.get(position);
			String weekday = cal.getDisplayName(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.LONG,
					Locale.getDefault());
			txtWeekday.setText(weekday);
			int dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
			txtDayOfMonth.setText("" + dayOfMonth);

			txtPostfix.setText(AndroidTextUtils.ordinalOnly(DatePickerActivity.this, dayOfMonth));

			return itemView;
		}

		@Override
		public Filter getFilter() {
			return myFilter;
		}

		/**
		 * A filter that filters the list of dates shown to the user based on month and year.
		 */
		private Filter myFilter = new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				List<GregorianCalendar> newList = new ArrayList<GregorianCalendar>();
				String strConstraint = constraint.toString();
				String[] parts = strConstraint.split(" ");
				int yearFilter = Integer.parseInt(parts[0]);
				String monthFilter = parts[1];
				for (GregorianCalendar cal : validDates) {
					int year = cal.get(GregorianCalendar.YEAR);
					String month = cal.getDisplayName(GregorianCalendar.MONTH, GregorianCalendar.LONG,
							Locale.getDefault());
					if (year == yearFilter && monthFilter.equalsIgnoreCase(month)) {
						newList.add(cal);
					}
				}

				FilterResults res = new FilterResults();
				res.count = newList.size();
				res.values = newList;
				return res;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {

				filteredList = (List<GregorianCalendar>) results.values;
				notifyDataSetChanged();

			}

		};

	}

}
