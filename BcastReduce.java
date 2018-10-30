import mpi.*;
import java.nio.*;
import java.util.Scanner;
import java.lang.Math.*;

public class BcastReduce {

    public static void main(String[] args) throws MPIException {

        MPI.Init(args);

        int size = MPI.COMM_WORLD.getSize();
        int rank = MPI.COMM_WORLD.getRank();
        String name = MPI.COMM_WORLD.getName();

        DoubleBuffer buffer = MPI.newDoubleBuffer(1);

        int numItemsToTransfer = 1;
        int sourceRank = 0;
        int bufferPosition = 0;
        

        double n = 1000;
        double width  = 0.0;
        double sum = 0.0;


    /**
        root process only:
        ask the user for a value n
        broadcast the value n to all processes
            width <- 1 / n
        sum = 0
        forall 1 <= i <= n (in parallel - divide the i values between processes somehow):
            sum <- sum + 4 / (1 + (width * (i - 0.5)) ** 2)
        mypi <- width * sum
        sum all processes' mypi using reduce
        print sum
    */

        if (rank == sourceRank) {
            //Scanner in = new Scanner(System.in);
            //System.out.print("n = ");
            //n = (double)in.nextInt();

            //in.close();
            buffer.put(bufferPosition, n);
        }


        MPI.COMM_WORLD.bcast(buffer, numItemsToTransfer, MPI.DOUBLE, sourceRank);

        double valueReceived = buffer.get(0);
        
        n = valueReceived;
        //System.out.println(n);

        width = 1 / n;
        

        double start = (rank / size) * n + 1;
        double end = ((rank + 1)/size) * n ;

        for (double i = start ; i <= end; i++)
        {
            
            sum += 4 / (1+ Math.pow(width * (i-0.5), 2));
            //System.out.println(sum);
            
        }

        
        double piFrag = width * sum;
        System.out.println(piFrag);

        buffer.put(bufferPosition, piFrag);
        DoubleBuffer finalAnswer = MPI.newDoubleBuffer(1);
        //System.out.println();
        int destProcess = 0;
        MPI.COMM_WORLD.reduce(buffer, finalAnswer, numItemsToTransfer,
                              MPI.DOUBLE, MPI.SUM, destProcess);
        if (rank == 0) {
            System.out.println("Final answer = " + finalAnswer.get(0));
        }

        MPI.Finalize();

    }
}
