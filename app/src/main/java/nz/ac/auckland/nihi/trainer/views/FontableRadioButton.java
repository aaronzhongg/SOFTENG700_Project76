package nz.ac.auckland.nihi.trainer.views;

import nz.ac.auckland.nihi.trainer.R.style;
import nz.ac.auckland.nihi.trainer.R.styleable;
import nz.ac.auckland.nihi.trainer.util.UiUtils;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioButton;

public class FontableRadioButton extends RadioButton {

	public FontableRadioButton(Context context) {
		this(context, null);
	}

	public FontableRadioButton(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.radioButtonStyle);
	}

	public FontableRadioButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		UiUtils.setCustomFont(this, context, attrs, styleable.nz_ac_auckland_nihi_trainer_views_FontableRadioButton,
				defStyle, style.RadioButtonStyle, styleable.nz_ac_auckland_nihi_trainer_views_FontableRadioButton_font);
	}

}
