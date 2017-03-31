package nz.ac.auckland.nihi.trainer.views;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.R.id;
import nz.ac.auckland.nihi.trainer.R.string;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ExpandablePanel2 extends LinearLayout {

	private static final String PROPERTY_COLLAPSED = "nz.ac.auckland.nihi.trainer.views.ExpandablePanel.collapsed";
	private static final String SUPER_STATE = "nz.ac.auckland.nihi.trainer.views.ExpandablePanel.super";

	private View collapsedView;

	private View expandedView;

	private TextView btnCollapse;

	private boolean collapsed = true;

	public ExpandablePanel2(Context context) {
		this(context, null);
		this.setOrientation(VERTICAL);
	}

	public ExpandablePanel2(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOrientation(VERTICAL);

		boolean startCollapsed;

		// Read values from the provided attributes.
		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandablePanel, 0, 0);
			startCollapsed = a.getBoolean(R.styleable.ExpandablePanel_panelCollapsed, true);
			a.recycle();
		} else {
			startCollapsed = true;
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

			setViewVisibility();
			updateButtonText();
		}

	}

	/**
	 * Prevents {@link #assignControls()} being called multiple times on calles to the different permutations of
	 * <code>addView()</code>.
	 */
	private int addViewCounter = 0;

	@Override
	public void addView(View child) {
		addViewCounter++;
		super.addView(child);
		addViewCounter--;
		assignControls();
	}

	@Override
	public void addView(View child, int index) {
		addViewCounter++;
		super.addView(child, index);
		addViewCounter--;
		assignControls();
	}

	@Override
	public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
		addViewCounter++;
		super.addView(child, index, params);
		addViewCounter--;
		assignControls();
	}

	@Override
	public void addView(View child, int width, int height) {
		addViewCounter++;
		super.addView(child, width, height);
		addViewCounter--;
		assignControls();
	}

	@Override
	public void addView(View child, android.view.ViewGroup.LayoutParams params) {
		addViewCounter++;
		super.addView(child, params);
		addViewCounter--;
		assignControls();
	}

	/**
	 * Finds views with appropriate IDs within this view's children, and assigns them to {@link #collapsedView},
	 * {@link #expandedView}, and {@link #btnCollapse}. Then recomputes the visibility status of those controls.
	 */
	private void assignControls() {
		if (addViewCounter == 0) {
			collapsedView = findViewById(id.collapsedView);
			expandedView = findViewById(id.expandedView);

			if (btnCollapse != null) {
				btnCollapse.setOnClickListener(null);
			}
			btnCollapse = (TextView) findViewById(id.btnCollapse);
			if (btnCollapse != null) {
				btnCollapse.setOnClickListener(expandButtonClickListener);
			}

			setViewVisibility();
			updateButtonText();
		}
	}

	/**
	 * Sets the visibility of child controls to the appropriate value.
	 */
	private void setViewVisibility() {
		if (collapsedView != null) {
			collapsedView.setVisibility(collapsed ? VISIBLE : GONE);
		}
		if (expandedView != null) {
			expandedView.setVisibility(collapsed ? GONE : VISIBLE);
		}
	}

	/**
	 * Updates the button text to the appropriate value.
	 */
	private void updateButtonText() {
		if (btnCollapse != null) {
			String buttonText = collapsed ? getContext().getString(string.cpanel_button_more) : getContext().getString(
					string.cpanel_button_less);
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			ssb.append(buttonText);
			ssb.setSpan(new URLSpan("#"), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			btnCollapse.setText(ssb, TextView.BufferType.SPANNABLE);
		}
	}

	/**
	 * Toggle collapsed / expanded state when the button is clicked
	 */
	private final View.OnClickListener expandButtonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			setCollapsed(!collapsed);
		}
	};
}
