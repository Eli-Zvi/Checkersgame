package checkersgame.server;

import checkersgame.common.*;

import javax.crypto.SecretKey;

import java.rmi.AccessException;
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

//TODO ADD DOCUMENTATION AND HEARTBEAT
public class HomePage extends UnicastRemoteObject implements IHomePage {
    private static Registry registry = null;
    private final UserManager userManager;
    private final DatabaseManager databaseManager;
    private final Map<UUID, CheckersGame> onGoingGames = new HashMap<>();
    private final Queue<IClientCallBack> gameQueue = new ConcurrentLinkedQueue<>();
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
        heartbeatMonitor();
    }

    @Override // add return so the user will have a "waiting" message
    public void joinGame(IClientCallBack client) throws RemoteException{
        if(userManager.existsByCallback(client) && !gameQueue.contains(client) &&
                userManager.findByCallback(client).getGameUUID() == null) {
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

       CheckersGame game = null;
       UUID gameID = UUID.randomUUID();

       UUID player1UUID = null, player2UUID = null;

       try(Connection connection = databaseManager.getConnection()) {

           Player player1 = userManager.findByCallback(client1);
           Player player2 = userManager.findByCallback(client2);

           game = new CheckersGame(player1, player2, gameID);
           registry.rebind(gameID.toString(), game);

           player1UUID = player1.getPlayerUUID();
           player2UUID = player2.getPlayerUUID();
           databaseManager.addGame(connection, gameID, player1UUID, player2UUID);

           client1.sendGameID(gameID.toString());
           client2.sendGameID(gameID.toString());

           player1.setGameUUID(gameID);
           player2.setGameUUID(gameID);

           System.out.println("New game created ID: " + gameID + " " + player1.getUsername() + " vs " + player2.getUsername());
           onGoingGames.put(gameID, game);
       }catch (SQLException | RemoteException e){
           boolean player1Disconnected = false;

           try {
               client1.sendHeartbeat();
               gameQueue.offer(client1);
           }catch (RemoteException ignored){
               player1Disconnected = true;
           }

           try {
               client2.sendHeartbeat();
               gameQueue.offer(client2);
           }catch (RemoteException ignored){
           }

           try {
               if (game != null) {
                   registry.unbind(gameID.toString());
                   game = null;

                   try(Connection connection = databaseManager.getConnection()){
                       databaseManager.updateGame(connection, gameID,
                               player1Disconnected ? player2UUID : player1UUID,
                               player1Disconnected ? player1UUID : player2UUID);
                   }catch (SQLException ignored){}
               }
           } catch (RemoteException | NotBoundException ignored){}
       }
    }

    @Override
    public void registerCallBack(IClientCallBack client, String encryptedAESKey) throws RemoteException {
        try {
            SecretKey key = KeyUtils.rsaDecrypt(encryptedAESKey, privateKey);
            Player player = new Player(client, key);
            userManager.addClient(client, player);
        } catch (GeneralSecurityException e) {
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
                player.registerPlayer(username, uuid);
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
            player.registerPlayer(username, playerUUID);
            System.out.println("Player " + username + " has successfully logged in");
            return playerUUID;
        }
    }

    /**
     * Returns the server's public key
     * @return the server's public key in string form
     */
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
    public synchronized static void cleanUp(UUID gameID, UUID winner, UUID loser) throws SQLException {
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

    /**
     * <p>Adds a move with the given parameters to the MOVES table</p>
     * for more details see DatabaseManager's addMove method
     * @return the latest moveNumber
     */
    public static int addMove(UUID gameID, UUID playerID, int moveNumber, LinkedList<MoveInfo> move, boolean promotion)
            throws SQLException{
        DatabaseManager instance = DatabaseManager.getInstance();

        try(Connection connection = instance.getConnection()) {
            return instance.addMove(connection, gameID, playerID, moveNumber, move, promotion);
        }
    }

    /**
     * Fetches a list of FinishedGames from the database and returns it
     * @return a list containing FinishedGames or an empty list
     */
    @Override
    public ArrayList<FinishedGame> getReplayableGames() throws RemoteException{
        try(Connection connection = databaseManager.getConnection()) {
            return databaseManager.getFinishedGameIDs(connection);
        }catch (SQLException e){
            return new ArrayList<>();
        }
    }

    /**
     * Fetches a list of moves from the database and returns it
     * @param gameID the UUID of the game to fetch the moves of from the database
     * @return a list containing the moves obtained from the database, null if an error occurs or the game does not have moves
     */
    @Override
    public ArrayList<LinkedList<MoveInfo>> getReplayMoves(UUID gameID) throws RemoteException {
        try(Connection connection = databaseManager.getConnection()) {
            return databaseManager.getGamesMoves(connection, gameID);
        } catch (SQLException e) {
           return null;
        }
    }

    public void heartbeatMonitor(){
        new Thread(() -> {
            System.out.println("im here");
            while(true){
                try{
                    Thread.sleep(10000);

                    userManager.getCallbacks().removeIf(key -> {
                        try{
                            key.sendHeartbeat();
                            return false;
                        }catch (RemoteException e){
                            Player player = userManager.findByCallback(key);

                            if(player.isLoggedIn()) {
                                gameQueue.remove(key);

                                if(player.getGameUUID() != null)
                                    onGoingGames.get(player.getGameUUID()).forfeitGame(key);

                                try (Connection connection = databaseManager.getConnection()) {
                                    databaseManager.disconnectUser(connection, player.getPlayerUUID());
                                } catch (SQLException ignored) {
                                }

                                String username = player.getUsername();
                                System.out.println("Player:" + " " + username + " has disconnected");
                            }else System.out.println("unregistered user has disconnected");

                            return true;
                        }
                    });
                }catch(InterruptedException exception){
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}
