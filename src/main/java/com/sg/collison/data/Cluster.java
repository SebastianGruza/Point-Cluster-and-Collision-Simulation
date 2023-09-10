package com.sg.collison.data;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Cluster {
    private AtomicLong sumPointsId;

    public Long getSumPointsId() {
        return sumPointsId.get();
    }

    public void setSumPointsId(Long sumPointsId) {
        this.sumPointsId.set(sumPointsId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cluster cluster = (Cluster) o;
        if (!this.getSumPointsId().equals(cluster.getSumPointsId())) {
            return false;
        }
        if (points.size() != cluster.points.size()) {
            return false;
        }
        if (edges.size() != cluster.edges.size()) {
            return false;
        }
        if (cluster.points.size() > 0 && !points.contains(cluster.points.iterator().next())) {
            return false;
        }
        if (points.size() > 0 && !cluster.points.contains(points.iterator().next())) {
            return false;
        }
        if (Math.abs(cluster.getDx() - getDx()) > 0.00000001
                || Math.abs(cluster.getDy() - getDy()) > 0.00000001) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return this.getSumPointsId().hashCode();
    }

    private Set<Point> points;

    public Set<Point> getPoints() {
        return points;
    }

    private Set<Edge> edges;

    public Set<Edge> getEdges() {
        return edges;
    }

    public void setDx(double dx) {
        this.dx = dx;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }

    private double dx;

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    private double dy;

    public Cluster(Point initialPoint) {
        points = Collections.synchronizedSet(new HashSet<>());
        edges = new HashSet<>();
        points.add(initialPoint);
        initialPoint.setOwner(this);
        sumPointsId = new AtomicLong(0);
        sumPointsId.addAndGet(initialPoint.getId());
        dx = initialPoint.getDx();
        dy = initialPoint.getDy();
    }

    public Set<Cluster> splitOnExpiredEdges(Long actualTime) {
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

    public Set<Cluster> splitByDFSCluster() {
        Set<Cluster> clusters = new HashSet<>();
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
        if (points.contains(point)) {
            return;
        }
        point.setOwner(this);
        sumPointsId.addAndGet(point.getId());
        Integer size = points.size();
        dx = (dx * size + point.getDx()) / (size + 1);
        dy = (dy * size + point.getDy()) / (size + 1);
        points.add(point);
    }

    public void computeMST() {
        List<Edge> sortedEdges = new ArrayList<>(edges);
        sortedEdges.sort(Comparator.comparingDouble(Edge::getWeight));

        Set<Long> uniqueIds = points.stream().map(Point::getId).collect(Collectors.toSet());
        UnionFind unionFind = new UnionFind(uniqueIds);

        Set<Edge> mstEdges = new HashSet<>();

        for (Edge edge : sortedEdges) {
            Long p1 = edge.getPoint1Id();
            Long p2 = edge.getPoint2Id();

            if (unionFind.union(p1, p2)) {
                mstEdges.add(edge);
            }
        }

        this.edges = mstEdges; // Ustawiamy krawędzie klastra na te z MST
    }


    class UnionFind {

        private Map<Long, Integer> idToIndexMap = new HashMap<>();
        private int nextIndex = 0;

        private int[] parent;
        private int[] rank;

        public UnionFind(Set<Long> uniqueIds) {
            int size = uniqueIds.size();
            parent = new int[size];
            rank = new int[size];

            int index = 0;
            for (long id : uniqueIds) {
                idToIndexMap.put(id, index);
                parent[index] = index;
                rank[index] = 1;
                index++;
            }
        }

        public int find(long id) {
            int index = getIndex(id);
            while (index != parent[index]) {
                index = parent[index];
            }

            // Optymalizacja ścieżki (path compression)
            int root = index;
            index = getIndex(id);
            while (index != root) {
                int next = parent[index];
                parent[index] = root;
                index = next;
            }

            return root;
        }

        public boolean union(long x, long y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
                return true;
            }
            return false;
        }

        private int getIndex(long id) {
            if (!idToIndexMap.containsKey(id)) {
                idToIndexMap.put(id, nextIndex);
                nextIndex++;
            }
            return idToIndexMap.get(id);
        }
    }


    public static class Edge implements Comparable<Edge> {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge edge = (Edge) o;
            return (Objects.equals(point1Id, edge.point1Id) && Objects.equals(point2Id, edge.point2Id)) || (Objects.equals(point1Id, edge.point2Id) && Objects.equals(point2Id, edge.point1Id));
        }

        @Override
        public int hashCode() {
            return Objects.hash(point1Id + point2Id);
        }

        public Long getPoint1Id() {
            return point1Id;
        }

        public Long getPoint2Id() {
            return point2Id;
        }

        public Double getLifetime() {
            return lifetime;
        }

        public Double getWeight() {
            return 1000.0 / lifetime;
        }

        @Override
        public int compareTo(Edge other) {
            if (this.equals(other)) {
                return 0;
            } else {
                return Double.compare(this.getWeight(), other.getWeight());
            }
        }

        private Long point1Id;
        private Long point2Id;
        private double lifetime;

        public Edge(Point point1, Point point2, double lifetime) {
            this.point1Id = point1.getId();
            this.point2Id = point2.getId();
            this.lifetime = lifetime;
        }

    }
}
