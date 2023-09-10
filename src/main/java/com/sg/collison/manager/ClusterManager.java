package com.sg.collison.manager;

import com.sg.collison.data.Cluster;
import com.sg.collison.data.Point;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sg.collison.manager.CenterOfMassCalculator.modulo;

@Service
public class ClusterManager {
    private Set<Cluster> clusters;

    @Autowired
    private DrawPoints drawPoints;

    private Set<Cluster.Edge> allEdges;

    @Value("${points.radius}")
    private double radius = 16.0;
    public static final int RESET_THRESHOLD = 200000;
    @Value("${points.number}")
    private Integer numberOfPoints = null;

    @Value("${cluster.lifetime}")
    private Long clusterLifetime = null;

    @Value("${threads.parts.number}")
    private int numberOfParts = 0;

    @Value("${threads.number}")
    private int numberOfThreads = 0;

    @Value("${threads.drawCount}")
    private int threadDrawCount = 0;

    public Set<Point> allPoints = null;
    private Long actualTime = 0L;

    private ExecutorService executor;
    private double width;

    private double height;
    private Integer THRESHOLD_PARALLELISM = 4096;
    private Thread currentThread = null;
    private Thread[] threads;

    public ClusterManager() {

    }

    @PostConstruct
    public void init() {
        this.executor = Executors.newWorkStealingPool(numberOfThreads);
        clusters = Collections.synchronizedSet(new HashSet<>());
        allEdges = Collections.synchronizedSet(new HashSet<>());
        width = 1080;
        height = 1080;
    }

    public void mainLoop() {
        initializeRandomPointsWithRandomCoordinateWithClusters();
        Integer frame = 0;
        while (true) {
            allProcessingOneStep(frame++);
        }
    }


