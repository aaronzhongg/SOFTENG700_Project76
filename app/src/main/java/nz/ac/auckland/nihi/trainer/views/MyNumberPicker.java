package nz.ac.auckland.nihi.trainer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.NumberPicker;

public class MyNumberPicker extends NumberPicker {

	public MyNumberPicker(Context context) {
		super(context);
	}

	public MyNumberPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		processAttrs(attrs);
	}

	public MyNumberPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		processAttrs(attrs);
	}

	private void processAttrs(AttributeSet attrs) {
		this.setMinValue(attrs.getAttributeIntValue(null, "min", 0));
		this.setMaxValue(attrs.getAttributeIntValue(null, "max", 100));
		this.setValue(attrs.getAttributeIntValue(null, "value", 0));
	}

}
