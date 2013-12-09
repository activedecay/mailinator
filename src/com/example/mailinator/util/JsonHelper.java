package com.example.mailinator.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonHelper {
    public static JSONArray getJsonArray(String json, String key) {

        JSONObject obj = null;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert obj != null;
        return getJsonArray(obj, key);
    }

    public static JSONArray getJsonArray(JSONObject o, String key) {
        JSONArray jsonArray = null;
        try {
            jsonArray = o.getJSONArray(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public static String getJsonString(String json, String key) {
        JSONObject unsetObj = null;
        try {
            unsetObj = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        assert unsetObj != null;
        return getJsonString(unsetObj, key);
    }

    public static String getJsonString(JSONObject o, String key) {
        String value = null;
        try {
            value = o.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static Boolean getJsonBoolean(JSONObject o, String key) {
        Boolean value = null;
        try {
            value = o.getBoolean(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static Long getJsonLong(JSONObject o, String key) {
        Long value = null;
        try {
            value = o.getLong(key);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }
}