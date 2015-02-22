/*
 * Copyright (c) 2014, 2015 Data4All
 * 
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 * 
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.data4all.model.data;

import io.github.data4all.logger.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A track is the represenation of a .gpx file. It has a name and a list of
 * trackpoints.
 * 
 * A track is initialized when the GPSservice starts. There should be only one
 * track for a whole GPSservice lifecycle. GPSservice.onCreate() starts a new
 * track. GPSservice.onDestroy() should start the parsing in to a file. This
 * object is parsed to a .gpx file
 * 
 * @author sbrede
 * 
 */
@SuppressLint("SimpleDateFormat")
public class Track implements Parcelable {

    /**
     * Logger Tag.
     */
    private static final String TAG = "Track";

    /**
     * trackName is a Timestamp of format "yyyy_MM_dd_HH_mm_ss".
     */
    private String trackName;

    /**
     * saves a list of TrackPoints.
     */
    private final List<TrackPoint> tracklist;

    /**
     * Methods to write and restore a Parcel.
     */
    public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {

        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

    /**
     * Constructor to create a Track from a parcel.
     * 
     * @param in
     *            the parcel to read from
     */
    private Track(Parcel in) {
        tracklist = new ArrayList<TrackPoint>();
        in.readTypedList(tracklist, TrackPoint.CREATOR);
        trackName = in.readString();
    }

    /**
     * Constructor to create a Track. Name of the track will be the creation
     * time. "yyyy_MM_dd_HH_mm_ss"
     */
    public Track() {
        this.tracklist = new ArrayList<TrackPoint>();
        this.trackName = this.getTimeStamp();
        Log.d(TAG, "New Track with name: " + trackName + " created.");
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String timestamp) {
        this.trackName = timestamp;
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
    }

    /**
     * Adds a TrackPoint to the ArrayList of TrackPoints.
     * 
     * @param location
     *            The Location
     */
    public void addTrackPoint(final Location location) {
        if (location != null) {
            tracklist.add(new TrackPoint(location));
            Log.d(TAG, "Added TrackPoint: " + location.toString());
        }
    }

    public List<TrackPoint> getTrackPoints() {
        return new ArrayList<TrackPoint>(tracklist);
    }

    /**
     * Clears the list of TrackPoints belonging to this track and appends
     * another list of TrackPoints to it.
     * 
     * @param trackPoints the given list of TrackPoints
     */
    public void setTrackPoints(List<TrackPoint> trackPoints) {
        tracklist.clear();
        tracklist.addAll(trackPoints);
    }

    /**
     * Returns the latest TrakPoint from this track.
     * 
     * @return trackpoint The latest TrackPoint in the list
     */
    public TrackPoint getLastTrackPoint() {
        if (tracklist.isEmpty()) {
            return null;
        }
        return tracklist.get(tracklist.size() - 1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(trackName + '\n');
        for (TrackPoint loc : tracklist) {
            sb.append(loc.toString() + '\n');
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.Parcelable#describeContents()
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the tracklist and the trackname a parcel.
     * 
     * @param dest
     *            destination parcel
     * @param flags
     *            additional flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(tracklist);
        dest.writeString(trackName);
    }
}