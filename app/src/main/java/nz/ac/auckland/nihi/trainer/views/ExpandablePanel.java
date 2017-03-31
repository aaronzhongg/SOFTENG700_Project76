package nz.ac.auckland.nihi.trainer.views;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.layout;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExpandablePanel extends LinearLayout {

	private static final String PROPERTY_COLLAPSED = "nz.ac.auckland.nihi.trainer.views.ExpandablePanel.collapsed";
	private static final String SUPER_STATE = "nz.ac.auckland.nihi.trainer.views.ExpandablePanel.super";

	private LinearLayout collapsedViewHolder;
	private final int collapsedViewId;
	private View collapsedView;

	private LinearLayout expandedViewHolder;
	private final int expandedViewId;
	private View expandedView;

	// private ImageView btnCollapse;
	private TextView btnCollapse;
	// private final RelativeLayout.LayoutParams btnCollapse_collapsedLParams;
	// private final RelativeLayout.LayoutParams btnCollapse_expandedLParams;

	private boolean collapsed = true;

	public ExpandablePanel(Context context) {
		this(context, null);
	}

	public ExpandablePanel(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.setOrientation(VERTICAL);

		boolean startCollapsed;

		// Read values from the provided attributes.
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandablePanel, 0, 0);
			collapsedViewId = a.getResourceId(R.styleable.ExpandablePanel_collapsedView, 0);
			expandedViewId = a.getResourceId(R.styleable.ExpandablePanel_expandedView, 0);
			startCollapsed = a.getBoolean(R.styleable.ExpandablePanel_panelCollapsed, true);
			a.recycle();
		} else {
			startCollapsed = true;
			collapsedViewId = 0;
			expandedViewId = 0;
		}

		// Inflate the collapsible panel layout into this view
		LayoutInflater li = LayoutInflater.from(context);
		View innerView = li.inflate(layout.collapsible_panel_layout, null);
		addView(innerView);

		btnCollapse = (TextView) innerView.findViewById(id.btnExpand);
		// btnCollapse = (ImageView) innerView.findViewById(id.btnExpand);
		btnCollapse.setOnClickListener(expandButtonClickListener);
		btnCollapse.setEnabled(collapsedViewId != 0);

		// Get the holders that will hold the collapsed and expanded views
		collapsedViewHolder = (LinearLayout) innerView.findViewById(id.stubCollapsedView);
		expandedViewHolder = (LinearLayout) innerView.findViewById(id.stubExpandedView);

		// Expand the views into the holders
		if (collapsedViewId != 0) {
			li.inflate(collapsedViewId, collapsedViewHolder);
			collapsedView = collapsedViewHolder.getChildAt(0);
		}

		if (expandedViewId != 0) {
			li.inflate(expandedViewId, expandedViewHolder);
			expandedView = expandedViewHolder.getChildAt(0);
		}

		setCollapsed(startCollapsed);

	}

	public View getCollapsedContentView() {
		return collapsedView;
	}

	public View getExpandedContentView() {
		return expandedView;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable p = super.onSaveInstanceState();
		Bundle b = new Bundle();
		b.putBoolean(PROPERTY_COLLAPSED, this.collapsed);
		if (p != null) {
			b.putParcelable(SUPER_STATE, p);
		}
		return b;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle b = (Bundle) state;
		if (b.containsKey(SUPER_STATE)) {
			super.onRestoreInstanceState(b.getParcelable(SUPER_STATE));
		}
		setCollapsed(b.getBoolean(PROPERTY_COLLAPSED));
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean collapsed) {
		if (this.collapsed != collapsed) {
			this.collapsed = collapsed;

			// String buttonText = null;

			if (collapsed) {
				// btnCollapse.setText("More");
				// btnCollapse.setContentDescription(getContext().getString(string.button_expand_more));
				// btnCollapse.setImageResource(drawable.expander_open);
				// buttonText = getContext().getString(string.button_expand_more);

				expandedViewHolder.setVisibility(GONE);
				collapsedViewHolder.setVisibility(VISIBLE);

			} else {

				// btnCollapse.setText("Less");
				// btnCollapse.setContentDescription(getContext().getString(string.button_expand_less));
				// btnCollapse.setImageResource(drawable.expander_close);
				// btnCollapse.setText("Less");
				// buttonText = getContext().getString(string.button_expand_less);
				expandedViewHolder.setVisibility(VISIBLE);
				collapsedViewHolder.setVisibility(GONE);
			}

		}
		String buttonText = collapsed ? getContext().getString(string.cpanel_button_more) : getContext().getString(
				string.cpanel_button_less);
		SpannableStringBuilder ssb = new SpannableStringBuilder();
		ssb.append(buttonText);
		ssb.setSpan(new URLSpan("#"), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		btnCollapse.setText(ssb, TextView.BufferType.SPANNABLE);
	}

	// Toggle collapsed / expanded state when the button is clicked
	private final View.OnClickListener expandButtonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			setCollapsed(!collapsed);
		}
	};
}
