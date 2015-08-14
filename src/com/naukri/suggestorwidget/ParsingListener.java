package com.naukri.suggestorwidget;

import org.json.JSONArray;


public interface ParsingListener<T> {

	// Parse the volley response.
	public JSONArray parseApiResponse(T response,String parsingKey);

}
