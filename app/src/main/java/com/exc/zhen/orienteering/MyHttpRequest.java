package com.exc.zhen.orienteering;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import org.json.JSONArray;
/**
 * Created by ZHEN on 2015/10/9 0009.
 * send and get data from server.
 */
public class MyHttpRequest {
    private final static String TAG = "MyDebug";
    //private final static String BASEURL="http://drghostserver.sinaapp.com:80";
    //private final static String BASEURL="http://127.0.0.1:8080";
    private final static String savePath = Environment.getExternalStorageDirectory().
            getPath()+"/orienteeringImg/";
    private final static String BOUNDARY = java.util.UUID.randomUUID().toString();
    private final static String PREFIX = "--", LINEND = "\r\n";
    private final static String MULTIPART_FROM_DATA = "multipart/form-data";
    private final static String JSON_FORM = "application/json";
    private final static String CHARSET = "UTF-8";
    private final static String ACCEPT_ENCODING_TYPE = "gzip,deflate";

    static String postJson(String urlpath,String jsonstr) throws IOException{
        URL url = new URL(urlpath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);//设置5秒连接超时
        conn.setDoInput(true);//设置允许输入
        conn.setDoOutput(true);//设置允许输出
        conn.setUseCaches(false);//不许缓存
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Charset", CHARSET);
        conn.setRequestProperty("Content-Type", JSON_FORM);
        conn.setRequestProperty("Accept-Encoding", ACCEPT_ENCODING_TYPE);
        //conn.setRequestProperty("Content-Length", String.valueOf(jsonstr.length()));

        DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
        outStream.write(jsonstr.getBytes());
        outStream.flush();
        outStream.close();
        String resp = "";
        if (200==conn.getResponseCode()) {
            InputStream inStream = conn.getInputStream();
            InputStreamReader isReader = new InputStreamReader(inStream);
            BufferedReader bufReader = new BufferedReader(isReader);
            String line = null;
            while((line=bufReader.readLine())!=null)
                resp+=line;
        }else resp = null;
        conn.disconnect();
        return resp;
    }
    static String postFiles(String urlpath,List<String> filenames) throws IOException{
        URL url = new URL(urlpath);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);//设置5秒连接超时
        conn.setDoInput(true);//设置允许输入
        conn.setDoOutput(true);//设置允许输出
        conn.setUseCaches(false);//不许缓存
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Charset", CHARSET);
        conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA
                +"; boundary=" + BOUNDARY);
        conn.setRequestProperty("Accept-Encoding", ACCEPT_ENCODING_TYPE);
        DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
        if (null!=filenames) {
            int i=0;
            for (String filename : filenames) {
                i++;
                StringBuilder sb1 = new StringBuilder();
                sb1.append(PREFIX+BOUNDARY+LINEND);
                sb1.append("Content-Disposition: form-data; name=\"file"+i+"\";"
                        + "filename=\""+filename+"\""+LINEND);
                sb1.append("Content-Type: image/jpeg" + LINEND);
                sb1.append(LINEND);
                outStream.write(sb1.toString().getBytes());
                InputStream is = new FileInputStream(new File(savePath+filename));
                byte[] buffer = new byte[1024];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, len);
                }
                is.close();
                outStream.write(LINEND.getBytes());
            }
        }
        // 请求结束标志
        byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
        outStream.write(end_data);
        outStream.flush();
        String resp = "";
        if (200==conn.getResponseCode()) {
            InputStream inStream = conn.getInputStream();
            InputStreamReader isReader = new InputStreamReader(inStream);
            BufferedReader bufReader = new BufferedReader(isReader);
            String line = null;
            while((line=bufReader.readLine())!=null)
                resp+=line;
        }else {
            resp = conn.getResponseMessage();
        }
        conn.disconnect();
        return resp;
    }
    static int getFile(String urlpath,String filename) throws IOException{
        URL url = new URL(urlpath+filename);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);//设置5秒连接超时
        conn.setDoInput(true);//设置允许输入
        conn.setUseCaches(false);//不许缓存
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Charset", CHARSET);
        if (200==conn.getResponseCode()) {
            InputStream is = conn.getInputStream();
            File file = new File(savePath+filename);
            FileOutputStream fos = new FileOutputStream(file);
            int len=0;
            byte[] buffer = new byte[1024];
            while((len=is.read(buffer))!=-1)
                fos.write(buffer, 0, len);
            fos.flush();
            Log.i(TAG, filename + " downloaded");
            conn.disconnect();
            return 1;
        }else{
            Log.i(TAG, conn.getResponseMessage());
            conn.disconnect();
            return -1;
        }
    }
}
