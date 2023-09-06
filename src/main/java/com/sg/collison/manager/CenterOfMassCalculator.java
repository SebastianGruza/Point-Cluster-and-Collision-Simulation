package com.sg.collison.manager;

import com.sg.collison.data.Point;

import java.util.ArrayList;
import java.util.List;


public class CenterOfMassCalculator {
    public static List<Point> calculateCenterOfMass(List<Point> points, double width, double height) {
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

        double centerX = ((avgAngleX / (2 * Math.PI)) * width + width) % width;
        double centerY = ((avgAngleY / (2 * Math.PI)) * height + height) % height;

        List<Point> ret = new ArrayList<>(points.size());
        System.out.println("Center of mass: " + centerX + ", " + centerY);
        for (Point p : points) {
            double x = (p.getGlobalX() - centerX + 3 * width / 2) % width;
            double y = (p.getGlobalY() - centerY + 3 * height / 2) % height;
            double clusterX = (p.getClusterX() - centerX + 3 * width / 2) % width;
            double clusterY = (p.getClusterY() - centerY + 3 * height / 2) % height;

            Point newPoint = new Point(p.getId(), x, y, clusterX, clusterY, p.getDx(), p.getDy());
            ret.add(newPoint);
        }

        return ret;
    }


}
