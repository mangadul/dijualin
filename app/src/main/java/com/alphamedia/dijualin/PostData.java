package com.alphamedia.dijualin;

/**
 * Created by abdulmuin on 02/02/17.
 */

import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostData {

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    String post(String url, String json) throws IOException {
        String rsp = "";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        /*
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            System.out.println(response.body().string());
        }
        */

        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            rsp = response.body().string();
        } catch (IOException ie){
            rsp = ie.getMessage();
            Log.i("Error post", ie.getMessage());
        }
        return rsp;
    }

    String formatData(String tgl, String barcode, String imei, double lng, double lat) {
        return "{'postdate':'"+tgl+"',"
                + "'barcode':'"+barcode+"',"
                + "'imei':'"+imei+"',"
                + "'longitude':'"+lng+"',"
                + "'latitude':'"+lat+"'"
                + "}";
    }

    /*
    public static void main(String[] args) throws IOException {
        PostData example = new PostData();
        String json = example.formatData("Jesse", "Jake");
        String response = example.post("http://plunk.alphamedia.id/barcode/post.php", json);
        System.out.println(response);
    }
    */

}