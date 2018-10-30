import mpi.*;
import java.net.*;

public class HelloWorld {

    public static void main(String[] args) throws MPIException, UnknownHostException {

        MPI.Init(args);

        int size = MPI.COMM_WORLD.getSize();
        int rank = MPI.COMM_WORLD.getRank();
        String hostname = InetAddress.getLocalHost().getHostName();

        System.out.println("Hello world from host " + hostname + ", rank " + rank + " out of " + size);

        MPI.Finalize();
    }
}
