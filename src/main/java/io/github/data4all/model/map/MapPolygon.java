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
package io.github.data4all.model.map;

import java.util.ArrayList;
import java.util.List;

import io.github.data4all.activity.AbstractActivity;
import io.github.data4all.activity.MapViewActivity;
import io.github.data4all.logger.Log;
import io.github.data4all.model.data.AbstractDataElement;
import io.github.data4all.model.data.ClassifiedTag;
import io.github.data4all.model.data.PolyElement;
import io.github.data4all.model.data.Tag;
import io.github.data4all.util.MapUtil;
import io.github.data4all.view.D4AMapView;

import org.osmdroid.bonuspack.overlays.Polygon;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.view.MotionEvent;

/**
 * Polygon which is editable. It has an InfoWindow opened with a single tap, if
 * it is not editable and it is movable and rotatable with a TouchEvent, if it
 * is editable.
 * 
 * @author Oliver Schwartz
 * @author sbollen
 *
 */
public class MapPolygon extends Polygon {

    private static final String TAG = "MapPolygon";
    private AbstractActivity activity;
    private D4AMapView mapView;
    private AbstractDataElement element;
    private boolean editable;
    // checks, that the length of the offset vectors is be calculated only once
    private boolean lengthSet = true;

    // midpoint of the bounding box of the polygon
    private Point midpoint;

    // start time for touch event action_down
    private long timeStart;

    // True when the edit mode is active
    private boolean active = false;

    // the maximum time difference between action_down and action_up, so that
    // the mode will be changed
    private static final int TIME_DIFF = 200;

    // Default Stroke Color
    protected static final int DEFAULT_STROKE_COLOR = Color.BLUE;
    // Active Stroke Color
    protected static final int ACTIVE_STROKE_COLOR = Color.GREEN;

    // Fill Color for Polygons
    protected static final int DEFAULT_FILL_COLOR = Color.argb(100, 0, 0, 255);
    // Fill Color for activated Polygons
    protected static final int ACTIVE_FILL_COLOR = Color.argb(100, 50, 255, 50);

    /**
     * Modes for edits which differ from touch events.
     */
    private static final int NONE = 0;
    private static final int MOVE = 1;
    private static final int ROTATE = 2;
    private int mode = NONE;

    /**
     * Start values for rotation.
     */
    private int xStartPo1 = 0;
    private int yStartPo1 = 0;
    private int xStartPo2 = 0;
    private int yStartPo2 = 0;

    /**
     * Start values for moving.
     */
    private int xStartM = 0;
    private int yStartM = 0;

    /**
     * List of GeoPoints of the MapPolygon before it was edited.
     */
    private List<GeoPoint> originalPoints;

    /**
     * List of vectors from the midpoint of the MapPolygon to every point.
     */
    private List<Point> pointsOffset;

    /**
     * List of the length of all vectors from the midpoint to every point.
     */
    private List<Double> pOffsetLength;

    /**
     * List of GeoPoints for editing the MapPolygon.
     */
    private List<GeoPoint> geoPointList;

    /**
     * Projection of the mapView.
     */
    private Projection pj;

    /**
     * Default constructor.
     * 
     * @param ctx
     *            the Context for the Overlay
     * 
     * @param mv
     *            the Mapview
     * 
     * @param ele
     *            the associated OsmElement
     */
    public MapPolygon(AbstractActivity ctx, D4AMapView mv,
            AbstractDataElement ele) {
        super(ctx);
        this.element = ele;
        this.activity = ctx;
        this.mapView = mv;
        this.editable = false;
        if (activity instanceof MapViewActivity) {
            mInfoWindow = new CustomInfoWindow(this.mapView, ele, this,
                    activity);
        } else {
            mInfoWindow = null;
        }
        setInfo();
    }

    /**
     * Set the info of the MapPolygon for the InfoWindow.
     */
    public void setInfo() {
        if (!element.getTags().keySet().isEmpty()
                && !element.getTags().values().isEmpty()) {
            Log.i(TAG, element.getTags().toString());
            Tag tag = (Tag) element.getTags().keySet().toArray()[0];
            String key = tag.getKey();
            String value = element.getTags().get(tag);
            Log.i(TAG, tag.toString());
            setTitle(activity.getString(tag.getNameRessource()));
            if (tag instanceof ClassifiedTag) {
                setSubDescription(getLocalizedName(activity, key, value));
            } else {
                setSubDescription(element.getTags().get(tag));
            }
        }
    }

