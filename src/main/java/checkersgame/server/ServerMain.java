package checkersgame.server;

import checkersgame.common.IHomePage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * <b>This is the main class of the CheckersGame Server</b>
 *
 * <p>The class is responsible for initializing the server side of the CheckersGame</p>
 *
 * @author Ilay Zvi
 */
public class ServerMain {
    public static void main(String[] args) {

        try{

            //initialize registry
            Registry registry = LocateRegistry.createRegistry(1099);
            //initialize homepage
            IHomePage homePage = new HomePage(registry);

            //bind homepage to registry
            registry.rebind("home", homePage);
            System.out.println("Checkers RMI Server is running...");
        } catch (Exception e) { // if homepage fails to initialize, close server
            System.out.println("Error during server initialization, exiting now");
            System.exit(-1);
        }
    }
}
