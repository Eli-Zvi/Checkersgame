package checkersgame.server;

import checkersgame.common.IHomePage;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

//TODO ADD DOCUMENTATION

public class ServerMain {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            IHomePage homePage = new HomePage(registry);
            registry.rebind("home", homePage);
            System.out.println("Checkers RMI Server is running...");
        } catch (Exception e) {
            System.out.println("Error during server initialization, exiting now");
        }
    }
}
