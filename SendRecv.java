import mpi.*;
import java.nio.*;

public class SendRecv {

    public static void main(String[] args) throws MPIException{

        MPI.Init(args);

        int size = MPI.COMM_WORLD.getSize();
        int rank = MPI.COMM_WORLD.getRank();

        if (size < 2) {
            System.out.println("Too few processes!");
            System.exit(1);
        }

        IntBuffer buffer = MPI.newIntBuffer(1);

        int numItemsToTransfer = 1;
        int sourceRank = 0;
        int destinationRank = 1;
        int messageTag = 0;  // supplemental tag, often just not used
        int bufferPosition = 0;

        if (rank == 0) {
            int valueToTransmit = 5;
            buffer.put(bufferPosition, valueToTransmit);
            MPI.COMM_WORLD.send(buffer, numItemsToTransfer, MPI.INT,
                                destinationRank, messageTag);
        } else if (rank == 1) {
            Status status = MPI.COMM_WORLD.recv(buffer, numItemsToTransfer,
                                                MPI.INT, sourceRank, messageTag);
            System.out.println("Process 1 received value " +
                               buffer.get(bufferPosition) + " from process 0");
        }

        MPI.Finalize();
    }
}