    /**
     * Get the localized name of the element to show in the InfoWindow.
     * 
     * @param context
     *            the context of the application
     * @param key
     *            the tag key
     * @param value
     *            the tag value
     * @return the localized name
     */
    public String getLocalizedName(Context context, String key, String value) {
        Resources resources = context.getResources();
        String s = "name_" + key + "_" + value;
        int id = resources.getIdentifier(s.replace(":", "_"), "string",
                context.getPackageName());
        if (id == 0) {
            return null;
        } else {
            return resources.getString(id);
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
        super.onTouchEvent(event, mapView);

        if (editable) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                pj = mapView.getProjection();
                timeStart = System.currentTimeMillis();
                if (active) {
                    mode = MOVE;
                    // actual polygon point list
                    geoPointList = this.getPoints();
                    // get the offset of all points in the list to the first one
                    if (pointsOffset == null) {
                        pointsOffset = getOffset();
                    }
                    xStartM = (int) event.getX();
                    yStartM = (int) event.getY();
                    Log.d(TAG, "action_down at point: " + xStartM + " "
                            + yStartM);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d(TAG, "more than one pointer on screen");
                if (active) {
                    mode = ROTATE;
                    // set the start values for the rotation
                    xStartPo1 = (int) event.getX(0);
                    xStartPo2 = (int) event.getX(1);
                    yStartPo1 = (int) event.getY(0);
                    yStartPo2 = (int) event.getY(1);
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "action_up");
                if (active) {
                    // set the new information to the element
                    ((PolyElement) element).setNodesFromGeoPoints(geoPointList);
                }
                if (Math.abs(timeStart - System.currentTimeMillis()) < TIME_DIFF
                        && contains(event)) {
                    changeMode();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "action_move");
                if (active) {
                    if (mode == MOVE) {
                        Log.d(TAG, "move polygon");
                        moveToNewPos(event, mapView);
                    } else if (mode == ROTATE) {
                        Log.d(TAG, "rotate polygon");
                        rotatePolygon(event);
                    }
                }
                break;
            default:
                Log.d(TAG, "detected another touch event");
            }
            return active;
        } else {
            return super.onTouchEvent(event, mapView);
        }
    }

    /**
     * change the mode whether the edit function is active or not.
     */
    public void changeMode() {
        if (!active) {
            // change mode to active, polygon is now rotatable and movable
            this.setFillColor(ACTIVE_FILL_COLOR);
            this.setStrokeColor(ACTIVE_STROKE_COLOR);
            pj = mapView.getProjection();
            midpoint = pj.toPixels(MapUtil.getCenterFromOsmElement(element),
                    null);
            setOriginalPoints();
            lengthSet = true;
            pointsOffset = getOffset();
            mapView.invalidate();
            active = true;
        } else {
            // change mode to not active, polygon is not modifiable now
            this.setFillColor(CustomInfoWindow.MARKED_FILL_COLOR);
            this.setStrokeColor(CustomInfoWindow.MARKED_STROKE_COLOR);
            mapView.invalidate();
            active = false;
        }
        Log.d(TAG, "actual activity mode: " + active);
    }

