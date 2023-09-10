package com.sg.collison.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Point {

    private Long id;
    private double globalX;
    private double globalY;
    private double clusterX;
    private double clusterY;
    private double dx;
    private double dy;

    private double clusterDx;
    private double clusterDy;

    @JsonIgnore
    private Cluster owner = null;

    public Cluster getOwner() {
        return owner;
    }

    public void setOwner(Cluster owner) {
        this.owner = owner;
    }

    public Point(Long id, double globalX, double globalY, double clusterX, double clusterY, double dx, double dy) {
        this.id = id;
        this.globalX = globalX;
        this.globalY = globalY;
        this.clusterX = clusterX;
        this.clusterY = clusterY;
        this.dx = dx;
        this.dy = dy;
    }

    public void move(double width, double height) {
        this.globalX += this.dx;
        this.globalY += this.dy;
        if (this.globalX < 0) {
            this.globalX = width + this.globalX;
        }
        if (this.globalX > width) {
            this.globalX = this.globalX - width;
        }
        if (this.globalY < 0) {
            this.globalY = height + this.globalY;
        }
        if (this.globalY > height) {
            this.globalY = this.globalY - height;
        }
    }

    public Point(Long id) {
        this.id = id;
    }

    public Point(Long id, double globalX, double globalY) {
        this.id = id;
        this.globalX = globalX;
        this.globalY = globalY;
    }

    public boolean isColliding(Point point, double width, double height, double radius) {
        double dx = this.globalX - point.globalX;
        double absDx = Math.abs(dx);

        double minX1 = Math.min(width - this.globalX, this.globalX);
        double minX2 = Math.min(width - point.globalX, point.globalX);
        double dx_w = minX1 + minX2;

        double distDx = Math.min(absDx, dx_w);
        if (distDx > radius) {
            return false;
        }

        double dy = this.globalY - point.globalY;
        double absDy = Math.abs(dy);

        double minY1 = Math.min(height - this.globalY, this.globalY);
        double minY2 = Math.min(height - point.globalY, point.globalY);
        double dy_h = minY1 + minY2;

        double distDy = Math.min(absDy, dy_h);
        if (distDy > radius) {
            return false;
        }

        return (distDx * distDx + distDy * distDy) <= radius * radius;
    }

    public Long getId() {
        return id;
    }

    public double getGlobalX() {
        return globalX;
    }

    public void setGlobalX(double globalX) {
        this.globalX = globalX;
    }

    public double getGlobalY() {
        return globalY;
    }

    public void setGlobalY(double globalY) {
        this.globalY = globalY;
    }

    public double getClusterX() {
        return clusterX;
    }

    public void setClusterX(double clusterX) {
        this.clusterX = clusterX;
    }

    public double getClusterY() {
        return clusterY;
    }

    public void setClusterY(double clusterY) {
        this.clusterY = clusterY;
    }

    public double getDx() {
        return dx;
    }

    public void setDx(double dx) {
        this.dx = dx;
    }

    public double getDy() {
        return dy;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }

    public double getClusterDx() {
        return clusterDx;
    }

    public void setClusterDx(double clusterDx) {
        this.clusterDx = clusterDx;
    }

    public double getClusterDy() {
        return clusterDy;
    }

    public void setClusterDy(double clusterDy) {
        this.clusterDy = clusterDy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Objects.equals(id, point.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

