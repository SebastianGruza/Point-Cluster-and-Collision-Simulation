package com.sg.collison.manager;

import com.sg.collison.data.Point;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashSet;
import java.util.Set;


public class CenterOfMassCalculator {
    public static Point calculateCenterOfMass(Set<Point> points, double width, double height) {
        double sumSinX = 0.0, sumCosX = 0.0;
        double sumSinY = 0.0, sumCosY = 0.0;

        for (Point p : points) {
            double angleX = 2 * Math.PI * p.getGlobalX() / width;
            double angleY = 2 * Math.PI * p.getGlobalY() / height;

            sumSinX += Math.sin(angleX);
            sumCosX += Math.cos(angleX);

            sumSinY += Math.sin(angleY);
            sumCosY += Math.cos(angleY);
        }

        double avgAngleX = Math.atan2(sumSinX, sumCosX);
        double avgAngleY = Math.atan2(sumSinY, sumCosY);

        double centerX = modulo(((avgAngleX / (2 * Math.PI)) * width + width), width);
        double centerY = modulo(((avgAngleY / (2 * Math.PI)) * height + height), height);
        return new Point(0L, centerX, centerY, centerX, centerY, 0.0, 0.0);
    }

    public static ImmutablePair<Set<Point>, Point> movePointsToCenterOfMass(Set<Point> points, double width, double height) {
        Point center = calculateCenterOfMass(points, width, height);
        double centerX = center.getGlobalX();
        double centerY = center.getGlobalY();
        Set<Point> ret = new HashSet<>(points.size());
        for (Point p : points) {
            double x = (p.getGlobalX() - centerX + 3 * width / 2) % width;
            double y = (p.getGlobalY() - centerY + 3 * height / 2) % height;
            double clusterX = (p.getClusterX() - centerX + 3 * width / 2) % width;
            double clusterY = (p.getClusterY() - centerY + 3 * height / 2) % height;

            Point newPoint = new Point(p.getId(), x, y, clusterX, clusterY, p.getDx(), p.getDy());
            ret.add(newPoint);
        }

        return new ImmutablePair<>(ret, center);
    }

    public static double modulo(double x, double y) {
        if (y == 0.0) {
            throw new ArithmeticException("Modulo by zero");
        }
        double result = x - y * Math.floor(x / y);
        return result;
    }


}
