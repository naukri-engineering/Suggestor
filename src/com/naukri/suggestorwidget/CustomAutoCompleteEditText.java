package com.naukri.suggestorwidget;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

/**
 * 
 * @author vishakha.tyagi
 * 
 *         <p>
 *         An editable text view that shows completion suggestions automatically
 *         while the user is typing. The list of suggestions is displayed in a
 *         drop down menu from which the user can choose an item to replace the
 *         content of the edit box with.
 *         </p>
 * 
 *         <p>
 *         The list of suggestions is obtained from a data adapter which gets
 *         filled by sending volley JSONObject request for given url and
 *         retrieving results from received volley response
 *         </p>
 * 
 *         <p>
 *         Suggestions dropdown would be displayed only when you set a parsing
 *         listener for web service response
 *         </p>
 * 
 * 
 */

public class CustomAutoCompleteEditText extends MultiAutoCompleteTextView
		implements CustomView {

	private ArrayList<String> mSuggestorItems = new ArrayList<String>();
	private ArrayAdapter<String> mSuggesterAdapter;

	private Context mContext;

	private String mSuggestersUrl;
	private String mUrlQuery;

	private RequestQueue mRequestQueue;
	private JsonObjectRequest mJsonRequest;

	private ProgressBar mLoadingIndicator;
	private ParsingListener<JSONObject> mParsingListener;
	private UrlQueryListener<String> mUrlQueryListener;

	private int defaultLevel = 0;
	private boolean mFilterCompletionFlag = false;
	// Threshhold defines no of characters to be typed after which list of
	// suggestions appears
	private int mThreshhold;
	private long mDelay;
	private String mTag;

	private long mCurrentTime = 0;
	private long mLastTime = 0;

	public static final int THRESHHOLD = 2;
	public static final String ERROR_MESSAGE = "Check your internet connenction.";

	public CustomAutoCompleteEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);

	}

	// To show progress loader on the right of autocomplete textview
	// while filtering gets completed

	private void init(Context context, AttributeSet attrs) {
		mContext = getContext();
		TypedArray typedArray = mContext.obtainStyledAttributes(attrs,
				R.styleable.CustomAutoCompleteEditText);
		mThreshhold = typedArray.getInteger(
				R.styleable.CustomAutoCompleteEditText_threshhold, THRESHHOLD);

		mDelay = Long.valueOf(typedArray
				.getString(R.styleable.CustomAutoCompleteEditText_delayTime));
		mTag = typedArray.getString(R.styleable.CustomAutoCompleteEditText_tag);
		if (mTag == null)
			throw new RuntimeException(
					" Attribute named \"tag\" is not defined for this view");

		// Make a volley request queue
		mRequestQueue = Volley.newRequestQueue(context);

		// Don't forget this
		typedArray.recycle();
	}

	public void setLoadingIndicator(ProgressBar progressBar) {
		mLoadingIndicator = progressBar;
	}

	/**
	 * set parsing listener
	 * 
	 * @param mParsingListener
	 */
	public void setParsingListener(ParsingListener<JSONObject> mParsingListener) {
		this.mParsingListener = mParsingListener;
	}

	/**
	 * set UrlQuery Method
	 * 
	 * @param mUrlQueryListener
	 */
	public void setUrlQueryListener(UrlQueryListener<String> mUrlQueryListener) {
		this.mUrlQueryListener = mUrlQueryListener;

	}

	@Override
	protected void onTextChanged(CharSequence text, int start,
			int lengthBefore, int lengthAfter) {

		super.onTextChanged(text, start, lengthBefore, lengthAfter);

		int idxOfSeperator = text.toString().lastIndexOf(",");
		String queryText = text.toString().substring(idxOfSeperator + 1).trim();

		mCurrentTime = System.currentTimeMillis();

		if (mFilterCompletionFlag)
			mLastTime = 0;

		if (this.isPerformingCompletion()) {
			mFilterCompletionFlag = true;
			// An item has been selected from the list. Ignore.
			return;

		}
		mFilterCompletionFlag = false;

		// When enough characters are deleted from the text view ,
		// Filter.filter(null) method gets called which in turn cancel all the
		// previous filtering requests and a new null filter request gets
		// posted. Hence we need to set the adapter again with a new filter.

		if (queryText.toString().length() < mThreshhold) {

			if (mSuggesterAdapter != null)
				mSuggesterAdapter.clear();

		}

		// If number of characters typed is more than given threshhold , then
		// web url request should be sent for retrieving suggestors .

		if (queryText.toString().length() >= mThreshhold) {
			try {
				if (mUrlQueryListener != null)
					mSuggestersUrl = mUrlQueryListener.getUrlQuery(
							URLEncoder.encode(queryText.toString(), "UTF-8"),
							mTag);

			} catch (UnsupportedEncodingException e) {

				e.printStackTrace();
			}
			if ((mCurrentTime - mLastTime) < this.mDelay) {
				if (mTag == null)
					mRequestQueue.cancelAll(this);
				else
					mRequestQueue.cancelAll(mTag);
				mHandler.removeCallbacks(run);
			}
			if (mSuggestersUrl != null)
				mHandler.postDelayed(run, mDelay);

		}
		mLastTime = mCurrentTime;

	}

	/**
	 * 
	 * @param string
	 *            text which user types in edit text box
	 * @param url
	 *            web api url for retrieving suggestors
	 * 
	 *            The method Initiate a volley request queue sends Volley
	 *            Request for given url parses volley response sets array
	 *            adapter for suggestors dropdown.
	 */

	private void sendRequest(String suggestersUrl) {
		if (suggestersUrl != null) {

			// Make a JSONObject request
			mJsonRequest = new JsonObjectRequest(Request.Method.GET,
					suggestersUrl, reponseListener, errorListener);

			if (mParsingListener != null) {

				/*
				 * if ((mCurrentTime - mLastTime) < this.mDelay) { if (mTag ==
				 * null) mRequestQueue.cancelAll(this); else
				 * mRequestQueue.cancelAll(mTag);
				 * mLoadingIndicator.setVisibility(View.INVISIBLE); } else {
				 */if (mTag == null)
					mJsonRequest.setTag(mTag);
				else
					mJsonRequest.setTag(mTag);
				// Add the request to the RequestQueue.
				mRequestQueue.add(mJsonRequest);
				mLoadingIndicator.setVisibility(View.VISIBLE);
			}
		} else
			Log.e("Error: ",
					"CustomAutoCompleteTextView would not get populated untill you set a parsing listener to this. ");
	}

	// Clears all entries from volleyCache

	public void clearVolleyCache() {
		if (this.mRequestQueue != null) {
			this.mRequestQueue.getCache().clear();
			if (this.mSuggesterAdapter != null)
				this.mSuggesterAdapter.clear();
		}

	}

	public void invalidateCache(String key) {
		if (this.mRequestQueue != null) {
			this.mRequestQueue.getCache().invalidate(key, true);
		}

	}

	// updates suggestors list

	public void updateSuggestorList(JSONArray topArray) throws JSONException {

		JSONObject suggestorItem;
		if (topArray == null) {
			return;
		}

		for (int i = 0; i < topArray.length(); i++) {
			suggestorItem = (JSONObject) topArray.get(i);
			if (suggestorItem != null) {
				mSuggestorItems.add(suggestorItem.getString("displayTextEn"));

			}

		}
	}

	public void updateSuggestorUrlQuery(String url) {
		mSuggestersUrl = url;
	}

	public void setLevel(int levelValue) {
		Drawable drawable = this.getBackground();
		drawable.setLevel(levelValue);
	}

	public void setErrorView() {
		setLevel(ERROR_VIEW);
	}

	public void setNormalView() {
		setLevel(defaultLevel);
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public void setBackground(Drawable drawable) {

		int[] arr = { this.getPaddingLeft(), this.getPaddingTop(),
				this.getPaddingRight(), this.getPaddingBottom() };

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			this.setBackgroundDrawable(drawable);
			this.setPadding(arr[0], arr[1], arr[2], arr[3]);
		} else {
			super.setBackground(drawable);
			super.setPadding(arr[0], arr[1], arr[2], arr[3]);
		}
	}

	Handler mHandler = new Handler();

	Runnable run = new Runnable() {
		public void run() {
			sendRequest(mSuggestersUrl);
		}
	};
	private Response.Listener<JSONObject> reponseListener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {

			if (mSuggestorItems != null)
				mSuggestorItems.clear();

			if (response != null && response.length() != 0) {

				JSONArray suggestorsList = mParsingListener.parseApiResponse(
						response, mTag);
				try {
					updateSuggestorList(suggestorsList);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (mSuggesterAdapter == null || mSuggesterAdapter.isEmpty()) {
					mSuggesterAdapter = new ArrayAdapter<String>(mContext,
							android.R.layout.simple_list_item_1,
							mSuggestorItems) {

					};
					setAdapter(mSuggesterAdapter);
					mSuggesterAdapter.notifyDataSetChanged();

				} else {
					mSuggesterAdapter.notifyDataSetChanged();

				}
				mLoadingIndicator.setVisibility(View.INVISIBLE);

			} else
				mLoadingIndicator.setVisibility(View.INVISIBLE);

		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			if (error.networkResponse == null)
				Toast.makeText(mContext, ERROR_MESSAGE, Toast.LENGTH_SHORT)
						.show();

			mLoadingIndicator.setVisibility(View.INVISIBLE);

		}
	};
}
