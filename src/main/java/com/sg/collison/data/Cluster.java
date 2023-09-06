package com.sg.collison.data;

import java.util.*;
import java.util.stream.Collectors;

public class Cluster {
    private Long sumPointsId;

    public Long getSumPointsId() {
        return sumPointsId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cluster cluster = (Cluster) o;
        if (!sumPointsId.equals(cluster.sumPointsId)) {
            return false;
        }
        if (points.size() != cluster.points.size()) {
            return false;
        }
        if (points.stream().filter(point -> !cluster.points.contains(point)).findFirst().orElse(null) != null) {
            return false;
        }
        if (cluster.points.stream().filter(point -> !points.contains(point)).findFirst().orElse(null) != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return sumPointsId.hashCode();
    }

    private Set<Point> points;

    public Set<Point> getPoints() {
        return points;
    }

    private Set<Edge> edges;
    private double dx;

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    private double dy;

    public Cluster(Point initialPoint) {
        points = new HashSet<>();
        edges = new HashSet<>();
        points.add(initialPoint);
        initialPoint.setOwner(this);
        sumPointsId = initialPoint.getId();
        dx = initialPoint.getDx();
        dy = initialPoint.getDy();
    }

    public List<Cluster> splitOnExpiredEdges(Long actualTime) {
        List<Edge> expiredEdges = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.lifetime < actualTime) {
                expiredEdges.add(edge);
            }
        }
        for (Edge edge : expiredEdges) {
            edges.remove(edge);
        }
        return this.splitByDFSCluster();
    }

    public List<Cluster> splitByDFSCluster() {
        List<Cluster> clusters = new ArrayList<>();
        Set<Point> visited = new HashSet<>();

        for (Point point : points) {
            if (!visited.contains(point)) {
                Set<Point> newClusterPoints = new HashSet<>();
                dfs(point, newClusterPoints, visited);
                Cluster newCluster = new Cluster(point);
                for (Point newPoint : newClusterPoints) {
                    newCluster.addPoint(newPoint);
                }
                clusters.add(newCluster);
            }
        }
        return clusters;
    }

    private void dfs(Point current, Set<Point> newClusterPoints, Set<Point> visited) {
        visited.add(current);
        newClusterPoints.add(current);
        List<Edge> edgesWithPoint = edges.stream()
                .filter(edge -> edge.point1Id.equals(current.getId()) || edge.point2Id.equals(current.getId()))
                .collect(Collectors.toList());

        for (Edge edge : edgesWithPoint) {
            Long nextPointId = edge.point1Id.equals(current.getId()) ? edge.point2Id : edge.point1Id;
            Point nextPoint = points.stream().filter(p -> p.getId().equals(nextPointId)).findFirst().orElse(null);
            if (nextPoint != null && !visited.contains(nextPoint)) {
                dfs(nextPoint, newClusterPoints, visited);
            }
        }
    }


    public void addPoint(Point point) {
        Integer size = points.size();
        if (points.contains(point)) {
            return;
        }
        points.add(point);
        point.setOwner(this);
        sumPointsId += point.getId();
        dx = (dx * size + point.getDx()) / (size + 1);
        dy = (dy * size + point.getDy()) / (size + 1);
    }

    public void addEdge(Point point1, Point point2, Long lifetime) {
        edges.add(new Edge(point1, point2, lifetime));
    }

    // Dodaj pozostałe gettery i settery

    private class Edge {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return (Objects.equals(point1Id, edge.point1Id) && Objects.equals(point2Id, edge.point2Id)) ||  (Objects.equals(point1Id, edge.point2Id) && Objects.equals(point2Id, edge.point1Id));
        }

        @Override
        public int hashCode() {
            return Objects.hash(point1Id + point2Id);
        }

        private Long point1Id;
        private Long point2Id;
        private Long lifetime;

        public Edge(Point point1, Point point2, Long lifetime) {
            this.point1Id = point1.getId();
            this.point2Id = point2.getId();
            this.lifetime = lifetime;
        }

    }
}