    /**
     * Rotate the polygon handling the touch events.
     *
     * @param event
     *            the actual touch event
     */
    private void rotatePolygon(MotionEvent event) {
        // set end values for the next rotation action
        int xEndPo1 = (int) event.getX(0);
        int xEndPo2 = (int) event.getX(1);

        int yEndPo1 = (int) event.getY(0);
        int yEndPo2 = (int) event.getY(1);

        // get the rotation angle
        double delta_xEnd = (xEndPo1 - xEndPo2);
        double delta_yEnd = (yEndPo1 - yEndPo2);
        double radians1 = Math.atan2(delta_yEnd, delta_xEnd);

        double delta_xStart = (xStartPo1 - xStartPo2);
        double delta_yStart = (yStartPo1 - yStartPo2);
        double radians2 = Math.atan2(delta_yStart, delta_xStart);

        double radians = radians1 - radians2;

        if ((Math.abs(xStartPo1 - xEndPo1) > 0 && Math.abs(yStartPo1 - yEndPo1) > 0)
                || (Math.abs(xStartPo2 - xEndPo2) > 0 && Math.abs(yStartPo2
                        - yEndPo2) > 0)) {
            if (Math.abs(radians) > 0.0) {
                // get the offset of all points in the list to the first one
                if (pointsOffset.size() < geoPointList.size()) {
                    pointsOffset = getOffset();
                }
                Log.d(TAG, "Rotation in radians: " + radians);
                // Get the sin and cos of the rotation angle
                float sin = (float) Math.sin(radians);
                float cos = (float) Math.cos(radians);

                // set all rotated points of the polygon
                for (int i = 0; i < geoPointList.size(); i++) {
                    Point newPoint = new Point();
                    Point offset = pointsOffset.get(i);
                    // calculate new offset with rotation angle
                    double xOffset = offset.x * cos - offset.y * sin;
                    double yOffset = offset.x * sin + offset.y * cos;
                    double newOffsetLength = Math.sqrt((xOffset * xOffset)
                            + (yOffset * yOffset));
                    double offsetLength = pOffsetLength.get(i);
                    xOffset = xOffset * (offsetLength / newOffsetLength);
                    yOffset = yOffset * (offsetLength / newOffsetLength);
                    offset.set((int) xOffset, (int) yOffset);

                    newPoint.set((midpoint.x + (int) xOffset),
                            (midpoint.y + (int) yOffset));
                    geoPointList.set(i, (GeoPoint) pj.fromPixels(
                            (int) newPoint.x, (int) newPoint.y));
                }
            }
        }
        // set new start values for the next rotation action
        xStartPo1 = (int) event.getX(0);
        xStartPo2 = (int) event.getX(1);

        yStartPo1 = (int) event.getY(0);
        yStartPo2 = (int) event.getY(1);

        // set the list with the changed points
        this.setPoints(geoPointList);
        mapView.invalidate();
    }

    /**
     * Move this polygon to the new position handling the touch events. Move the
     * midpoint of the bounding box of the polygon and after that add the offset
     * of all points of the polygon to the new midpoint.
     * 
     * @param event
     *            the current MotionEvent from onTouchEvent
     * @param mapView
     *            the current mapView
     */
    public void moveToNewPos(final MotionEvent event, final MapView mapView) {
        // set the end coordinates of the movement
        int xEnd = (int) event.getX();
        int yEnd = (int) event.getY();

        if (pointsOffset == null) {
            pointsOffset = getOffset();
        }
        // only move the polygon if there is a movement
        if (Math.abs(xEnd - xStartM) > 0 && Math.abs(yEnd - yStartM) > 0) {
            Log.i(TAG, "moveMapPolygon from: " + xStartM + " " + yStartM);
            Log.i(TAG, "moveMapPolygon to: " + xEnd + " " + yEnd);
            // move the midpoint
            midpoint.set((midpoint.x + (xEnd - xStartM)),
                    (midpoint.y + (yEnd - yStartM)));

            // set all other points depending on the midpoint
            for (int i = 0; i < geoPointList.size(); i++) {
                Point newPoint = new Point();
                newPoint.set((midpoint.x + pointsOffset.get(i).x),
                        (midpoint.y + pointsOffset.get(i).y));
                geoPointList.set(i, (GeoPoint) pj.fromPixels((int) newPoint.x,
                        (int) newPoint.y));
            }
            // set new start values for the next move action
            xStartM = (int) event.getX();
            yStartM = (int) event.getY();

            // set the list with the changed points
            this.setPoints(geoPointList);
            mapView.invalidate();
        }
    }

    /**
     * Get the vectors to all points of the polygon starting from the midpoint.
     * Necessary for moving the polygon.
     * 
     * @return List with all vectors
     */
    public List<Point> getOffset() {
        Log.i(TAG, "number of points in the polygon: " + originalPoints.size());
        List<Point> pointsOffset = new ArrayList<Point>();
        if (lengthSet) {
            pOffsetLength = new ArrayList<Double>();
        }
        if (originalPoints.size() > 0) {
            for (int i = 0; i < originalPoints.size(); i++) {
                Point point = pj.toPixels(originalPoints.get(i), null);
                int xOffset = (point.x - midpoint.x);
                int yOffset = (point.y - midpoint.y);
                // get the length of the vector from the midpoint to the point
                if (lengthSet) {
                    double offsetLength = Math.sqrt((xOffset * xOffset)
                            + (yOffset * yOffset));
                    pOffsetLength.add(offsetLength);
                }
                pointsOffset.add(new Point(xOffset, yOffset));
            }
            lengthSet = false;
        }
        return pointsOffset;
    }

    /**
     * Set the original points of the polygon. Called when the polygon is added
     * to the map.
     */
    public void setOriginalPoints() {
        this.originalPoints = this.getPoints();
    }

    /**
     * Set whether the polygon is editable.
     * 
     * @param editable
     *            true if polygon is editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
