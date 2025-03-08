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
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
//TODO ADD DOCUMENTATION AND LOCKS AND HEARTBEAT AND NEW CLASS FOR USER MANAGEMENT
public class HomePage extends UnicastRemoteObject implements IHomePage {
    private static Registry registry = null;
    private static DatabaseManager databaseManager;
    private final UserManager userManager;
    //private final ArrayList<CheckersGame> onGoingGames = new ArrayList<>(); <- make a map between player -> checkersgame
    private final Queue<IClientCallBack> gameQueue = new LinkedList<>(); //change to player later
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    protected HomePage(Registry registry) throws RemoteException, NoSuchAlgorithmException, SQLException {
        super();
        DatabaseManager.initializeInstance();
        databaseManager = DatabaseManager.getInstance();
        userManager = new UserManager();
        HomePage.registry = registry;
        KeyPair rsaPair = KeyUtils.generateRSAKeyPair();
        publicKey = rsaPair.getPublic();
        privateKey = rsaPair.getPrivate();
    }

    @Override // add return so the user will have a "waiting" message
    public synchronized void joinGame(IClientCallBack client) throws RemoteException, SQLException {
        if(userManager.existsByCallback(client) && !gameQueue.contains(client)) {
            gameQueue.add(client);
        }
        if(gameQueue.size() >= 2){
            initializeGame();
        }
    }

    private synchronized void initializeGame() throws RemoteException, SQLException{
        IClientCallBack client1 = gameQueue.poll();
        IClientCallBack client2 = gameQueue.poll();
        assert client1 != null && client2 != null;
        UUID gameID = UUID.randomUUID();
        Player player1 = userManager.findByCallback(client1);
        Player player2 = userManager.findByCallback(client2);
        databaseManager.addGame(gameID, player1.getPlayerUUID(), player2.getPlayerUUID());
        CheckersGame game = new CheckersGame(player1, player2, gameID);
        registry.rebind(gameID.toString(), game);
        client1.sendGameID(gameID.toString());
        client2.sendGameID(gameID.toString());
        System.out.println("New game created ID: " + gameID + " " + player1.getName() + " vs " + player2.getName());
        //onGoingGames.add(game);
    }

    @Override
    public synchronized void registerCallBack(IClientCallBack client, String encryptedAESKey) throws RemoteException {
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
            databaseManager.addPlayer(uuid, username, password.decrypt(player.getKey()));
            player.setPlayerUUID(uuid);
            player.setName(username);
            player.setLoggedIn(true);
            System.out.println("Player " + username + " has successfully registered and logged in");
            return uuid;
        }catch (GeneralSecurityException e){
            return null;
        }
    }

    @Override //pull uuid from table and send to user
    public synchronized UUID login(IClientCallBack client, String username, Password password) throws RemoteException,
            SQLException, GeneralSecurityException{
        Player player = userManager.findByCallback(client);
        if (player == null){
            throw new RemoteException(); //catch on client side and display error message and tell them to relog
        }
        UUID playerUUID = databaseManager.verifyLogin(username, password.decrypt(player.getKey()));
        player.setPlayerUUID(playerUUID);
        player.setName(username);
        player.setLoggedIn(true);
        System.out.println("Player " + username + " has successfully logged in");
        return playerUUID;
    }

    @Override
    public String getServerPublicKey() throws RemoteException {
        return KeyUtils.publicKeyToBase64(publicKey);
    }

    @Override
    public String getWinRate(IClientCallBack client) throws RemoteException {
        try {
            return databaseManager.getPlayerWinRate(userManager.findByCallback(client).getPlayerUUID());
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
            databaseManager.updateGame(gameID, winner, loser);
            System.out.println("Game " + gameID + " has been successfully unbound from the registry.");
        }
    }

    public synchronized static int addMove(UUID gameID, UUID playerID, int moveNumber, LinkedList<MoveInfo> move, boolean promotion) throws SQLException{
        return databaseManager.addMove(gameID, playerID, moveNumber, move, promotion);
    }

    @Override
    public synchronized ArrayList<FinishedGame> getReplayableGames() throws RemoteException{
        try {
            return databaseManager.getFinishedGameIDs();
        }catch (SQLException e){
            return new ArrayList<>();
        }
    }

    @Override
    public ArrayList<LinkedList<MoveInfo>> getReplayMoves(UUID gameID) throws RemoteException {
        try {
            return databaseManager.getGamesMoves(gameID);
        } catch (SQLException e) {
           return null;
        }
    }
}
