import mpi.*;
import java.nio.*;

public class SendRecv {

    public static void main(String[] args) throws MPIException{

        MPI.Init(args);

        int size = MPI.COMM_WORLD.getSize();
        int rank = MPI.COMM_WORLD.getRank();

        if (size < 4) {
            System.out.println("Too few processes!");
            System.exit(1);
        }

        IntBuffer buffer = MPI.newIntBuffer(1);

        int numItemsToTransfer = 1;
        int sourceRank = rank - 1;
        int destinationRank = rank + 1;
        int messageTag = 0;  // supplemental tag, often just not used
        int bufferPosition = 0;
        if (rank == 0) {
                int valueToTransmit = -1;
                buffer.put(bufferPosition, valueToTransmit);
                sourceRank = size-1;
                MPI.COMM_WORLD.send(buffer, numItemsToTransfer, MPI.INT,
                                    destinationRank, messageTag);
            } 
        
        if(rank == size - 1){
            destinationRank = 0;
        }

        
        
        Status status = MPI.COMM_WORLD.recv(buffer, numItemsToTransfer,
                                            MPI.INT, sourceRank, messageTag);
        System.out.println("Process " + rank + " received value " +
                           buffer.get(bufferPosition) + " from process " + sourceRank );
         MPI.COMM_WORLD.send(buffer, numItemsToTransfer, MPI.INT,
                                destinationRank, messageTag);
                
            
        

        MPI.Finalize();
    }
}