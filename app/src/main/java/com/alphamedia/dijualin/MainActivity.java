package com.alphamedia.dijualin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.onesignal.OneSignal;
import com.onesignal.OneSignal.IdsAvailableHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button button;
    String imei;

    private static Activity currentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OneSignal.idsAvailable(new IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {

                String text = "OneSignal UserID:\n" + userId + "\n\n";
                if (registrationId != null)
                    text += "Google Registration Id:\n" + registrationId;
                else
                    text += "Google Registration Id:\nCould not subscribe for push";

                TextView textView = (TextView)findViewById(R.id.debug_view);
                textView.setText(text);
            }
        });

        button = (Button) this.findViewById(R.id.button);
        final Activity activity = this;
        imei = getIMEI(MainActivity.this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(activity);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Scan");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });
    }

    private String getIMEI(Context context){
        TelephonyManager mngr = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        String imei = mngr.getDeviceId();
        return imei;
    }

    private boolean isGPSOn()
    {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE );
        boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return statusOfGPS;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.d("MainActivity", "Cancelled scan");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();

                OneSignal.clearOneSignalNotifications();

            } else {
                String barcode = data.getStringExtra("SCAN_RESULT");
                String format = data.getStringExtra("SCAN_RESULT_FORMAT");
                Log.d("MainActivity", "Scanned");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String tgl = sdf.format(new Date());
                Toast.makeText(this, "Scanned: " + barcode, Toast.LENGTH_LONG).show();

                OneSignal.clearOneSignalNotifications();

                if(isGPSOn()){
                    GPSTracker gps = new GPSTracker(MainActivity.this);
                    if(gps.canGetLocation()){
                        double latitude = gps.getLatitude();
                        double longitude = gps.getLongitude();
                        Toast.makeText(this, "Imei "+ imei.toString() +"\nYour Location is - Lat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                        if(!Double.valueOf(longitude).toString().isEmpty() && !Double.valueOf(latitude).toString().isEmpty())
                        {
                            HashMap<String, String> params = new HashMap<>();
                            params.put( "tanggal", tgl );
                            params.put( "imei", imei );
                            params.put( "barcode", barcode );
                            params.put( "format", format );
                            params.put( "longitude", Double.toString(longitude) );
                            params.put( "latitude", Double.toString(latitude));

                            FormBody.Builder builder = new FormBody.Builder();

                            for ( Map.Entry<String, String> entry : params.entrySet() ) {
                                builder.add( entry.getKey(), entry.getValue() );
                            }

                            final OkHttpClient client = new OkHttpClient();

                            RequestBody formBody = builder.build();
                            final Request request = new Request.Builder()
                                    .url("http://plunk.alphamedia.id/barcode/post.php")
                                    .post( formBody )
                                    .build();

                            AsyncTask<Void, Void, String> asyncTask = new AsyncTask<Void, Void, String>() {
                                @Override
                                protected String doInBackground(Void... params) {
                                    try {
                                        Response response = client.newCall(request).execute();
                                        if (!response.isSuccessful()) {
                                            return null;
                                        } else Toast.makeText(getApplicationContext(), "Response dari server "+ response.body().string(), Toast.LENGTH_SHORT).show();
                                        return response.body().string();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }

                                @Override
                                protected void onPostExecute(String s) {
                                    super.onPostExecute(s);
                                    if (s != null) {
                                    }
                                }
                            };

                            asyncTask.execute();

                        }

                    }else{
                        gps.showSettingsAlert();
                    }
                } else {
                    Toast.makeText(this, "GPS Error: Mohon Aktifkan GPS anda untuk mengetahui lokasi saat ini", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

}