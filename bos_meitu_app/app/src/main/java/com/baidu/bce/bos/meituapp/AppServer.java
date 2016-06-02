package com.baidu.bce.bos.meituapp;

import android.util.Log;

import com.baidubce.BceClientConfiguration;
import com.baidubce.http.HttpClientFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by gavin on 5/14/16.
 */
public class AppServer {
    /**
     * get info from app server for the file to upload to or download from BOS
     *
     * @param appServerEndpoint app server
     * @param userName          the app user's name, registered in app server
     * @param bosOperationType  download? upload? or?
     * @return STS, and BOS endpoint, bucketName, prefix, path, object name etc
     */
    public static Map<String, Object> getBosInfoFromAppServer(String appServerEndpoint, String userName, BosOperationType bosOperationType) {
        String type = "";
        switch (bosOperationType) {
            // to simplify
            case UPLOAD: {
                type = "upload";
                break;
            }
            case DOWNLOAD: {
                type = "download";
                break;
            }
            case DOWNLOAD_PROCESSED: {
                type = "download-processed";
                break;
            }
            default:{
                break;
            }
        }
        // TODO: this url should be url encoded
        String appServerUrl = appServerEndpoint + "/?" + "userName=" + userName + "&command=stsToken&type=" + type;

        // create a http client to contact app server to get sts
        HttpParams httpParameters = new BasicHttpParams();
        HttpClient httpClient = new DefaultHttpClient(httpParameters);

        HttpGet httpGet = new HttpGet(appServerUrl);
        httpGet.addHeader("User-Agent", "bos-meitu-app/demo");
        httpGet.setHeader("Accept", "*/*");
        try {
            httpGet.setHeader("Host", new URL(appServerUrl).getHost());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        httpGet.setHeader("Accept-Encoding", "identity");

        Map<String, Object> bosInfo = new HashMap<String, Object>();
        try {
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            long len = entity.getContentLength();
            InputStream is = entity.getContent();
            int off = 0;
            byte[] b = new byte[(int) len];
            while (true) {
                int readCount = is.read(b, off, (int) len);
                if (readCount < 0) {
                    break;
                }
                off += readCount;
            }
            Log.d("AppServer", new String(b, "utf8"));
            JSONObject jsonObject = new JSONObject(new String(b, "utf8"));
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                bosInfo.put(key, jsonObject.get(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return bosInfo;
    }

    public enum BosOperationType {
        UPLOAD,
        DOWNLOAD,
        DOWNLOAD_PROCESSED,
    }
}
