package checkersgame.server;

import checkersgame.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>The homepage part of the server, it serves as the landing page for a client during and post connection establishment
 * its responsibilities include the following:</p>
 * <ol>
 *     <li>Key exchange with the new client</li>
 *     <li>Adding the client to the user manager's client list</li>
 *     <li>Client registration and log in</li>
 *     <li>Communication with DatabaseManager for database queries</li>
 *     <li>Game Queue management and on going games management</li>
 *     <li>Initializing games</li>
 *     <li>Handling terminated connections</li>
 * </ol>
 */
public class HomePageImpl extends UnicastRemoteObject implements HomePage {
    private final static Logger logger = LoggerFactory.getLogger(HomePageImpl.class);
    private static Registry registry = null;
    private final UserManager userManager;
    private final DatabaseManager databaseManager;
    private static final Map<UUID, CheckersGameImpl> onGoingGames = new ConcurrentHashMap<>();
    private final Queue<ClientCallBack> gameQueue = new ConcurrentLinkedQueue<>();
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final int HEARTBEAT_INTERVAL = 15000; //15 seconds

    private final ReentrantLock gameInitLock = new ReentrantLock();
    private final Condition condition = gameInitLock.newCondition();

    /**
     * Initializes the server's database, user manager, RSA keys, gameInitializer loop and heartbeat monitor
     * @param registry the registry the homepage is registered with
     * @throws SQLException if database initialization is unsuccessful and will shut the server down
     */
    protected HomePageImpl(Registry registry) throws RemoteException, NoSuchAlgorithmException, SQLException{
        super(1099);
        databaseManager = DatabaseManager.getInstance();
        userManager = new UserManager();
        HomePageImpl.registry = registry;
        KeyPair rsaPair = KeyUtils.generateRSAKeyPair();
        publicKey = rsaPair.getPublic();
        privateKey = rsaPair.getPrivate();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(this::gameInitializer);
        heartbeatMonitor();
    }

