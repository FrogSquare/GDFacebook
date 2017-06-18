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

import com.godot.game.R;
import org.godotengine.godot.KeyValueStorage;

public class Facebook extends Godot.SingletonBase {

	static public Godot.SingletonBase initialize (Activity p_activity) {
		return new Facebook(p_activity);
	}

	public Facebook(Activity p_activity) {
		registerClass ("Facebook", new String[] {
			"init", "login", "logout", "isConnected", "isDetsLoaded", "submitScore",
			"getFriends", "loadScoreBoard", "sendInvitation", "sendRequest",
			"sendDirectRequest", "deleteFBRequest", "loadPendingRequest",
			"sendGift", "requestGift"
		});

		activity = p_activity;
		KeyValueStorage.set_context(activity.getApplicationContext());
	}

	public void init(final String appID, final int script_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				Utils.setScriptInstance(script_id);
				FacebookSDK.getInstance(activity).init(appID);
			}
		});
	}

/**
	public void  (final String appID, final int script_id) {
		activity.runOnUiThread(new Runnable() {
			public void run() {

			}
		});
	}

**/
	public void  login() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).connect();
			}
		});
	}

	public void logout() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).disconnect();
			}
		});
	}

	public boolean isConnected() {
		return FacebookSDK.getInstance(activity).isConnected();
	}

	public boolean isDetsLoaded() {
		return FacebookSDK.getInstance(activity).isDetsLoaded();
	}

	public void submitScore(final int score) {
		if (!isConnected()) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).submitScore(score);
			}
		});
	}

	public String getFriends() {
		if (!isConnected()) { return "NULL"; }

		return FacebookSDK.getInstance(activity).getFriends().toString();
	}

	public void sendInvitation (final String message) {
		if (!isConnected()) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity) .showDialogForInvites(
				activity.getString(R.string.godot_project_name_string), message);
			}
		});
	}

	public void sendRequest (final String message) {
		if (!isConnected()) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).showDialogForRequests(
				activity.getString(R.string.godot_project_name_string),
				message);
			}
		});
	}

	public void sendChallenge (final String message, final String[] recip) {
		if (!isConnected()) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).showDirectRequests(
				activity.getString(R.string.godot_project_name_string),
				message, Arrays.asList(recip));
			}
		});
	}

	public void loadScoreBoard () {
		if (!isConnected()) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).loadTopScore();
			}
		});
	}

	public void loadPendingRequest() {
		if (!isConnected()) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).loadRequests();
			}
		});
	}

	public void deleteFBRequest (final String req_id) {
		if (!isConnected()) { return; }

		activity.runOnUiThread(new Runnable() {
			public void run() {
				FacebookSDK.getInstance(activity).deleteRequest(req_id);
			}
		});
	}

	protected void onMainActivityResult (int requestCode, int resultCode, Intent data) {
		FacebookSDK.getInstance(activity).onActivityResult(requestCode, resultCode, data);
	}

	protected void onMainResume () {
		FacebookSDK.getInstance(activity).onResume();
	}
  
	protected void onMainPause () {
		FacebookSDK.getInstance(activity).onPause();
	}

	protected void onMainDestroy () {
		FacebookSDK.getInstance(activity).onStop();
	}

	private static Activity activity = null;
}
