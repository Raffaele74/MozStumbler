/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.mozstumbler.client.mapview;

import android.location.Location;

import com.ekito.simpleKML.model.Coordinate;

import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.mozstumbler.client.ClientPrefs;
import org.mozilla.mozstumbler.service.core.logging.ClientLog;
import org.mozilla.mozstumbler.service.stumblerthread.datahandling.MLSJSONObject;
import org.mozilla.mozstumbler.service.stumblerthread.datahandling.StumblerBundle;
import org.mozilla.mozstumbler.svclocator.ServiceLocator;
import org.mozilla.mozstumbler.svclocator.services.log.ILogger;
import org.mozilla.mozstumbler.svclocator.services.log.LoggerUtil;

public class ObservationPoint implements AsyncGeolocate.MLSLocationGetterCallback {

    private static final ILogger Log = (ILogger) ServiceLocator.getInstance().getService(ILogger.class);
    private static final String LOG_TAG = LoggerUtil.makeLogTag(ObservationPoint.class);

    public final Location pointGPS;
    public Coordinate pointMLS;
    public int mWifiCount;
    public int mCellCount;
    public int mTrackSegment;
    private JSONObject mMLSQuery;
    private boolean mIsMLSLocationQueryRunning;

    public ObservationPoint(Location pointGPS) {
        this.pointGPS = pointGPS;
    }

    public ObservationPoint(String provider, Coordinate pointGPS, int wifis, int cells/*, long timestamp*/) {
        this.pointGPS = new Location(provider);
        this.pointGPS.setLatitude(pointGPS.getLatitude());
        this.pointGPS.setLongitude(pointGPS.getLongitude());
        mWifiCount = wifis;
        mCellCount = cells;
    }

    public void setMLSQuery(StumblerBundle stumbleBundle) {
        try {
            mMLSQuery = stumbleBundle.toMLSGeolocate();
            //Log.d(LOG_TAG, "PII geolocate: " + mMLSQuery.toString(2));

        } catch (JSONException e) {
            ClientLog.w(LOG_TAG, "Failed to convert bundle to JSON: " + e);
        }
    }

    public void setCounts(MLSJSONObject ichnaeaQueryObj) {
        mCellCount = ichnaeaQueryObj.getCellCount();
        mWifiCount = ichnaeaQueryObj.getWifiCount();
    }

    public void fetchMLS(boolean isWifiConnected, boolean isWifiAvailable) {
        if (mMLSQuery == null || pointMLS != null || mIsMLSLocationQueryRunning) {
            return;
        }
        ClientPrefs prefs = ClientPrefs.getInstanceWithoutContext();
        if (prefs == null || !isWifiConnected || (prefs.getUseWifiOnly() && !isWifiAvailable)) {
            return;
        }

        mIsMLSLocationQueryRunning = true;
        new AsyncGeolocate(this, mMLSQuery).execute();
    }

    public boolean needsToFetchMLS() {
        return pointMLS == null && mMLSQuery != null;
    }

    public void setMLSResponseLocation(Location location) {
        mIsMLSLocationQueryRunning = false;

        if (location != null) {
            mMLSQuery = null; // todo decide how to persist this to kml
            pointMLS = new Coordinate(location.getLongitude(), location.getLatitude(), location.getAltitude());
        }
    }

    public Coordinate getGPSCoordinate() {
        return new Coordinate(pointGPS.getLongitude(), pointGPS.getLatitude(), 0.0);
    }

    public Coordinate getMLSCoordinate() {
        return new Coordinate(pointMLS.getLongitude(), pointMLS.getLatitude(), 0.0);
    }

    public void setMLSCoordinate(Coordinate c) {
        pointMLS = c;
    }

    public void errorMLSResponse(boolean stopRequesting) {
        if (stopRequesting) {
            ClientLog.i(ObservationPoint.class.getSimpleName(), "Error:" + mMLSQuery.toString());
            mMLSQuery = null;
        }
        mIsMLSLocationQueryRunning = false;
    }
}
