package com.example.guptarak.tomtom_map_sample.request;


import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class SearchResultsRequest extends Request {
    private Response.Listener listener;
    private Gson gson;
    private Class responseClass;
    private JSONObject jsonObject;
    private String mbody;
    protected static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE =String.format("application/json; charset=%s", PROTOCOL_CHARSET);
    public SearchResultsRequest(int method, String url,JSONObject jsonObject,Class responseClass, Response.Listener listener, Response.ErrorListener errorListener) {
        super(method, url,errorListener);
        gson = new Gson();
        this.listener=listener;
        this.responseClass=responseClass;
        this.mbody =  jsonObject.toString();
    }

    @Override
    protected Response parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));

            return Response.success(gson.fromJson(jsonString,responseClass), HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        }
    }

    @Override
    public byte[] getPostBody() throws AuthFailureError {
        return getBody();
    }

    @Deprecated
    @Override
    public String getPostBodyContentType() {
        return getBodyContentType();
    }

    @Override
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        try {
            return mbody == null ? null : mbody.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException uee) {
            return null;
        }
    }

    @Override
    protected void deliverResponse(Object response) {
        listener.onResponse(response);
    }
}
