// Data clustering. Implementation by Dave Musicant
import mpi.*;
import java.nio.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Clustering {

    public static final double TOL = 0.1;

    // Grab the data

    public static double[][] importData() {
        try {

            // First grab as ArrayList of strings, largely to just count
            Scanner scanner = new Scanner(new File("/tmp/output.txt"));
            ArrayList<String> datasetAsStrings = new ArrayList<>();

            while (scanner.hasNext()) {
                datasetAsStrings.add(scanner.nextLine().trim());
            }

            int rows = datasetAsStrings.size();

            // Determine number of columns from first row; if dataset is inconsistent, code will break
            int cols = 0;
            Scanner rowScanner = new Scanner(datasetAsStrings.get(0));
            while (rowScanner.hasNext()) {
                cols++;
                rowScanner.next();
            }

            double[][] dataset = new double[rows][cols];
            for (int row = 0; row < datasetAsStrings.size(); row++) {
                rowScanner = new Scanner(datasetAsStrings.get(row));
                int col = 0;
                while (rowScanner.hasNext()) {
                    dataset[row][col] = Double.parseDouble(rowScanner.next());
                    col++;
                }
            }

            return dataset;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double distance(double[] row1, double[] row2) {
        double distance = 0;
        for (int i = 0; i < row1.length; i++) {
            distance += Math.pow(row1[i] - row2[i], 2);
        }
        return distance;
    }

    public static double[][] cluster(double[][] data, int k) throws MPIException {
        int rows = data.length;
        int cols = data[0].length;

        int size = MPI.COMM_WORLD.getSize();
        int rank = MPI.COMM_WORLD.getRank();

        int start = (int)((rank / (double)(size)) * rows) ;
        int end = (int)(((rank + 1)/(double)(size)) * rows);
        
        //System.out.println("rank: " + rank + " start: " + start + " end: " + end);
        // Initially assign all points to no cluster at all
        int[] assignments = new int[rows];
        for (int i = 0; i < rows; i++) {
            assignments[i] = -1;
        }

        // Initial centers are arbitrarily space points
        double[][] centers = new double[k][cols];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < cols; j++) {
                centers[i][j] = data[rows / k * i][j];
            }
        }


        // Kick off main loop with crazy large errors to get started
        double prevError = Double.MAX_VALUE;
        double curError = Double.MAX_VALUE / 2;

        while (curError < prevError - TOL) {
            //System.out.println("rank " + rank + " iteration");
            prevError = curError;
            curError = 0;

            // Assign all clusters to nearest centers. Keep track of which one is furthest to handle the empty
            // cluster possibility.
            double largestDist = Double.MIN_VALUE;
            int furthestPoint = -1;
            for (int i = start; i < end; i++) { //figure out tomorrow.
                int closest = -1;
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < k; j++) {
                    double dist = distance(data[i], centers[j]);
                    if (dist <= minDist) {
                        minDist = dist;
                        closest = j;
                    }
                }
                if (minDist > largestDist) {
                    largestDist = minDist;
                    furthestPoint = i;
                }
                assignments[i] = closest;
                curError += minDist;
            }
            
            
            // Here, we're reducing and summing curError,
            // and then broadcasting it back out for everyone.
            //System.out.println("rank " + rank + " curError " + curError);
            DoubleBuffer buffer = MPI.newDoubleBuffer(1);
            buffer.put(0, curError);
            DoubleBuffer finalError = MPI.newDoubleBuffer(1);
            MPI.COMM_WORLD.reduce(buffer, finalError, 1, MPI.DOUBLE, MPI.SUM, 0);
            DoubleBuffer buffer1 = MPI.newDoubleBuffer(1);
            if (rank == 0) {
                buffer1.put(0, finalError.get(0));
            }
            MPI.COMM_WORLD.bcast(buffer1, 1, MPI.DOUBLE, 0);
            //System.out.println("Avg Clustering error: "  + buffer1.get(0)/rows);
            
            curError = buffer1.get(0)/rows;
            System.out.println("rank: " + rank + " error: " + curError);
            // If an empty cluster happens, we need to make sure all
            // copies have the same largestDist and furthestPoint
            // so that when we're remaking centers, it doesn't bug out
            buffer = MPI.newDoubleBuffer(1);
            buffer.put(0, largestDist);
            DoubleBuffer largeDist = MPI.newDoubleBuffer(1);
            MPI.COMM_WORLD.reduce(buffer, largeDist, 1, MPI.DOUBLE, MPI.MAX, 0);
            if (rank == 0)
                MPI.COMM_WORLD.bcast(largeDist, 1, MPI.DOUBLE, 0);
            if (largeDist.get(0) == largestDist)
                buffer.put(0, furthestPoint);
            if (rank == 0)
                MPI.COMM_WORLD.bcast(buffer, 1, MPI.DOUBLE, 0);
            furthestPoint = (int)buffer.get(0);


            
            // Assign new centroids for each cluster
            int[] numPoints = new int[k];
            double[][] clusterTotal = new double[k][cols];

            // Aggregate each cluster
            for (int i = start; i < end; i++) {
                int cluster = assignments[i];
                numPoints[cluster]++;
                for (int j = 0; j < cols; j++) {
                    clusterTotal[cluster][j] += data[i][j];
                }
            }

            // Show assignments
            // Calculate averages. If a cluster is empty, just pick as a centroid the row of the
            // dataset that matches the cluster number. This is not a great strategy, but good enough
            // and the code is short.
            for (int cluster = 0; cluster < k; cluster++) {
                
                DoubleBuffer newBuff = MPI.newDoubleBuffer(1);
                buffer = MPI.newDoubleBuffer(1);

                buffer.put(0, numPoints[cluster]); 
                
                MPI.COMM_WORLD.reduce(buffer, newBuff, 1, MPI.DOUBLE, MPI.SUM, 0);
                if (rank == 0)
                    MPI.COMM_WORLD.bcast(newBuff, 1, MPI.DOUBLE, 0);
                numPoints[cluster] = (int)newBuff.get(0);
                if (rank==0)
                    System.out.println("Num Points for cluster " + cluster + ": " + numPoints[cluster]);
                for (int j = 0; j < cols; j++) {
                    if (numPoints[cluster] > 0) {
                        // to make this parallel, we need to
                        // for each k
                        //    reduce the cluster totals
                        //    reduce numPoints
                        //    do the division
                        //    broadcast the new center value
                        buffer = MPI.newDoubleBuffer(1);
                        newBuff = MPI.newDoubleBuffer(1);

                        buffer.put(0, clusterTotal[cluster][j]);
                        MPI.COMM_WORLD.reduce(buffer, newBuff, 1, MPI.DOUBLE, MPI.SUM, 0);
                        if (rank == 0)
                            MPI.COMM_WORLD.bcast(newBuff, 1, MPI.DOUBLE, 0);
                        clusterTotal[cluster][j] = newBuff.get(0);
                        centers[cluster][j] = clusterTotal[cluster][j] / numPoints[cluster];
                    }

                    else {
                        if (rank == 0 && j==0)
                            System.out.println("Empty cluster happened");
                        centers[cluster][j] = data[furthestPoint][j];
                    }
                }
            }

        }
        System.out.println("rank: " + rank + " done");
        return centers;
    }


    public static void main(String[] args) throws MPIException {

        MPI.Init(args);

        double[][] dataset = importData();
        double[][] centers = cluster(dataset, 10);
        if (MPI.COMM_WORLD.getRank() == 0){
            System.out.println("Centers are: ");
            for (double[] row : centers) {
                for (double value : row) {
                    System.out.print(value + " ");
                }
                System.out.println();
            }
        }

        MPI.Finalize();
    }
}
