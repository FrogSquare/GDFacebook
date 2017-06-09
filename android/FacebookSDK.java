/**
 * Copyright 2017 FrogSquare. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package org.godotengine.godot;

import android.app.*;
import android.content.pm.ActivityInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestBatch;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.model.GameRequestContent.ActionType;
import com.facebook.share.widget.GameRequestDialog;

import org.godotengine.godot.Godot;
import org.godotengine.godot.Dictionary;

import com.godot.game.R;
import org.godotengine.godot.KeyValueStorage;

public class FacebookSDK {

	private static final String PARAM_FIELDS = "fields";

	public static FacebookSDK getInstance(Activity p_activity) {
		if (mInstance == null) {
			synchronized (FacebookSDK.class) {
				mInstance = new FacebookSDK(p_activity);
			}
		}

		return mInstance;
	}

	public FacebookSDK (Activity p_activity) {
		activity = p_activity;
	}

	public void init (String p_appID) {
		APP_ID = p_appID;

		FacebookSdk.sdkInitialize(activity);
		FacebookSdk.setApplicationId(APP_ID);

		initCallbacks();

		AppEventsLogger.activateApp(activity);
		Utils.callScriptFunc("initialized", "true");
		Utils.d("Facebook:Initialized");

		onStart();
	}

	private void initCallbacks() {
		callbackManager = CallbackManager.Factory.create();

		requestDialog = new GameRequestDialog(activity);
		requestDialog.registerCallback(callbackManager,
		new FacebookCallback<GameRequestDialog.Result>() {
			@Override
			public void onSuccess(GameRequestDialog.Result result) {
				Utils.d("Facebook:Request:Send:success");
			}

			@Override
			public void onCancel() {
				Utils.d("Facebook request has been canceled");
			}

			@Override
			public void onError(FacebookException error) {
				Utils.d("Facebook request error: " + error.toString());
			}
		});

		LoginManager.getInstance().registerCallback(callbackManager,
		new FacebookCallback<LoginResult>() {
			@Override
			public void onSuccess(LoginResult result) {
				Log.d("godot", "Facebook login success");
				Utils.d("" + result.toString());

				accessToken = result.getAccessToken();
				successLogin();
			}

			@Override
			public void onCancel() {
				Log.d("godot", "Facebook login cancel");
				Utils.callScriptFunc("login", "cancel");
			}

			@Override
			public void onError(FacebookException exception) {
				Log.d("godot", "Facebook login error");
				Utils.callScriptFunc("login", "error");
			}
		});

		accessTokenTracker = new AccessTokenTracker() {
			@Override
			protected void onCurrentAccessTokenChanged (
				AccessToken oldAccessToken,
				AccessToken currentAccessToken) {

				if (currentAccessToken == null) { successLogOut(); }
				else { accessToken = currentAccessToken; }
			}
		};

		profileTracker = new ProfileTracker() {
			@Override
			protected void onCurrentProfileChanged(
				Profile oldProfile,
				Profile currentProfile) {

				profile = currentProfile;
			}
		};

		accessTokenTracker.startTracking();
		profileTracker.startTracking();

		accessToken = AccessToken.getCurrentAccessToken();
		profile = Profile.getCurrentProfile();
	}

	private void askForPublishActionsForScores() {
		new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT)
		.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				requestPublishPermissions();
			}
		})
		.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//hideGameOverContainer();
			}
		})
		.setTitle(R.string.publish_scores_dialog_title)
		.setMessage(R.string.publish_scores_dialog_message)
		.show();
	}

	public void requestPublishPermissions() {
		LoginManager.getInstance().logInWithPublishPermissions(
		activity, Arrays.asList("publish_actions"));
	}

	public void connect() {
		if (callbackManager == null) { return; }

		LoginManager.getInstance().logInWithReadPermissions(
		activity, Arrays.asList("public_profile", "email"));
	}

	public void disconnect() {
		if (callbackManager == null) { return; }

		Utils.d("Facebook logout");
		LoginManager.getInstance().logOut();
	}

	private void successLogOut() {
		loggedIn = false;
		dets_loaded = false;

		Utils.d("Facebook logged out");

		friends = null;
		currentFBUser = null;

		myTopScore = -1;

		Utils.callScriptFunc("login", "false");
	}

	private void successLogin() {
		loggedIn = true;

		accessToken = AccessToken.getCurrentAccessToken();
		profile = Profile.getCurrentProfile();

		if (profile != null) { Utils.d("Name: " + profile.getName()); }

		Utils.callScriptFunc("login", "true");
		fetchUserInformationAndLogin();
	}

	public boolean isDetsLoaded() {
		return dets_loaded;
	}

	public boolean isConnected() {
		return loggedIn;
	}

	public ArrayList<String> getFriendsAsArrayListOfString() {
		ArrayList<String> friendsAsArrayListOfStrings = new ArrayList<String>();

		int numFriends = friends.length();
		for (int i = 0; i < numFriends; i++) {
			friendsAsArrayListOfStrings.add(getFriend(i).toString());
		}

		return friendsAsArrayListOfStrings;
	}

	public JSONObject getFriend (int index) {
		JSONObject friend = null;

		if (friends != null && friends.length() > index) {
			friend = friends.optJSONObject(index);
		}

		return friend;
	}

	public void fetchUserInformationAndLogin() {
		AccessToken token = AccessToken.getCurrentAccessToken();

		GraphRequest myFriends = GraphRequest.newMyFriendsRequest(
		token, new GraphRequest.GraphJSONArrayCallback() {
			@Override
			public void onCompleted(JSONArray users, GraphResponse response) {
				FacebookRequestError error = response.getError();

				if (error == null) {
					JSONObject graphObject = response.getJSONObject();
					JSONArray data = graphObject.optJSONArray("data");
					setFriends(data);
					Utils.d("Friends List: " + data.toString());
				} else { Utils.d("Response Error: " + error.toString()); }
			}
		});

		Bundle params = myFriends.getParameters();
		params.putString(PARAM_FIELDS, "name,first_name");
		myFriends.setParameters(params);

		GraphRequest meCall = GraphRequest.newMeRequest(
		token, new GraphRequest.GraphJSONObjectCallback() {
			@Override
			public void onCompleted(JSONObject jsonObject, GraphResponse response) {
				FacebookRequestError error = response.getError();

				if (error == null) {
					JSONObject user = response.getJSONObject();
					setCurrentFBUser(user);
				} else { Utils.d("Response Error: " + error.toString()); }
			}
		});

		GraphRequest meScore = GraphRequest.newGraphPathRequest(
		token, "me/score", new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse response) {
				FacebookRequestError error = response.getError();

				if (error == null) {
					JSONArray ar = response.getJSONObject().optJSONArray("data");
					JSONObject data = ar.optJSONObject(0);

					if (data != null) {
						int score = data.optInt("score");
						myTopScore = score;

						KeyValueStorage.setValue(
						"bestScore_m", Integer.toString(score));

						Utils.d("My:Top:Score:" + score);
					}

				} else { Utils.d("Response:Error:" + error.toString()); }
			}
		});

		GraphRequestBatch batch = new GraphRequestBatch();
		batch.add(myFriends);
		batch.add(meCall);
		batch.add(meScore);

		batch.addCallback(new GraphRequestBatch.Callback() {
			@Override
			public void onBatchCompleted(GraphRequestBatch response) {
				dets_loaded = true;
			}
		});


		batch.executeAsync();
	}

	public void setCurrentFBUser(JSONObject user) {
		this.currentFBUser = user;
	}

	public void setFriends(JSONArray friends) {
		this.friends = friends;
	}

	public JSONArray getFriends() {
		return friends;
	}

	public void submitScore(final int score) {
		if (!loggedIn) {
			Utils.d("Facebook:NotConnected");
			return;
		}

		AccessToken token = AccessToken.getCurrentAccessToken();
		if (!token.getPermissions().contains("publish_actions")) {
			askForPublishActionsForScores();
		}

		JSONObject object = new JSONObject();

		try { object.put("score", score); }
		catch (JSONException e) { Utils.d("Error:Publishing:Score"); }

		GraphRequest graphRequest = GraphRequest.newPostRequest(token,
		"me/scores", object, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse response) {
				Utils.d(response.toString());
			}
		});

		graphRequest.executeAsync();
		Utils.d("F Score submitting: " + score);
	}

	public void loadRequests() {
		AccessToken token = AccessToken.getCurrentAccessToken();

		GraphRequest myRequests = GraphRequest.newGraphPathRequest(
		token, "/me/apprequests", new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse response) {
				FacebookRequestError error = response.getError();

				if (error == null) {
					JSONObject graphObject = response.getJSONObject();
					JSONArray data = graphObject.optJSONArray("data");

					Utils.callScriptFunc("pendingRequest", data.toString());
				} else { Utils.d("Response Error: " + error.toString()); }
			}
		});

		myRequests.executeAsync();
	}

	public void showDialogForInvites(String title, String message) {
		GameRequestContent content = new GameRequestContent.Builder()
		.setTitle(title)
		.setMessage(message)
		.setFilters(GameRequestContent.Filters.APP_NON_USERS)
		.build();

		requestDialog.show(content);
	}

	public void showDialogForRequests (String title, String message) {
		GameRequestContent content = new GameRequestContent.Builder()
		.setTitle(title)
		.setMessage(message)
		.setFilters(GameRequestContent.Filters.APP_USERS)
		.build();

		requestDialog.show(content);
	}

	public void showDialogForDirectRequests (String title, String message, List<String> recip) {
		GameRequestContent content = new GameRequestContent.Builder()
		.setTitle(title)
		.setMessage(message)
		.setRecipients(recip)
		.build();

		requestDialog.show(content);
	}

	public void showDialogForChallenge (String title, String message, List<String> recip) {
		GameRequestContent content = new GameRequestContent.Builder()
		.setTitle(title)
		.setMessage(message)
		.setRecipients(recip)
		.build();

		requestDialog.show(content);
	}

	public static void deleteRequest (String requestId) {
		// delete Requets here GraphAPI.
		Utils.d("Deleting:Request:" + requestId);

		AccessToken token = AccessToken.getCurrentAccessToken();
		GraphRequest graphRequest = GraphRequest.newDeleteObjectRequest(
		token, requestId, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse response) {
				FacebookRequestError error = response.getError();
				if (error == null) { Utils.d("OnDelete:Req:" + response.toString()); }
			}
		});

		graphRequest.executeAsync();
	}

	public static void getUserDataFromRequest (String requestId) {
		// Grah Api to get user data from request.

		AccessToken token = AccessToken.getCurrentAccessToken();
		GraphRequest graphRequest = GraphRequest.newGraphPathRequest(
		token, requestId, new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse response) {
				FacebookRequestError error = response.getError();

				if (error == null) { Utils.d("Response: " + response.toString()); }
				else { Utils.d("Error: " + response.toString()); }
			}
		});

		graphRequest.executeAsync();
	}


	public void loadTopScore() {
		AccessToken token = AccessToken.getCurrentAccessToken();

		GraphRequest graphRequest = GraphRequest.newGraphPathRequest(
		token, "/" + APP_ID + "/scores?fields=score&limit=20",
		new GraphRequest.Callback() {
			@Override
			public void onCompleted(GraphResponse response) {
				FacebookRequestError error = response.getError();

				Utils.d("Resonce:Top:Scores: " + response.toString());

				if (error == null) {
					JSONArray ret = new JSONArray();
					JSONArray data = response.getJSONObject().optJSONArray("data");
					for (int i = 0; i < data.length(); i++) {
						JSONObject jobj = data.optJSONObject(i);
						JSONObject user = jobj.optJSONObject("user");

						Utils.d("USER:Data:" + user.toString());

						int score = jobj.optInt("score");
						String userName = user.optString("name");

						JSONObject retObj = new JSONObject();
						try {
							retObj.put("score", score);
							retObj.put("name", userName);
						} catch(JSONException e) {
							Utils.d("Loading:Scores:exception");
						}

						Utils.d("Adding:User:" + userName);
						ret.put(retObj);
					}

					Utils.callScriptFunc("topScoresList", ret.toString());
				}
			}
		});

//		Bundle params = graphRequest.getParameters();
//		params.putString(PARAM_FIELDS, "name,first_name,score");
//		graphRequest.setParameters(params);

		graphRequest.executeAsync();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		callbackManager.onActivityResult(requestCode, resultCode, data);
	}

	public void onStart() {
		if (accessToken == null || accessToken.isExpired()) {
			Utils.d("AccessToken:Expired:OR:NotLoggedIN");
			loggedIn = false;

			Utils.callScriptFunc("login", "false");
		} else {
			Utils.d("AccessToken:Valid");
			successLogin();
		}
	}

	// Add to each long-lived activity
	public void onResume() { 
		AppEventsLogger.activateApp(activity); 
	}
  
	// for Android, you should also log app deactivation
	public void onPause() { 
		AppEventsLogger.deactivateApp(activity);
	}

	public void onStop() {
		accessTokenTracker.stopTracking();
		profileTracker.stopTracking();
	}

	private static String APP_ID;

	private static GameRequestDialog requestDialog;
	private static CallbackManager callbackManager;

	private static AccessTokenTracker accessTokenTracker;
	private static AccessToken accessToken;

	private static ProfileTracker profileTracker;
	private static Profile profile;

	private Boolean loggedIn = false;
	private Boolean dets_loaded = false;

	private JSONObject currentFBUser = new JSONObject();
	private JSONArray friends = new JSONArray();
	private int myTopScore;

	private static Activity activity;
	private static FacebookSDK mInstance = null;
}