    /**
     * <p>Initial key exchange between the client and the server</p>
     * @param client the client the server is exchanging the keys with
     * @param encryptedAESKey the client's encrypted AES key
     */
    @Override
    public void registerCallBack(ClientCallBack client, String encryptedAESKey) throws RemoteException {
        try {
            SecretKey key = KeyUtils.rsaDecrypt(encryptedAESKey, privateKey);
            Player player = new Player(client, key);
            userManager.addClient(client, player);
        } catch (GeneralSecurityException e) {
            // throw an exception to client that will disconnect them
            logger.error("An error has occurred during client callback registration");
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>Heartbeat protocol implementation, makes sure that the client is still connected every HEARTBEAT_INTERVAL milliseconds</p>
     * <p>If the client disconnects, handles termination by doing the following:</p>
     * <ul>
     *     <li>Removes the user from the user manager</li>
     *     <li>
     *         If the player is logged in:
     *         <ul>
     *             <li>Removes the user from the game queue if they joined it</li>
     *             <li>If the player is in a game during disconnection forfeits the game as the player</li>
     *             <li>Disconnects their account in the database to prevent account lock out</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    private void heartbeatMonitor(){
        new Thread(() -> {
            while(true){
                try{
                    Thread.sleep(HEARTBEAT_INTERVAL); //doesn't want or need to be notified

                    //remove user from userManager
                    userManager.getCallbacks().removeIf(key -> {
                        try{
                            key.sendHeartbeat(); //send heartbeat
                            return false;
                        }catch (RemoteException e){
                            Player player = userManager.findByCallback(key);
                            if(player.isLoggedIn()) {
                                gameQueue.remove(key); //if the user is logged in and in the queue for a game, remove him

                                //if the player is already in a game, forfeit the game
                                if(player.getGameUUID() != null)
                                    onGoingGames.get(player.getGameUUID()).forfeitGame(key);

                                //change the status of the user to disconnected in the database, so they don't get locked out
                                try (Connection connection = databaseManager.getConnection()) {
                                    databaseManager.disconnectUser(connection, player.getPlayerUUID());
                                } catch (SQLException ignored) {
                                }

                                String username = player.getUsername();

                                logger.info("Player: {} has disconnected", username);
                            }else logger.info("unregistered user has disconnected");

                            return true;
                        }
                    });
                }catch(InterruptedException exception){
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    /**
     * Adds the client to the game queue if they are not in a game, notifies gameInitializer
     * @param client the client requesting to join the game queue
     */
    @Override
    public void joinGame(ClientCallBack client) throws RemoteException{
        if(userManager.existsByCallback(client) && !gameQueue.contains(client) &&
                userManager.findByCallback(client).getGameUUID() == null) {
            gameQueue.offer(client);
        }

        if(gameQueue.size() >= 2) {
            if(gameInitLock.tryLock()){ //try to get lock, if it's already taken by gameInitializer or a different player continue
                try{
                    condition.signalAll(); //signal gameInitializer
                }finally {
                    gameInitLock.unlock();
                }
            }
        }
    }

    /**
     * Game initialization loop, wakes up on client queue join and checks if there are 2 players in the queue, initializes a new
     * game with if there are
     */
    private void gameInitializer(){
        gameInitLock.lock();
        while(true){
            while(gameQueue.size() < 2){
                try{
                    condition.await(); //release lock
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }

            initializeGame(); //initialize game with the first 2 players in the queue
        }
    }

    /**
     * Initializes a new game with the two players at the top of the queue to it,
     * notifies the players once initialization is finished
     */
    private void initializeGame(){

        //get both players from queue
       ClientCallBack client1 = gameQueue.poll();
       ClientCallBack client2 = gameQueue.poll();

       if(client1 == null || client2 == null){
           if (client1 == null && client2 == null){}
           else gameQueue.offer(Objects.requireNonNullElse(client1, client2)); //offers the non-null out of the 2
           return;
       }

       CheckersGameImpl game = null;
       UUID gameID = UUID.randomUUID();

       UUID player1UUID, player2UUID;

       try(Connection connection = databaseManager.getConnection()) {

           Player player1 = userManager.findByCallback(client1);
           Player player2 = userManager.findByCallback(client2);

           game = new CheckersGameImpl(player1, player2, gameID);
           registry.rebind(gameID.toString(), game);

           player1UUID = player1.getPlayerUUID();
           player2UUID = player2.getPlayerUUID();
           databaseManager.addGame(connection, gameID, player1UUID, player2UUID);

           client1.sendGameID(gameID.toString());
           client2.sendGameID(gameID.toString());

           player1.setGameUUID(gameID);
           player2.setGameUUID(gameID);

           logger.info("New game created with ID: {}, between {} and {}", gameID, player1.getUsername(), player2.getUsername());
           onGoingGames.put(gameID, game);
       }catch (SQLException | RemoteException e){
           //if one of the players disconnected during game initialization or game initialization failed

           try {
               client1.sendHeartbeat(); //if heartbeat successful replace into the queue
               gameQueue.offer(client1);
           }catch (RemoteException ignored){
           }

           try {
               client2.sendHeartbeat(); //if heartbeat successful replace into the queue
               gameQueue.offer(client2);
           }catch (RemoteException ignored){
           }

           try {
               if (game != null) { //if game was initialized unbind it, and delete it from database
                   registry.unbind(gameID.toString());
                   game = null;

                   try(Connection connection = databaseManager.getConnection()){
                       databaseManager.deleteGame(connection, gameID);
                       logger.info("Game with ID: {} has been successfully deleted after one or more players have disconnected", gameID);
                   }catch (SQLException ignored){}
               }
           } catch (RemoteException | NotBoundException ignored){}
       }
    }

    /**
     * Attempts to register the user in and returns his new UUID if successful
     * @param client client attempting to register
     * @param username the username the client is attempting to register with
     * @param password the password the client is attempting to register with
     * @return the user's UUID if login is successful, null otherwise
     */
    @Override
    public synchronized UUID register(ClientCallBack client, String username, Password password) throws RemoteException, SQLException
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
                logger.info("Player {} has successfully registered and logged in", username);
                return uuid;
            }
        }catch (GeneralSecurityException e){
            return null;
        }
    }

    /**
     * Attempts to log the user in and returns his UUID if successful
     * @param client client attempting to login
     * @param username the username the client is attempting to log in with
     * @param password the password the client is attempting to log in with
     * @return the user's UUID if login is successful, null otherwise
     */
    @Override
    public UUID login(ClientCallBack client, String username, Password password) throws RemoteException,
            SQLException, GeneralSecurityException{

        Player player = userManager.findByCallback(client);

        if (player == null){
            throw new RemoteException(); //catch on client side and display error message and tell them to relog
        }

        try(Connection connection = databaseManager.getConnection()) {
            UUID playerUUID = databaseManager.verifyLogin(connection, username, password.decrypt(player.getKey()));
            player.registerPlayer(username, playerUUID);
            logger.info("Player {} has successfully logged in", username);
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

    /**
     * Fetches and returns the given player's WINS and LOSSES in the form of a string if they exist
     * @param client the client requesting their win rate loss
     * @return a string containing the wins and losses of the player in the format of WINS-LOSSES
     */
    @Override
    public String getWinRate(ClientCallBack client) throws RemoteException {
        try(Connection connection = databaseManager.getConnection()) {
            return databaseManager.getPlayerWinRate(connection, userManager.findByCallback(client).getPlayerUUID());
        } catch (SQLException e) {
            logger.error("An error has occurred while fetching client's win rate, client ID: {}", userManager.findByCallback(client).getPlayerUUID());
            return "Error getting win rate";
        }
    }

    /**
     * <p>Performs a clean up after the game is over</p>
     * <ul>
     *     <li>Removes the game from the onGoingGames</li>
     *     <li>Unbinds itself from the RMI registry</li>
     *     <li>Updates the status of the game and the players</li>
     * </ul>
     * @param gameID the UUID of the game attempting to clean up after itself
     * @param winner the winner of the game
     * @param loser the loser of the game
     */
    protected static void cleanUp(UUID gameID, UUID winner, UUID loser) throws SQLException {
        try {
            onGoingGames.remove(gameID); // remove game from ongoing gamse

            registry.unbind(gameID.toString()); //remove game from registry

            registry.lookup(gameID.toString()); //look it up to trigger an exception

            // If no exception is thrown, the game is still bound
            logger.error("Game {} is still bound in the registry.", gameID);
        } catch (NotBoundException | RemoteException | NullPointerException e) {
            // If NotBoundException is thrown, the game is no longer bound
            DatabaseManager instance = DatabaseManager.getInstance();

            try(Connection connection = instance.getConnection()) {
                instance.updateGame(connection, gameID, winner, loser);
            }

            logger.info("Game {} has been successfully unbound from the registry.", gameID);
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
}
