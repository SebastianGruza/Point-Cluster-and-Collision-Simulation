package com.sg.collison;

import com.sg.collison.data.Cluster;
import com.sg.collison.data.Point;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CollisonApplicationTests {

//	@Test
//	void contextLoads() {
//	}
//
//	@Test
//	public void testSingleCluster() {
//		Cluster cluster = new Cluster(new Point(1L));
//		cluster.addPoint(new Point(2L));
//		cluster.addEdge(new Point(1L), new Point(2L), 1000L);
//
//		List<Cluster> splitClusters = cluster.splitByDFSCluster();
//
//		assertEquals(1, splitClusters.size());
//	}
//
//	@Test
//	public void testMultipleClusters() {
//		Cluster cluster = new Cluster(new Point(1L));
//		cluster.addPoint(new Point(2L));
//		cluster.addPoint(new Point(3L));
//		cluster.addEdge(new Point(1L), new Point(2L), 1000L);
//
//		List<Cluster> splitClusters = cluster.splitByDFSCluster();
//
//		assertEquals(2, splitClusters.size());
//	}
//
//	@Test
//	public void testMultipleClustersWithExpiredEdges() {
//		Cluster cluster = new Cluster(new Point(1L));
//		cluster.addPoint(new Point(2L));
//		cluster.addPoint(new Point(3L));
//		cluster.addEdge(new Point(1L), new Point(2L), 1000L);
//
//		List<Cluster> splitClusters = cluster.splitOnExpiredEdges(1001L);
//
//		assertEquals(3, splitClusters.size());
//	}

}
