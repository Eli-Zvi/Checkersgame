package checkersgame.server;

import checkersgame.common.HomePage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * <b>This is the main class of the CheckersGameImpl Server</b>
 *
 * <p>The class is responsible for initializing the server side of the CheckersGameImpl</p>
 *
 * @author Ilay Zvi
 */
public class ServerMain {
    public static void main(String[] args) {
        try{
            String hostname = System.getenv("DOCKER_ENV") != null ? "host.docker.internal" : "localhost";
            //set host based on the environment that is running it, if docker is running it, it will set it to the host's ip
            System.setProperty("java.rmi.server.hostname", hostname);
            //initialize registry
            Registry registry = LocateRegistry.createRegistry(1099);
            //initialize homepage
            HomePage homePage = new HomePageImpl(registry);

            //bind homepage to registry
            registry.rebind("home", homePage);
            System.out.println("Checkers RMI Server is running...");
        } catch (Exception e) { // if homepage fails to initialize, close server
            e.printStackTrace();
            System.out.println("Error during server initialization, exiting now");
            System.exit(-1);
        }
    }
}
