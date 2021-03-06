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
package io.github.data4all.util;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import io.github.data4all.model.DeviceOrientation;
import io.github.data4all.model.data.Node;
import io.github.data4all.model.data.TransformationParamBean;
import io.github.data4all.model.drawing.Point;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.location.Location;

/**
 * Test class for the class PointToCoordsTransformationUtil.
 * 
 * @author burghardt
 * @author sbollen
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class PointToCoordsTransformUtilTest {

    PointToCoordsTransformUtil util;
    TransformationParamBean tps;
    DeviceOrientation deviceOrientation;
    Location location;

    @Before
    public void setUp() {
        location = new Location("Provider");
        // height is 1.7, photoWidth is 500 and photoHeight is 1000
        tps = new TransformationParamBean(1.7, Math.toRadians(90),
                Math.toRadians(90), 500, 1000, location);
        util = new PointToCoordsTransformUtil(tps, deviceOrientation);
    }

    // Tests for method transform(TransformationParamBean tps,
    // DeviceOrientation deviceOrientation, List<Point> points,
    // int rotation)

    /**
     * Different transform Tests
     */
    @Test
    public void transformTest() {
        location.setLatitude(0.0);
        location.setLongitude(0.0);
        TransformationParamBean tps = new TransformationParamBean(2.0,
                Math.toRadians(90), Math.toRadians(90), 1000, 1000, location);
        List<Node> test;
        DeviceOrientation deviceOrientation = new DeviceOrientation(0.0f, 0.0f,
                0.0f, 10L);
        ArrayList<Point> points = new ArrayList<Point>();
        points.add(new Point(500, 500));
        points.add(new Point(1000, 500));
        points.add(new Point(500, 1000));
        points.add(new Point(1, 500));
        points.add(new Point(500, 1));
        test = util.transform(tps, deviceOrientation, points);
        assertThat(test.get(0).getLat(), is(0.0));
        assertThat(test.get(0).getLon(), is(0.0));
        assertThat(test.get(1).getLat(), is(0.0));
        assertThat(test.get(1).getLon(), greaterThan(0.0));
        assertThat(test.get(2).getLat(), lessThan(0.0));
        assertThat(test.get(2).getLon(), is(0.0));
        assertThat(test.get(3).getLat(), is(0.0));
        assertThat(test.get(3).getLon(), lessThan(0.0));
        assertThat(test.get(4).getLat(), greaterThan(0.0));
        assertThat(test.get(4).getLon(), is(0.0));

        deviceOrientation = new DeviceOrientation((float) (Math.PI / 2), 0.0f,
                0.0f, 10L);
        test = util.transform(tps, deviceOrientation, points);
        assertThat(test.get(0).getLat(), is(0.0));
        assertThat(test.get(0).getLon(), is(0.0));
        assertThat(test.get(1).getLat(), lessThan(0.0));
        assertThat(test.get(1).getLon(), closeTo(0.0, 0.00000000001));
        assertThat(test.get(2).getLat(), closeTo(0.0, 0.00000000001));
        assertThat(test.get(2).getLon(), lessThan(0.0));

        deviceOrientation = new DeviceOrientation((float) Math.toRadians(-45),
                (float) Math.toRadians(45), (float) Math.toRadians(45), 10L);
        ArrayList<Point> points2 = new ArrayList<Point>();
        points2.add(new Point(500, 500));
        test = util.transform(tps, deviceOrientation, points2);
        assertThat(test.get(0).getLon(), lessThan(0.0));
        assertThat(test.get(0).getLat(), lessThan(0.0));
    }

    // Tests for method calculateCoordFromPoint(TransformationParamBean tps,
    // DeviceOrientation deviceOrientation, Point point)

    /**
     * a few different Pixel
     */
    @Test
    public void calculateCoordFromPointTest_difPixel() {
        TransformationParamBean tps1 = new TransformationParamBean(2.0,
                Math.toRadians(90), Math.toRadians(90), 1000, 1000, location);
        DeviceOrientation deviceOrientation = new DeviceOrientation(0.0f, 0.0f,
                0.0f, 10L);
        double[] coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(500, 500));
        assertThat(coord[0], is(0.0));
        assertThat(coord[1], is(0.0));
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(1000, 500));
        assertThat(coord[0], closeTo(2.0, 0.00000001));
        assertThat(coord[1], is(0.0));
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(500, 1000));
        assertThat(coord[0], is(0.0));
        assertThat(coord[1], closeTo(-2.0, 0.00000001));
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(0, 500));
        assertThat(coord[0], closeTo(-2.0, 0.00001));
        assertThat(coord[1], is(0.0));
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(500, 0));
        assertThat(coord[0], is(0.0));
        assertThat(coord[1], closeTo(2.0, 0.00001));
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(1000, 1000));
        assertThat(coord[0], closeTo(2.0, 0.00001));
        assertThat(coord[1], closeTo(-2.0, 0.00001));
    }

    /**
     * a few different Orientations
     */
    @Test
    public void calculateCoordFromPointTest_difOrientation() {
        TransformationParamBean tps1 = new TransformationParamBean(2.0,
                Math.toRadians(90), Math.toRadians(90), 1000, 1000, location);
        DeviceOrientation deviceOrientation = new DeviceOrientation(0.0f, 0.0f,
                0.0f, 10L);
        double[] coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(500, 500));
        assertThat(coord[0], is(0.0));
        assertThat(coord[1], is(0.0));
        deviceOrientation = new DeviceOrientation((float) Math.toRadians(123),
                0.0f, 0.0f, 10L);
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(500, 500));
        assertThat(coord[0], closeTo(0, 0.000001));
        assertThat(coord[1], closeTo(0, 0.000001));

        deviceOrientation = new DeviceOrientation((float) Math.toRadians(90),
                (float) Math.toRadians(45), 0.0f, 10L);
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(500, 500));
        assertThat(coord[0], closeTo(-2.0, 0.00001));
        assertThat(coord[1], closeTo(0.0, 0.00001));

        deviceOrientation = new DeviceOrientation((float) Math.toRadians(90),
                0.0f, (float) Math.toRadians(45), 10L);
        coord = util.calculateCoordFromPoint(tps1, deviceOrientation,
                new Point(500, 500));
        assertThat(coord[0], closeTo(0.0, 0.00001));
        assertThat(coord[1], closeTo(2.0, 0.00001));
    }

    // Tests for method calculateGPSPoint(Location location, double[] coord)

    /**
     * Coords are 0
     */
    @Test
    public void calculateGPSPointTest_CoordsAre0() {
        location.setLatitude(0);
        location.setLongitude(0);
        double[] coord = { 0, 0 };
        Node node = MathUtil.calculateGPSPoint(location, coord);
        assertThat(node.getLat(), is(location.getLatitude()));
        assertThat(node.getLon(), is(location.getLongitude()));
    }

    /**
     * Coords are greater than 0
     */
    @Test
    public void calculateGPSPointTest_CoordsAreGreater0() {
        location.setLatitude(0);
        location.setLongitude(0);
        double[] coord = { 10, 10 };
        Node node = MathUtil.calculateGPSPoint(location, coord);
        assertThat(node.getLat(), greaterThan(location.getLatitude()));
        assertThat(node.getLon(), greaterThan(location.getLongitude()));
    }

    /**
     * Coords below 0
     */
    @Test
    public void calculateGPSPointTest_CoordsAreBelow0() {
        location.setLatitude(0);
        location.setLongitude(0);
        double[] coord = { -10, -10 };
        Node node = MathUtil.calculateGPSPoint(location, coord);
        assertThat(node.getLat(), lessThan(location.getLatitude()));
        assertThat(node.getLon(), lessThan(location.getLongitude()));
    }

    /**
     * Longitude jump over 180° and -180°
     */
    @Test
    public void calculateGPSPointTest_LongitudeJump() {
        location.setLatitude(0);
        location.setLongitude(179.9999999);
        double[] coord = { 100000, 100000 };
        Node node = MathUtil.calculateGPSPoint(location, coord);
        assertThat(node.getLon(), lessThan(-100.0));
        location.setLatitude(0);
        location.setLongitude(-179.9999999);
        double[] coord2 = { -100000, -100000 };
        Node node2 = MathUtil.calculateGPSPoint(location, coord2);
        assertThat(node2.getLon(), greaterThan(100.0));
    }
    
    
    // Tests for method fourthBuildingPoint
    /**
     * a few different Orientations
     */
    @Test
    public void fourthBuildingPointTest() {
        TransformationParamBean tps1 = new TransformationParamBean(2.0,
                Math.toRadians(90), Math.toRadians(90), 1000, 1000, location);
        DeviceOrientation deviceOrientation = new DeviceOrientation(0.0f, (float) Math.toRadians(0),
                0.0f, 10L);
        ArrayList<Point> points = new ArrayList<Point>();
        points.add(new Point(0, 0));
        points.add(new Point(1000,0));
        points.add(new Point(1000, 1000));
        Point point = util.fourthBuildingPoint(tps1, deviceOrientation, points);
        assertThat((double) point.getX(), closeTo(0.0f, 2.0f));
        assertThat((double) point.getY(), closeTo(1000.0f, 2.0f));
    }

}