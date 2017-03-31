package nz.ac.auckland.nihi.trainer.views;

import nz.ac.auckland.nihi.trainer.R;
import nz.ac.auckland.nihi.trainer.R.style;
import nz.ac.auckland.nihi.trainer.util.UiUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class FontableTextView extends TextView {

	public FontableTextView(Context context) {
		// super(context);
		this(context, null);
	}

	public FontableTextView(Context context, AttributeSet attrs) {
		// super(context, attrs);
		// UiUtil.setCustomFont(this, context, attrs, R.styleable.nz_ac_auckland_nihi_trainer_views_FontableTextView,
		// R.styleable.nz_ac_auckland_nihi_trainer_views_FontableTextView_font);
		this(context, attrs, android.R.attr.textViewStyle);
	}

	public FontableTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		UiUtils.setCustomFont(this, context, attrs, R.styleable.nz_ac_auckland_nihi_trainer_views_FontableTextView,
				defStyle, style.PrimaryTextViewStyle,
				R.styleable.nz_ac_auckland_nihi_trainer_views_FontableTextView_font);
	}
}