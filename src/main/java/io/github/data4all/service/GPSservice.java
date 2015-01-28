package io.github.data4all.service;

import io.github.data4all.logger.Log;
import io.github.data4all.util.Optimizer;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Toast;

/**
 * A service for listening for location changes.
 * 
 * @author konermann
 * 
 */
public class GPSservice extends Service implements LocationListener {

    private static final String TAG = "GPSservice";
    private LocationManager lmgr;
    private WakeLock wakeLock;
    private PowerManager powerManager;
    private final IBinder mBinder = new LocalBinder();
    /*
     * the minimum of time after we get a new locationupdate
     */
    private final long minTime = 1000;
    /*
     * the minimum of Distance after we get a new locationupdate
     */
    private final float minDistance = 0;

    @Override
    public void onCreate() {
        Log.d("GPSSERVICE", "service started");
        // wakelock, so the cpu is never shut down and is able to track at all
        // time
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GPSservice");
        wakeLock.acquire();

        lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Optimizer.putLoc(findLastKnownLocation());

        if (lmgr.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
            lmgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // second value is minimum of time, third value is minimum of meters
            lmgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
                    minDistance, this);
        }

        if (lmgr.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
            lmgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    minTime, minDistance, this);
        }
    }

    /**
     * Returns the last known location. <br/>
     *
     * first from GPS-Provider, if it does not have a location we check the
     * networkprovider for a location.
     * 
     * @return Location the last known location
     */

    public Location findLastKnownLocation() {
        Location loc = lmgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (loc != null) {
            Log.d(this.getClass().getSimpleName(), "lastknownloc from gps");
            return loc;
        }
        loc = lmgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (loc != null) {
            Log.d(this.getClass().getSimpleName(), "lastknownloc from network");
            return loc;
        }
        Log.d(this.getClass().getSimpleName(), "lastknownloc is null");
        return null;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();

    }

    @Override
    public void onLocationChanged(Location loc) {

        Optimizer.putLoc(loc);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Not interested in provider status

    }

    @Override
    public void onProviderEnabled(String provider) {
        // Not interested in provider status
    }

    @Override
    public void onProviderDisabled(String provider) {

        Toast.makeText(getBaseContext(),
                "Gps turned off, GPS tracking not possible ", Toast.LENGTH_LONG)
                .show();
    }

    public class LocalBinder extends Binder {
        public GPSservice getService() {
            return GPSservice.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
