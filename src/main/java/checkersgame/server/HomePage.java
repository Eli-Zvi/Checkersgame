package checkersgame.server;

import checkersgame.common.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

//TODO ADD DOCUMENTATION AND LOCKS AND HEARTBEAT AND NEW CLASS FOR USER MANAGEMENT
public class HomePage extends UnicastRemoteObject implements IHomePage {
    private static Registry registry = null;
    private final UserManager userManager;
    private final DatabaseManager databaseManager;
    //private final ArrayList<CheckersGame> onGoingGames = new ArrayList<>(); <- make a map between player -> checkersgame
    private final Queue<IClientCallBack> gameQueue = new ConcurrentLinkedQueue<>(); //change to player later
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final ReentrantLock gameInitLock = new ReentrantLock();

    protected HomePage(Registry registry) throws RemoteException, NoSuchAlgorithmException, SQLException{
        super();
        databaseManager = DatabaseManager.getInstance();
        userManager = new UserManager();
        HomePage.registry = registry;
        KeyPair rsaPair = KeyUtils.generateRSAKeyPair();
        publicKey = rsaPair.getPublic();
        privateKey = rsaPair.getPrivate();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(this::gameInitializer);
    }

    @Override // add return so the user will have a "waiting" message
    public void joinGame(IClientCallBack client) throws RemoteException{
        if(userManager.existsByCallback(client) && !gameQueue.contains(client)) {
            gameQueue.offer(client);
        }

        synchronized (gameInitLock) {
            gameInitLock.notifyAll();
        }
    }

    private void gameInitializer(){
        while(true){
            synchronized (gameInitLock){

                while(gameQueue.size() < 2){
                    try{
                        gameInitLock.wait();
                    }catch (InterruptedException e){
                        Thread.currentThread().interrupt();
                    }
                }

                initializeGame();
            }
        }
    }

    private void initializeGame(){

       IClientCallBack client1 = gameQueue.poll();
       IClientCallBack client2 = gameQueue.poll();

       if(client1 == null || client2 == null){
           if (client1 == null && client2 == null){
               //both had an issue notify both
           }else if(client1 == null){
               //only client1 had an issue
               gameQueue.offer(client2);
           }else{
               //only client2 had an issue
               gameQueue.offer(client1);
           }
           return;
       }

       UUID gameID = UUID.randomUUID();
       Player player1 = userManager.findByCallback(client1);
       Player player2 = userManager.findByCallback(client2);

        try(Connection connection = databaseManager.getConnection()) {
            databaseManager.addGame(connection, gameID, player1.getPlayerUUID(), player2.getPlayerUUID());

            CheckersGame game = new CheckersGame(player1, player2, gameID);
            registry.rebind(gameID.toString(), game);
            client1.sendGameID(gameID.toString());
            client2.sendGameID(gameID.toString());
            System.out.println("New game created ID: " + gameID + " " + player1.getName() + " vs " + player2.getName());
            //onGoingGames.add(game);
        }catch (SQLException | RemoteException e){
            //TODO SEND CALLBACK TO CLIENT TO NOTIFY THEM THAT AN ERROR HAS OCCURRED DURING GAME INIT
        }
    }

    @Override
    public void registerCallBack(IClientCallBack client, String encryptedAESKey) throws RemoteException {
        try {
            SecretKey key = KeyUtils.rsaDecrypt(encryptedAESKey, privateKey);
            Player player = new Player(null, null, client, key);
            userManager.addClient(client, player);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized UUID register(IClientCallBack client, String username, Password password) throws RemoteException, SQLException
    {
        try {
            Player player = userManager.findByCallback(client);
            if(player == null || player.isLoggedIn()) {
                return null;
            }
            UUID uuid = UUID.randomUUID();
            try(Connection connection = databaseManager.getConnection()) {
                databaseManager.addPlayer(connection, uuid, username, password.decrypt(player.getKey()));
                player.setPlayerUUID(uuid);
                player.setName(username);
                player.setLoggedIn(true);
                System.out.println("Player " + username + " has successfully registered and logged in");
                return uuid;
            }
        }catch (GeneralSecurityException e){
            return null;
        }
    }

    @Override //pull uuid from table and send to user
    public UUID login(IClientCallBack client, String username, Password password) throws RemoteException,
            SQLException, GeneralSecurityException{

        Player player = userManager.findByCallback(client);

        if (player == null){
            throw new RemoteException(); //catch on client side and display error message and tell them to relog
        }

        try(Connection connection = databaseManager.getConnection()) {
            UUID playerUUID = databaseManager.verifyLogin(connection, username, password.decrypt(player.getKey()));
            System.out.println("im here");
            player.setPlayerUUID(playerUUID);
            player.setName(username);
            player.setLoggedIn(true);
            System.out.println("Player " + username + " has successfully logged in");
            return playerUUID;
        }
    }

    @Override
    public String getServerPublicKey() throws RemoteException {
        return KeyUtils.publicKeyToBase64(publicKey);
    }

    @Override
    public String getWinRate(IClientCallBack client) throws RemoteException {
        try(Connection connection = databaseManager.getConnection()) {
            return databaseManager.getPlayerWinRate(connection, userManager.findByCallback(client).getPlayerUUID());
        } catch (SQLException e) {
            return "Error getting win rate";
        }
    }

    //sql update etc.
    public synchronized static void cleanUp(UUID gameID, UUID winner, UUID loser) throws RemoteException, SQLException {
        try {
            registry.unbind(gameID.toString());

            registry.lookup(gameID.toString());
            // If no exception is thrown, the game is still bound
            System.out.println("Game " + gameID + " is still bound in the registry.");
        } catch (NotBoundException | RemoteException | NullPointerException e) {
            // If NotBoundException is thrown, the game is no longer bound
            DatabaseManager instance = DatabaseManager.getInstance();

            try(Connection connection = instance.getConnection()) {
                instance.updateGame(connection, gameID, winner, loser);
            }
            System.out.println("Game " + gameID + " has been successfully unbound from the registry.");
        }
    }

    public static int addMove(UUID gameID, UUID playerID, int moveNumber, LinkedList<MoveInfo> move, boolean promotion)
            throws SQLException{
        DatabaseManager instance = DatabaseManager.getInstance();

        try(Connection connection = instance.getConnection()) {
            return instance.addMove(connection, gameID, playerID, moveNumber, move, promotion);
        }
    }

    @Override
    public ArrayList<FinishedGame> getReplayableGames() throws RemoteException{
        try(Connection connection = databaseManager.getConnection()) {
            return databaseManager.getFinishedGameIDs(connection);
        }catch (SQLException e){
            return new ArrayList<>();
        }
    }

    @Override
    public ArrayList<LinkedList<MoveInfo>> getReplayMoves(UUID gameID) throws RemoteException {
        try(Connection connection = databaseManager.getConnection()) {
            return databaseManager.getGamesMoves(connection, gameID);
        } catch (SQLException e) {
           return null;
        }
    }
}
