package id.ac.ui.cs.mobileprogramming.bryanza.luaraplikasi;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

public class MainScreen extends AppCompatActivity {

    List<Address> addresses;

    private class GPSTracker extends Service implements LocationListener {

        private final Context mContext;
        private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
        private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
        LocationManager lm;
        public GPSTracker(Context context) {
            this.mContext = context;
            lm = (LocationManager) mContext.getSystemService(mContext.LOCATION_SERVICE);
        }

        public double[] getLoc() {
            //LocationManager lm = (LocationManager) mContext.getSystemService(mContext.LOCATION_SERVICE);
            if ( ContextCompat.checkSelfPermission( (Activity) mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

                ActivityCompat.requestPermissions( (Activity) mContext, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                        this.MY_PERMISSION_ACCESS_COARSE_LOCATION );
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000*60*30,100,this);
            List<String> providers = lm.getProviders(true);

            Location l = null;

            for (int i=providers.size()-1; i>=0; i--) {
                try {
                    l = lm.getLastKnownLocation(providers.get(i));
                } catch (SecurityException e) {

                }
                //l = lm.getLastKnownLocation(providers.get(i));
                if (l != null) break;
            }

            double[] gps = new double[2];
            if (l != null) {
                gps[0] = l.getLongitude();
                gps[1] = l.getLatitude();
            }
            return gps;
        }
        @Override
        public IBinder onBind(Intent arg0) {
            return null;
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onLocationChanged(Location location) {
            //lm.removeUpdates(this);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

    }

    private class GetContacts extends AsyncTask<Void, Void, Void> {
        double[] loc;
        @Override
        protected Void doInBackground(Void... arg0) {
            final GPSTracker gps = new GPSTracker(MainScreen.this);
            final Geocoder geocoder = new Geocoder(MainScreen.this, Locale.getDefault());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loc = gps.getLoc();
                    String cityName = "lost. Turn on your GPS, then click Refresh.";
                    try {
                        addresses = geocoder.getFromLocation(loc[1], loc[0], 1);
                        if (addresses.size() != 0) {
                            cityName = "at " + addresses.get(0).getAddressLine(0);
                        }
                    } catch (final IOException e) {
                        if (loc.length != 0){
                            cityName = "at latitude " + loc[1] + " and longitude " + loc[0];
                            Log.e("exception", e.toString());
                        }
                    }
                    TextView longlat = (TextView) findViewById(R.id.longlat);
                    longlat.setText("You are "+cityName);
                }
            });
            return null;
        }
    }

    private void centerTitle() {
        ArrayList<View> textViews = new ArrayList<>();

        getWindow().getDecorView().findViewsWithText(textViews, getTitle(), View.FIND_VIEWS_WITH_TEXT);

        if(textViews.size() > 0) {
            AppCompatTextView appCompatTextView = null;
            if(textViews.size() == 1) {
                appCompatTextView = (AppCompatTextView) textViews.get(0);
            } else {
                for(View v : textViews) {
                    if(v.getParent() instanceof Toolbar) {
                        appCompatTextView = (AppCompatTextView) v;
                        break;
                    }
                }
            }

            if(appCompatTextView != null) {
                ViewGroup.LayoutParams params = appCompatTextView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                appCompatTextView.setLayoutParams(params);
                appCompatTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
        }
    }

    private void getIP(){
        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        int ipAddress = wifiInf.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        TextView ipaddr = (TextView) findViewById(R.id.ipaddr);
        ipaddr.setText("Your IP Address is "+ ip);
    }

    private void getOperator(){
        TelephonyManager telephonyManager =((TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE));
        String networkName = telephonyManager.getNetworkOperatorName();
        String simName = telephonyManager.getSimOperatorName();
        TextView opnumber = (TextView) findViewById(R.id.opnumber);
        opnumber.setText("Your Network Operator is "+ networkName + " and your SIM Operator is " + simName);
    }

    private void countContact(){
        Cursor cursor =  getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        int count = cursor.getCount();
        TextView sumcontact = (TextView) findViewById(R.id.sumcontact);
        sumcontact.setText("Your Number of Contacts are " + count);
    }

    private void getBatteryCapacity() {
        Object mPowerProfile_ = null;

        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

        try {
            mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS)
                    .getConstructor(Context.class).newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            double batteryCapacity = (Double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getAveragePower", java.lang.String.class)
                    .invoke(mPowerProfile_, "battery.capacity");
//            Toast.makeText(MainScreen.this, batteryCapacity + " mah",
//                    Toast.LENGTH_LONG).show();
            TextView remainbatt = (TextView) findViewById(R.id.remainbatt);
            remainbatt.setText("Your Current Battery is " + batteryCapacity + " mAh remaining");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        centerTitle();
        new GetContacts().execute();
        getIP();
        getOperator();
        countContact();
        getBatteryCapacity();
    }
}
