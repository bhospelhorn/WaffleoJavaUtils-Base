package waffleoRai_Utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class KMeansClustering {

	public static class KMeansResults{
		public int[] clusterAssignments;
		public double[] centroidDists;
		public double[][] clusterCentroids;
		
		public double[] intraDist; //Intra-cluster average distance for each cluster
		public double[] interDist; //Inter-cluster average distance for each cluster
		
		public double avgIntraDist;
		public double avgInterDist;
		
		public int iterations = 0;
	}
	
	private static double getDistSq(double[] p1, double[] p2) {
		double sum = 0.0;
		int dims = p1.length;
		for(int d = 0; d < dims; d++) {
			double diff = p1[d] - p2[d];
			sum += (diff * diff);
		}
		
		return sum;
	}
	
	private static void getDistsSq(double[][] points, double[] ref, double[] output) {
		int ptCount = points.length;
		for(int i = 0; i < ptCount; i++) {
			output[i] = getDistSq(points[i], ref);
		}
	}
	
	private static int[] generateSeedPool(int size, double[] seedCentroidDists, Set<Integer> usedPoints) {
		int[] pool = new int[size];
		
		//Create weights
		int ptCount = seedCentroidDists.length;
		double sum = 0.0;
		for(int i = 0; i < ptCount; i++) {
			if(!usedPoints.contains(i)) {
				sum += seedCentroidDists[i];
			}
		}
		
		int pPos = 0;
		for(int i = 0; i < ptCount; i++) {
			if(pPos >= size) break;
			if(!usedPoints.contains(i)) {
				int addCount = (int)Math.round((seedCentroidDists[i] / sum) * (double)size);
				for(int j = 0; j < addCount; j++) {
					pool[pPos++] = i;
					if(pPos >= size) break;
				}
			}
		}
		
		return pool;
	}
	
	private static void clusterRound(double[][] inputData, KMeansResults clusters) {
		int k = clusters.clusterCentroids.length;
		int ptCount = inputData.length;
		int dims = inputData[0].length;
		
		//Reassign points to clusters
		double[] pToCDist = new double[k]; //Distance from point to each cluster centroid
		for(int i = 0; i < ptCount; i++) {
			double minDist = Integer.MAX_VALUE;
			int minIdx = -1;
			
			getDistsSq(clusters.clusterCentroids, inputData[i], pToCDist);
			for(int c = 0; c < k; c++) {
				if(pToCDist[c] < minDist) {
					minDist = pToCDist[c];
					minIdx = c;
				}
			}
			
			clusters.clusterAssignments[i] = minIdx;
			clusters.centroidDists[i] = minDist;
		}
	
		//Update centroids based on new cluster contents
		int[] memberCounts = new int[k];
		double[][] dimSums = new double[k][dims];
		for(int i = 0; i < ptCount; i++) {
			int c = clusters.clusterAssignments[i];
			memberCounts[c]++;
			for(int d = 0; d < dims; d++) {
				dimSums[c][d] += inputData[i][d];
			}
		}
		
		for(int c = 0; c < k; c++) {
			for(int d = 0; d < dims; d++) {
				clusters.clusterCentroids[c][d] = dimSums[c][d] / (double)memberCounts[c];
			}
		}
		
		//Update distances from new centroids and intra-cluster distance average
		clusters.avgIntraDist = 0.0;
		Arrays.fill(clusters.intraDist, 0.0);
		for(int i = 0; i < ptCount; i++) {
			int c = clusters.clusterAssignments[i];
			clusters.centroidDists[i] = getDistSq(inputData[i], clusters.clusterCentroids[c]);
			clusters.intraDist[c] += clusters.centroidDists[i];
		}
		for(int c = 0; c < k; c++) {
			clusters.intraDist[c] /= (double)memberCounts[c];
			clusters.avgIntraDist += clusters.intraDist[c];
		}
		clusters.avgIntraDist /=(double)k;
		
		//Update inter-cluster distance average
		clusters.avgInterDist = 0.0;
		double[][] interDists = new double[k][k];
		for(int c = 0; c < k; c++) {
			clusters.interDist[c] = 0.0;
			for(int j = 0; j < c; j++) {
				//Previously calculated
				clusters.interDist[c] += interDists[j][c];
			}
			
			for(int j = (c+1); j < k; j++) {
				interDists[c][j] =  getDistSq(clusters.clusterCentroids[c], clusters.clusterCentroids[j]);
				clusters.interDist[c] += interDists[c][j];
			}
			
			clusters.interDist[c] /= (double)(k-1);
			clusters.avgInterDist += clusters.interDist[c];
		}
		clusters.avgInterDist /=(double)k;
	}
	
	public static KMeansResults cluster(double[][] inputData, int k, int maxIter, long randomSeed) {
		if(inputData == null) return null;
		KMeansResults res = new KMeansResults();
		
		int rowCount = inputData.length;
		int dims = inputData[0].length;
		res.clusterAssignments = new int[rowCount];
		res.centroidDists = new double[rowCount];
		res.clusterCentroids = new double[k][dims];
		res.intraDist = new double[k];
		res.interDist = new double[k];
		
		//Seed clusters
		int seedPoolSize = Math.max(rowCount << 2, k << 8);
		Random rand;
		if(randomSeed == -1L) rand = new Random();
		else rand = new Random(randomSeed);
		int index = rand.nextInt(rowCount);
		Set<Integer> usedPoints = new HashSet<Integer>();
		usedPoints.add(index);
		double[] seedCentroid = new double[dims];
		double[] seedSums = new double[dims];
		for(int d = 0; d < dims; d++) {
			res.clusterCentroids[0][d] = inputData[index][d];
			seedCentroid[d] = inputData[index][d];
			seedSums[d] = inputData[index][d];
		}
		double[] seedCentroidDists = new double[rowCount];
		getDistsSq(inputData, seedCentroid, seedCentroidDists);
		int[] seedPool = generateSeedPool(seedPoolSize, seedCentroidDists, usedPoints);
		
		for(int i = 1; i < k; i++) {
			index = rand.nextInt(seedPool.length);
			index = seedPool[index];
			usedPoints.add(index);
			
			//Update seed centroid
			double total = (double)i + 1.0;
			double weightOld = (total - 1.0) / total;
			double weightNew = 1.0 / total;
			for(int d = 0; d < dims; d++) {
				res.clusterCentroids[i][d] = inputData[index][d];
				seedCentroid[d] = (seedSums[d] * weightOld) + (inputData[index][d] * weightNew);
				seedSums[d] += inputData[index][d];
			}
			
			//Update distances
			getDistsSq(inputData, seedCentroid, seedCentroidDists);
			
			//Update probability pool
			seedPool = generateSeedPool(seedPoolSize, seedCentroidDists, usedPoints);
		}
		
		//Run clustering
		clusterRound(inputData, res);
		int itr = 1;
		while(((maxIter < 0) || (itr < maxIter)) && (res.avgInterDist < res.avgIntraDist)) {
			clusterRound(inputData, res);
			itr++;
		}
		res.iterations = itr;
		return res;
	}
	
	public static KMeansResults cluster(int[][] inputData, int k, int maxIter, long randomSeed) {
		if(inputData == null) return null;
		
		int rowCount = inputData.length;
		int dims = inputData[0].length;
		double[][] data = new double[rowCount][dims];
		for(int i = 0; i < rowCount; i++) {
			for(int j = 0; j < dims; j++) {
				data[i][j] = (double)inputData[i][j];
			}
		}
		
		return cluster(data, k, maxIter, randomSeed);
	}
	
	
}
