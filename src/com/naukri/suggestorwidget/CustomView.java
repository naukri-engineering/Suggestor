package com.naukri.suggestorwidget;

import android.graphics.drawable.Drawable;

public interface CustomView {
	
	public static final int ERROR_VIEW = 1;
	public static final int NORMAL_VIEW = 0;
	public static final int DISABLED_VIEW = 3;

	/** Lower end phone supported */
	public void setBackground(Drawable drawable);

	public void setLevel(int levelValue);

	public void setErrorView();

	public void setNormalView();

}
