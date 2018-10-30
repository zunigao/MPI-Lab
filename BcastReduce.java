import mpi.*;
import java.nio.*;
import java.util.Scanner.*;
import java.lang.Math.*;

public class BcastReduce {

    public static void main(String[] args) throws MPIException {

        MPI.Init(args);

        int size = MPI.COMM_WORLD.getSize();
        int rank = MPI.COMM_WORLD.getRank();
        String name = MPI.COMM_WORLD.getName();

        IntBuffer buffer = MPI.newIntBuffer(1);

        int numItemsToTransfer = 1;
        int sourceRank = 0;
        int bufferPosition = 0;
        

        int n;
        int width = 1 / n;
        int sum = 0;


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
            Scanner in = new Scanner(System.in);
            System.out.print("n = ");
            n = in.nextInt();
            in.close();
            buffer.put(bufferPosition, n);
        }

        MPI.COMM_WORLD.bcast(buffer, numItemsToTransfer, MPI.INT, sourceRank);

        int valueReceived = buffer.get(0);
        for (int i = 1; i <= n*(rank+1)/size; i++)
        {
            sum += 4 / (1+Math.pow(width * (i-0.5), 2));
        }
        int piFrag = width * sum;

        buffer.put(bufferPosition, piFrag);
        IntBuffer finalAnswer = MPI.newIntBuffer(1);
        int destProcess = 0;
        MPI.COMM_WORLD.reduce(buffer, finalAnswer, numItemsToTransfer,
                              MPI.INT, MPI.SUM, destProcess);
        if (rank == 0) {
            System.out.println("Final answer = " + finalAnswer.get(0));
        }

        MPI.Finalize();

    }
}