package com.sg.collison.manager;

import com.sg.collison.data.Cluster;
import com.sg.collison.data.Point;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ClusterManager {
    private Set<Cluster> clusters;

    private final double radius = 2.5;

    public List<Point> allPoints = null;

    public Integer numberOfPoints = 50000;
    private Long actualTime = 0L;

    private double width;

    private double height;

    private Long clusterLifetime = 20L;

    private Thread previousThread = null;

    public ClusterManager() {
        clusters = Collections.synchronizedSet(new HashSet<>());

        width = 1080;
        height = 1080;
    }

    public void mainLoop() {
        initializeRandomPointsWithRandomCoordinateWithClusters();
        Integer frame = 0;
        while (true) {
            allProcessingOneStep(frame++);
//            try {
//                Thread.sleep(2);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }
    }


    public void initializeRandomPointsWithRandomCoordinateWithClusters() {
        //this.numberOfPoints = numberOfPoints;
        clusters = Collections.synchronizedSet(new HashSet<>());
        for (int i = 0; i < numberOfPoints; i++) {
            Point point = new Point((long) i);
            point.setGlobalX(Math.random() * width);
            point.setGlobalY(Math.random() * height);
            point.setDx(Math.random() * 8 - 4);
            point.setDy(Math.random() * 8 - 4);
            point.setClusterDx(point.getDx());
            point.setClusterDy(point.getDy());
            Cluster cluster = new Cluster(point);
            cluster.addPoint(point);
            clusters.add(cluster);
        }
    }

    public void allProcessingOneStep(Integer frame) {

        if (actualTime % 100 == 0) {
            System.out.println("Processing step: " + actualTime);
        }

        if (actualTime % 3000 == 0) {
            System.out.println("Reset");
            actualTime = 0l;
            initializeRandomPointsWithRandomCoordinateWithClusters();

        }
        actualTime++;
        List<Point> allPointsCalculate = clusters.stream().map(cluster -> cluster.getPoints()).flatMap(points -> points.stream()).collect(Collectors.toList());
        detectCollisionsMulti(allPointsCalculate);
        movePoints(allPointsCalculate);
        if (previousThread != null) {
            try {
                previousThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        allPoints = CenterOfMassCalculator.calculateCenterOfMass(allPointsCalculate, width, height);
        checkIntegrityAndSplitClustersOnExpiredEdges();

        previousThread = new Thread(() -> {
            DrawPoints.drawPoints(allPoints, frame + ".jpg", (int) width, (int) height, (int) radius);
        });
        previousThread.start();
    }

    public void movePoints(List<Point> allPoints) {
        for (Cluster cluster : clusters) {
            Set<Point> clusterPoints = cluster.getPoints();
            for (Point point : clusterPoints) {
                point.setDx(cluster.getDx());
                point.setDy(cluster.getDy());
            }
        }
        for (Point point : allPoints) {
            point.move(width, height);
        }
    }

    public void detectCollisionsMulti(List<Point> allPoints) {
        int numberOfThreads = 24;  // liczba wątków do utworzenia
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        final List<Point> allPointsSorted = allPoints.stream().sorted(Comparator.comparingDouble(Point::getGlobalX)).collect(Collectors.toList());
        int batchSize = allPoints.size() / numberOfThreads;  // Liczba punktów przetwarzanych przez jeden wątek

        for (int i = 0; i < numberOfThreads; i++) {
            final int start = i * batchSize;
            final int end = (i + 1 == numberOfThreads) ? allPoints.size() : start + batchSize;

            executor.submit(() -> {
                for (int x = start; x < end; x++) {
                    Point point1 = allPointsSorted.get(x);
                    for (int j = x + 1; j < allPointsSorted.size(); j++) {
                        Point point2 = allPointsSorted.get(j);
                        if (Math.abs(point2.getGlobalX() - point1.getGlobalX()) > radius
                                && point2.getGlobalX() > radius
                                && point1.getGlobalX() > radius) {
                            break;
                        }
                        if (point1.isColliding(point2, this.width, this.height, radius)) {
                            handleCollision(point1, point2);
                        }
                    }
                }

            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public synchronized void handleCollision(Point point1, Point point2) {
        Cluster cluster1 = findClusterContainingPointFast(point1);
        Cluster cluster2 = findClusterContainingPointFast(point2);
        Long lifetime = actualTime + clusterLifetime;
        if (cluster1 != null && cluster2 != null && cluster1 != cluster2) {
            // Sklej punkty i klastry
            Cluster mergedCluster = mergeClusters(cluster1, cluster2, point1, point2);
            clusters.remove(cluster1);
            clusters.remove(cluster2);
            clusters.add(mergedCluster);
        } else if (cluster1 != null && cluster2 == null) {
            // Dodaj punkt do klastra
            cluster1.addPoint(point2);
            cluster1.addEdge(point1, point2, lifetime);
        } else if (cluster1 == null && cluster2 != null) {
            // Dodaj punkt do klastra
            cluster2.addPoint(point1);
            cluster2.addEdge(point1, point2, lifetime);
        } else if (cluster1 == null && cluster2 == null) {
            // Stwórz nowy klaster
            Cluster newCluster = new Cluster(point1);
            newCluster.addPoint(point2);
            newCluster.addEdge(point1, point2, lifetime);
            clusters.add(newCluster);
        }
    }



    public void detectCollisions(List<Point> allPoints) {
        List<Point> pointsToHandle = new ArrayList<>();
        for (int i = 0; i < allPoints.size(); i++) {
            for (int j = i + 1; j < allPoints.size(); j++) {
                Point point1 = allPoints.get(i);
                Point point2 = allPoints.get(j);
                if (point1.isColliding(point2, this.width, this.height, radius)) {
                    pointsToHandle.add(point1);
                    pointsToHandle.add(point2);
                }
            }
        }
        for (int i = 0; i < pointsToHandle.size() - 1; i += 2) {
            handleCollision(pointsToHandle.get(i), pointsToHandle.get(i + 1));
        }
    }


    private Cluster findClusterContainingPoint(Point point) {
        for (Cluster cluster : clusters) {
            if (cluster.getPoints().contains(point)) {
                if (!cluster.equals(point.getOwner())) {
                    System.out.println("ERROR" + point.getOwner());
                }
                return cluster;
            }
        }
        return null;
    }

    private Cluster findClusterContainingPointFast(Point point) {
        return point.getOwner();
    }

    private Cluster mergeClusters(Cluster cluster1, Cluster cluster2, Point point1, Point point2) {
        Cluster mergedCluster = new Cluster(point1);

        for (Point point : cluster1.getPoints()) {
            point.setClusterDx(point.getDx());
            point.setClusterDy(point.getDy());
            mergedCluster.addPoint(point);
        }

        for (Point point : cluster2.getPoints()) {
            point.setClusterDx(point.getDx());
            point.setClusterDy(point.getDy());
            mergedCluster.addPoint(point);
        }

        mergedCluster.addEdge(point1, point2, actualTime + clusterLifetime);

        return mergedCluster;
    }

    private void checkIntegrityAndSplitClustersOnExpiredEdges() {
        Set<Cluster> clustersToRemove = new HashSet<>();
        Set<Cluster> clustersToAdd = new HashSet<>();

        for (Cluster cluster : clusters) {
            List<Cluster> newClusters = cluster.splitOnExpiredEdges(actualTime);

            for (Cluster newCluster : newClusters) {
                for (Point point : newCluster.getPoints()) {
                    Double tempDx = point.getClusterDx();
                    Double tempDy = point.getClusterDy();
                    tempDx = Math.abs(tempDx) < 10.0 ? tempDx * 1.002 : tempDx;
                    tempDy = Math.abs(tempDy) < 10.0 ? tempDy * 1.002 : tempDy;
                    point.setDx(tempDx);
                    point.setDy(tempDy);
                }
            }
            if (newClusters != null && newClusters.size() > 1) {
                clustersToRemove.add(cluster);
                clustersToAdd.addAll(newClusters);
            }
        }

        clusters.removeAll(clustersToRemove);
        clusters.addAll(clustersToAdd);
    }


}
