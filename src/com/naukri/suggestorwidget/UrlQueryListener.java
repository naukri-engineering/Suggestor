package com.naukri.suggestorwidget;

public interface UrlQueryListener<T> {
	
	public String getUrlQuery(T searchParam,String urlKey);
	
}
