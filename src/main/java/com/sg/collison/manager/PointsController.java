package com.sg.collison.manager;

import com.sg.collison.data.Cluster;
import com.sg.collison.data.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class PointsController {


    @Autowired
    ClusterManager clusterManager;
    @GetMapping("/points")
    public List<Point> getPoints() {
        while (clusterManager == null || clusterManager.allPoints == null || clusterManager.allPoints.size() < clusterManager.numberOfPoints) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return clusterManager.allPoints;
    }
}
