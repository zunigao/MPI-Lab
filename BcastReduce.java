import mpi.*;
import java.nio.*;

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
        int numberToPlayWith = 5;

        if (rank == sourceRank) {
            buffer.put(bufferPosition, numberToPlayWith * 3);
        }

        MPI.COMM_WORLD.bcast(buffer, numItemsToTransfer, MPI.INT, sourceRank);

        int valueReceived = buffer.get(0);

        int valueModified = valueReceived + 4;

        buffer.put(bufferPosition, valueModified);

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