    public void initializeRandomPointsWithRandomCoordinateWithClusters() {
        clusters = Collections.synchronizedSet(new HashSet<>());
        allEdges = Collections.synchronizedSet(new HashSet<>());
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

    private long detectCollisionsTime = 0;
    private long checkIntegrityTime = 0;
    private long movePointsTime = 0;
    private long drawPointsTime = 0;

    private double dxMass;
    private double dyMass;

    private Point actualCenterOfMass = null;
    private Point previousCenterOfMass = null;


    private static final int TIME_RESET_FREQUENCY = 10;
    private static final double MILLION = 1000000.0;

    public void allProcessingOneStep(Integer frame) {
        resetPointsIfNeeded();

        Set<Point> allPointsCalculate = getAllPointsFromClusters();

        handleCollisions(allPointsCalculate);
        updateEdgesAndClusters(allPointsCalculate);
        movePoints(allPointsCalculate);

        ImmutablePair<Set<Point>, Point> pair = CenterOfMassCalculator.movePointsToCenterOfMass(allPointsCalculate, width, height);
        updateCenterOfMassData(pair);
        manageThreadStateAndUpdates();
        handleDrawing(frame, pair);

        printAndResetTimings();
    }

    private void resetPointsIfNeeded() {
        if (actualTime % RESET_THRESHOLD == 0) {
            System.out.println("Reset");
            actualTime = 0l;
            initializeRandomPointsWithRandomCoordinateWithClusters();
        }
        actualTime++;
    }

    private Set<Point> getAllPointsFromClusters() {
        return clusters.stream()
                .map(Cluster::getPoints)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private void handleCollisions(Set<Point> points) {
        if (clusters.size() > 1) {
            long startTime = System.nanoTime();
            detectCollisionsMulti(points);
            long endTime = System.nanoTime();
            detectCollisionsTime += (endTime - startTime) / 1000;
        }
    }

    private void updateEdgesAndClusters(Set<Point> points) {
        long startTime = System.nanoTime();
        clusters = checkIntegrityAndSplitClustersOnExpiredEdgesMulti(points);
        long endTime = System.nanoTime();
        checkIntegrityTime += (endTime - startTime) / 1000;
    }

    private void updateCenterOfMassData(ImmutablePair<Set<Point>, Point> pair) {
        previousCenterOfMass = actualCenterOfMass;
        allPoints = pair.getKey();
        actualCenterOfMass = pair.getValue();
        if (previousCenterOfMass != null && actualCenterOfMass != null) {
            dxMass = actualCenterOfMass.getGlobalX() - previousCenterOfMass.getGlobalX();
            dyMass = actualCenterOfMass.getGlobalY() - previousCenterOfMass.getGlobalY();
        } else {
            dxMass = 0.0;
            dyMass = 0.0;
        }
    }

    private void handleDrawing(Integer frame, ImmutablePair<Set<Point>, Point> pair) {
        final Set<Point> drawingPoints = new HashSet<>(allPoints);
        int sizeBiggestCluster = clusters.stream().map(cluster -> cluster.getPoints().size()).max(Integer::compareTo).get();
        currentThread = new Thread(() -> {
            drawPoints.drawPoints(drawingPoints, frame + ".jpg", (int) width, (int) height, radius, clusters.size(), sizeBiggestCluster, dxMass, dyMass);
        });
        currentThread.start();
        threads[0] = currentThread;
    }

    private void printAndResetTimings() {
        if (actualTime % TIME_RESET_FREQUENCY == 0) {
            System.out.println("Processing step: " + actualTime);
            System.out.println("Aggregate Time detectCollisionsMultiGPT: " + detectCollisionsTime / MILLION);
            System.out.println("Aggregate Time checkClustersOnExpiredEdgesMulti: " + checkIntegrityTime / MILLION);
            System.out.println("Aggregate Time movePoints: " + movePointsTime / MILLION);
            System.out.println("Aggregate Time drawPoints: " + drawPointsTime / MILLION);

            detectCollisionsTime = 0;
            checkIntegrityTime = 0;
            movePointsTime = 0;
            drawPointsTime = 0;
        }
    }

    public void manageThreadStateAndUpdates() {
        // Upewniamy się, że tablica wątków została zainicjowana
        if (threads == null) {
            threads = new Thread[threadDrawCount];
        }

        // Jeśli ostatni wątek w tablicy jest aktywny, próbujemy go zakończyć
        if (threads[threadDrawCount - 1] != null && threads[threadDrawCount - 1].isAlive()) {
            try {
                long startTime = System.nanoTime();
                threads[threadDrawCount - 1].join();
                long endTime = System.nanoTime();
                drawPointsTime += (endTime - startTime) / 1000;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Przesuwamy wszystkie wątki o jedno miejsce w tablicy
        for (int i = threadDrawCount - 1; i > 0; i--) {
            threads[i] = threads[i - 1];
        }
    }

    public void updateVelocityWithRotation(double centerX, double centerY, double theta, Set<Point> points) {
        double maxDistance = Math.min(width, height) / 3.0;

        for (Point point : points) {
            // Przesunięcie do nowego układu współrzędnych
            double x = modulo((point.getGlobalX() - centerX + 1.5 * width), width);
            double y = modulo((point.getGlobalY() - centerY + 1.5 * height), height);
            x -= 0.5 * width;
            y -= 0.5 * height;

            // Obliczenie odległości od środka ciężkości
            double distance = Math.sqrt(x * x + y * y);

            // Współczynnik zależny od odległości od środka ciężkości
            double distanceFactor = distance / maxDistance;  // Zakładając, że maxDistance jest maksymalną możliwą odległością
            distanceFactor = distance > maxDistance ? 1.0 : distanceFactor;  // Jeśli odległość jest większa niż maxDistance, to ustawiamy współczynnik na 1.0
            double speedFactor = Math.pow(1 - distanceFactor, 4.0);  // Możesz użyć różnych funkcji dla speedFactor

            // Obliczenie nowych wartości x i y po rotacji
            double rotated_x = x * Math.cos(theta * speedFactor) - y * Math.sin(theta * speedFactor);
            double rotated_y = x * Math.sin(theta * speedFactor) + y * Math.cos(theta * speedFactor);

            // Obliczenie zmiany w prędkości
            double delta_dx = rotated_x - x;
            double delta_dy = rotated_y - y;

            // Aktualizacja prędkości
            point.setDx(point.getDx() + delta_dx);
            point.setDy(point.getDy() + delta_dy);
        }
    }


    public void movePoints(Set<Point> allPoints) {
        long startTime = System.nanoTime();
        for (Cluster cluster : clusters) {
            Set<Point> clusterPoints = cluster.getPoints();
            for (Point point : clusterPoints) {
                point.setDx(cluster.getDx());
                point.setDy(cluster.getDy());
            }
            Point center = CenterOfMassCalculator.calculateCenterOfMass(clusterPoints.stream().collect(Collectors.toSet()), width, height);
            updateVelocityWithRotation(center.getGlobalX(), center.getGlobalY(), 0.1, clusterPoints);
        }
        for (Point point : allPoints) {
            point.move(width, height);
        }
        long endTime = System.nanoTime();
        movePointsTime += (endTime - startTime) / 1000;
    }

    public void detectCollisionsMulti(Set<Point> allPoints) {
        Long lifetime = actualTime + clusterLifetime;
        if (executor.isShutdown()) {
            executor = Executors.newWorkStealingPool(numberOfThreads);
        }
        final List<Point> allPointsSorted = allPoints.stream().sorted(Comparator.comparingDouble(Point::getGlobalX)).collect(Collectors.toList());
        int batchSize = allPoints.size() / numberOfParts;  // Liczba punktów przetwarzanych przez jeden wątek
        CountDownLatch latch = new CountDownLatch(numberOfParts);

        for (int i = 0; i < numberOfParts; i++) {
            final int start = i * batchSize;
            final int end = (i + 1 == numberOfParts) ? allPoints.size() : start + batchSize;
            executor.submit(() -> {
                try {
                    Random random = new Random();
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
                                Cluster cluster1 = findClusterContainingPointFast(point1);
                                Cluster cluster2 = findClusterContainingPointFast(point2);
                                if (cluster1 != null && cluster2 != null && cluster1.equals(cluster2)) {
                                    continue;
                                }
                                Cluster.Edge edge = new Cluster.Edge(point1, point2, lifetime + random.nextDouble() * clusterLifetime);
                                if (!allEdges.contains(edge)) {
                                    allEdges.add(edge);
                                }
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdownExecutor() {
        executor.shutdown();
    }

    private Cluster findClusterContainingPointFast(Point point) {
        return point.getOwner();
    }

    public Set<Cluster> splitByDFSClusterMulti(Integer threads, Map<Long, Set<Cluster.Edge>> pointEdgeMap, Set<Point> allPoints) {
        Set<Cluster> clusters = new HashSet<>();
        Set<Point> todoPoints = new HashSet<>(allPoints);
        ExecutorService executor = Executors.newWorkStealingPool(threads);
        ExecutorCompletionService<Cluster> completionService = new ExecutorCompletionService<>(executor);
        Map<Long, Point> mapPoints = allPoints.parallelStream().collect(Collectors.toMap(Point::getId, Function.identity()));
        Integer limit = numberOfPoints / 100 + 1;
        while (todoPoints.size() > 0) {
            List<Point> startingPoints = todoPoints.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            for (Point point : startingPoints) {
                completionService.submit(() -> {
                    Set<Point> newClusterPoints = new HashSet<>();
                    Set<Point> visitedOnFuture = new HashSet<>();
                    AtomicBoolean isNew = new AtomicBoolean(true);
                    dfsMulti(isNew, point, newClusterPoints, visitedOnFuture, todoPoints, pointEdgeMap, mapPoints);
                    if (!isNew.get()) {
                        return null;
                    }
                    Cluster newCluster = new Cluster(point);
                    for (Point newPoint : newClusterPoints) {
                        newCluster.addPoint(newPoint);
                        newCluster.getEdges().addAll(pointEdgeMap.get(newPoint.getId()));
                    }
                    return newCluster;
                });
            }

            for (int i = 0; i < startingPoints.size(); i++) {
                try {
                    Future<Cluster> future = completionService.take(); // Blokuje, aż którykolwiek z zadań się zakończy
                    Cluster cluster = future.get(); // Nie blokuje, ponieważ zadanie już się zakończyło

                    if (cluster != null) {
                        todoPoints.removeAll(cluster.getPoints());
                        clusters.add(cluster);
                    }

                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        executor.shutdown();
        return clusters;
    }

    private void dfsMulti(AtomicBoolean isNew, Point start, Set<Point> newClusterPoints, Set<Point> visited, Set<Point> todoPoints, Map<Long, Set<Cluster.Edge>> pointEdgeMap, Map<Long, Point> mapPoints) {
        Stack<Point> stack = new Stack<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            Point current = stack.pop();

            if (visited.contains(current)) continue;
            visited.add(current);
            newClusterPoints.add(current);

            Set<Cluster.Edge> edgesWithPoint = pointEdgeMap.get(current.getId());

            for (Cluster.Edge edge : edgesWithPoint) {
                Long nextPointId = edge.getPoint1Id().equals(current.getId()) ? edge.getPoint2Id() : edge.getPoint1Id();
                Point nextPoint = mapPoints.get(nextPointId);

                if (!todoPoints.contains(nextPoint)) {
                    isNew.set(false);
                    return;
                }

                if (nextPoint != null && !visited.contains(nextPoint)) {
                    stack.push(nextPoint);
                }
            }
        }
    }


    public Set<Cluster> splitOnExpiredEdges(Long actualTime, Set<Cluster.Edge> edges, Set<Point> allPoints) {
        ConcurrentMap<Long, Set<Cluster.Edge>> pointEdgeMap = allPoints.parallelStream()
                .collect(Collectors.toConcurrentMap(Point::getId, point -> ConcurrentHashMap.newKeySet()));

        // Usuwanie wygasłych krawędzi za pomocą strumieni
        edges.removeIf(edge -> edge.getLifetime() < actualTime);

        // Wypełnianie pointEdgeMap równolegle
        edges.parallelStream().forEach(edge -> {
            Long point1 = edge.getPoint1Id();
            Long point2 = edge.getPoint2Id();
            pointEdgeMap.get(point1).add(edge);
            pointEdgeMap.get(point2).add(edge);
        });

        return this.splitByDFSClusterMulti(numberOfThreads, pointEdgeMap, allPoints);
    }


    private Set<Cluster> checkIntegrityAndSplitClustersOnExpiredEdgesMulti(Set<Point> allPoints) {
        Set<Cluster> newClusters = splitOnExpiredEdges(actualTime, this.allEdges, allPoints);

        System.out.println("New clusters: " + newClusters.size());
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
        newClusters.parallelStream().filter(c -> c != null && c.getEdges().size() > 1).forEach(Cluster::computeMST);
        ConcurrentMap<Cluster.Edge, Boolean> concurrentEdgesMap = newClusters.parallelStream()
                .flatMap(cluster -> cluster.getEdges().stream())
                .distinct()
                .collect(Collectors.toConcurrentMap(edge -> edge, edge -> Boolean.TRUE, (existing, replacement) -> existing));

        this.allEdges = new ConcurrentSkipListSet<>();
        this.allEdges.addAll(concurrentEdgesMap.keySet());

        return newClusters;
    }
}
