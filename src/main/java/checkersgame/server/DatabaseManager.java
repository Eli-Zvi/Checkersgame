package checkersgame.server;

import checkersgame.common.FinishedGame;
import checkersgame.common.MoveInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

/**
 * <p>This singleton class manages the connection with a datasource, and distributes connections according to requests</p>
 *
 * <p>The class allows requesters to update and add rows to the tables in the database after obtaining a connection</p>
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static HikariDataSource ds;
    private static DatabaseManager instance;
    private final int MAX_POOL_SIZE = 20;
    private final int CONNECTION_TIMEOUT = 30000; //30 seconds

    /**
     * Initializes instance of DatabaseManager and calls onto initializeDatabase
     * @throws SQLException if an error occurs during initialization
     */
    private DatabaseManager() throws SQLException {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(System.getenv("DB_URL"));
        config.setUsername(System.getenv("DB_USERNAME"));
        config.setPassword(System.getenv("DB_PASS"));

        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setIdleTimeout(CONNECTION_TIMEOUT); // return connection to pool after one cycle
        config.setConnectionTimeout(CONNECTION_TIMEOUT * 2); // wait upto 2 cycles until throwing an exception

        ds = new HikariDataSource(config);

        initializeDatabase();
    }

    /**
     * <p>Initializes The Database</p>
     * <ol>
     *     <li>Initializes tables</li>
     *     <li>Sets all player's status to offline</li>
     * </ol>
     * @throws SQLException if an error occurs during database initialization
     */
    private void initializeDatabase() throws SQLException{
        Connection connection = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);

            createTables(connection);

            try(Statement stmt = connection.createStatement()) {
                String setOffline = "UPDATE PLAYERS SET LOGGED_IN = FALSE";
                stmt.executeUpdate(setOffline);
            }

            connection.commit();
            logger.info("DB connection established successfully");
        }catch (SQLException e){
            logger.error("connection failed to initialize");
            if (connection != null)
                connection.rollback();

            throw e;
        }
    }

    /**
     * Creates a new instance of DatabaseManager if necessary, and returns it
     * @return the singleton instance of DatabaseManager
     */
    public synchronized static DatabaseManager getInstance() throws SQLException{
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Establishes a connection with datasource and returns it
     * Hikari thread pooling is thread safe
     * @return get a connection from the connection pool
     */
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    /**
     * Creates the tables that do not exist in the database
     * @param connection an established connection with the database
     */
    private static synchronized void createTables(Connection connection) throws SQLException {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS PLAYERS (" +
                "UUID CHAR(36) PRIMARY KEY, " + // UUID is 16 bytes -> 36 bytes as string form
                "USERNAME VARCHAR(16) NOT NULL UNIQUE, " + // allowing user upto 16 char long username
                "PASSWORD BINARY(60) NOT NULL, " +
                "SALT CHAR(24) NOT NULL," +
                "WINS INT UNSIGNED DEFAULT 0, " + //user won't have more than 4 billion wins
                "LOSSES INT UNSIGNED DEFAULT 0, " + // user won't have more than 4 billion losses
                "LOGGED_IN BINARY(1) DEFAULT TRUE, " + // LOGGED_IN - 0 = OFFLINE 1 = ONLINE
                "STATUS BINARY(1) DEFAULT FALSE" + // STATUS - 0 - NOT IN-GAME 1 - IN-GAME
                ")";

        String createGamesTable = "CREATE TABLE IF NOT EXISTS GAMES (" + //maybe add registry ID to table
                "ID CHAR(36) UNIQUE NOT NULL PRIMARY KEY, " + // PRIMARY KEY ID
                "PLAYER1_UUID CHAR(36) NOT NULL, " + // PLAYER1 UUID
                "PLAYER2_UUID CHAR(36) NOT NULL, " + // PLAYER2 UUID
                "STATUS BINARY(1) DEFAULT FALSE, " + // STATUS -  0 = ONGOING, 1 = CONCLUDED
                "WINNER_UUID CHAR(36), " + //UUID OF WINNER (NULL INITIALLY)
                "CONSTRAINT FK_PLAYER1 FOREIGN KEY (PLAYER1_UUID) REFERENCES players(UUID)," + //ensures player exists in players table
                "CONSTRAINT FK_PLAYER2 FOREIGN KEY (PLAYER2_UUID) REFERENCES players(UUID)," + //ensures player exists in players table
                "CONSTRAINT FK_WINNER FOREIGN KEY (WINNER_UUID) REFERENCES players(UUID)" + //ensures player exists in players table
                ")";

        String createMoveTable = "CREATE TABLE IF NOT EXISTS MOVES (" +
                "ID INT UNSIGNED AUTO_INCREMENT PRIMARY KEY," +
                "GAME_ID CHAR(36) NOT NULL," +
                "MOVE_NUMBER SMALLINT UNSIGNED NOT NULL," +
                "PLAYER_UUID CHAR(36) NOT NULL," +
                "FROM_POSITION VARCHAR(3) NOT NULL," +
                "TO_POSITION VARCHAR(3) NOT NULL," +
                "PROMOTION BINARY(1) DEFAULT FALSE," +
                "CONSTRAINT FK_GAME FOREIGN KEY (GAME_ID) REFERENCES GAMES(ID)," +
                "CONSTRAINT FK_PLAYER FOREIGN KEY (PLAYER_UUID) REFERENCES PLAYERS(UUID)," +
                "UNIQUE(GAME_ID, MOVE_NUMBER)" + //make sure we have a unique number for each game
                ")";

        try(Statement statement = connection.createStatement()) {
            statement.addBatch(createPlayersTable);
            statement.addBatch(createGamesTable);
            statement.addBatch(createMoveTable);

            statement.executeBatch();
        }
    }

    /**
     * Adds a new instance of a player to the PLAYERS table if it doesn't exist
     * @param connection an established connection with the database
     * @param uuid the player's uuid
     * @param username the player's username
     * @param password the player's password
     */
    public synchronized void addPlayer(Connection connection, UUID uuid, String username, String password) throws SQLException,
            NoSuchAlgorithmException, InvalidKeySpecException {
        //salt and hash password before storing it
        String salt = PasswordUtils.generateSalt();
        byte[] hashedPass = PasswordUtils.hashPassword(password, salt);

        String addPlayer = "INSERT INTO PLAYERS (UUID, USERNAME, PASSWORD, SALT, WINS, LOSSES, LOGGED_IN, STATUS) " +
                "VALUES ( ?, ?, ?, ?, 0, 0, TRUE, FALSE)";
        try(PreparedStatement statement = connection.prepareStatement(addPlayer)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, username);
            statement.setBytes(3, hashedPass);
            statement.setString(4, salt);
            statement.executeUpdate();
            logger.info("New player registered with name: {} and uuid: {}", username, uuid);
        }
    }

    /**
     * Verifies login of a player
     * @param connection an established connection with the database
     * @param username the player's username
     * @param password the player's password
     * @return the UUID if the login was valid
     * @throws SQLException if verification is unsuccessful(wrong password or player is logged in)
     * or the player with the given info does not exist
     */
    public UUID verifyLogin(Connection connection, String username, String password) throws SQLException,
            NoSuchAlgorithmException, InvalidKeySpecException {

        connection.setAutoCommit(false);
        String selectPlayer = "SELECT UUID, PASSWORD, SALT, LOGGED_IN FROM PLAYERS WHERE USERNAME = ? FOR UPDATE";
        try(PreparedStatement statement = connection.prepareStatement(selectPlayer)) {
            //lock table

            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next() && !resultSet.getBoolean("LOGGED_IN")) {
                //salt and hash given password to compare it
                String salt = resultSet.getString("SALT");
                byte[] hashedPassword = resultSet.getBytes("PASSWORD");
                //compare password that is in table(hashed and salted) with the given password(after salting and hashing)

                if (PasswordUtils.verifyPassword(password, hashedPassword, salt)) {
                    UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                    String updateLogin = "UPDATE PLAYERS SET LOGGED_IN = TRUE WHERE UUID= ?";
                    PreparedStatement winsStatement = connection.prepareStatement(updateLogin);
                    winsStatement.setString(1, uuid.toString());
                    winsStatement.executeUpdate();
                    connection.commit();
                    return uuid;
                }
            }
            throw new SQLException("Invalid username or password");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Updates the given player's LOGGED_IN to FALSE
     * @param connection an established connection with the database
     * @param player the UUID of the player to disconnect
     */
    public void disconnectUser(Connection connection, UUID player) throws SQLException{
        connection.setAutoCommit(false);
        String selectPlayer = "UPDATE PLAYERS SET LOGGED_IN = FALSE WHERE UUID = ?";

        try(PreparedStatement statement = connection.prepareStatement(selectPlayer)){

            statement.setString(1, player.toString());

            //lock table
            lockPlayers(connection, player, null);

            statement.executeUpdate();

            connection.commit();
        }catch (SQLException e){
            logger.error("An error has occurred during disconnection of user with ID: {}", player);
            connection.rollback();
        }
        finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Adds a new game with the given parameters to the GAMES table, and update the player's status(NOT INGAME -> INGAME)
     * @param connection an established connection with the database
     * @param gameUUID the game's UUID
     * @param player1UUID player1's UUID
     * @param player2UUID player2's UUID
     */
    public void addGame(Connection connection, UUID gameUUID, UUID player1UUID, UUID player2UUID)
            throws SQLException {

        String addGame = "INSERT INTO GAMES (ID, PLAYER1_UUID, PLAYER2_UUID, STATUS, WINNER_UUID) " +
                "VALUES (?, ?, ?, FALSE, NULL)";
        String updateStatus1 = "UPDATE PLAYERS SET STATUS = TRUE WHERE UUID= ?";
        String updateStatus2 = "UPDATE PLAYERS SET STATUS = TRUE WHERE UUID= ?";

        connection.setAutoCommit(false);

        try(PreparedStatement statement = connection.prepareStatement(addGame);
            PreparedStatement winsStatement = connection.prepareStatement(updateStatus1);
            PreparedStatement lossStatement = connection.prepareStatement(updateStatus2)) {

            lockPlayers(connection, player1UUID, player2UUID); //lock player1 and player2 roles before updating

            statement.setString(1, gameUUID.toString());
            statement.setString(2, player1UUID.toString());
            statement.setString(3, player2UUID.toString());
            statement.executeUpdate();

            winsStatement.setString(1, player1UUID.toString());
            winsStatement.executeUpdate();

            lossStatement.setString(1, player2UUID.toString());
            lossStatement.executeUpdate();

            connection.commit();
            logger.info("New game started with id {}", gameUUID);
        }catch (SQLException e){
            logger.error("An error has occurred while starting game with ID: {}", gameUUID);
            connection.rollback();
            throw e;
        }finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Deletes a game in the GAMES table if it exists
     * @param connection an established connection with the database
     * @param gameUUID the UUID of the game to be removed
     */
    public void deleteGame(Connection connection, UUID gameUUID) throws SQLException{
        String deleteQuery = "DELETE FROM GAMES WHERE ID = ?";

        try(PreparedStatement statement = connection.prepareStatement(deleteQuery)){
            statement.setString(1, gameUUID.toString());

            statement.executeUpdate();
        }
    }

    /**
     * Updates the following at the end of a game:
     * <ul>
     *     <li>The given game's STATUS to CONCLUDED</li>
     *     <li>The given game's WINNER_UUID to the given winner UUID</li>
     *     <li>Both player's STATUS TO NOT INGAME</li>
     *     <li>Increments the winning player's WINS</li>
     *     <li>Increments the losing player's LOSSES</li>
     * </ul>
     * @param connection an established connection with the database
     * @param gameID the game's UUID
     * @param winner the winning player's UUID
     * @param loser the losing player's UUID
     */
    public void updateGame(Connection connection, UUID gameID, UUID winner, UUID loser) throws SQLException {
        String updateGame = "UPDATE GAMES SET STATUS = TRUE, WINNER_UUID = ? WHERE ID = ?";
        String updateWins = "UPDATE PLAYERS SET STATUS = FALSE, WINS = WINS + 1 WHERE UUID= ?";
        String updateLosses = "UPDATE PLAYERS SET STATUS = FALSE, LOSSES = LOSSES + 1 WHERE UUID= ?";

        try(PreparedStatement gameUpdateStatement = connection.prepareStatement(updateGame);
            PreparedStatement winsStatement = connection.prepareStatement(updateWins);
            PreparedStatement lossStatement = connection.prepareStatement(updateLosses)) {
            connection.setAutoCommit(false);

            lockPlayers(connection, winner, loser); //lock player1 and player2 roles before updating

            gameUpdateStatement.setString(1, winner.toString());
            gameUpdateStatement.setString(2, gameID.toString());
            gameUpdateStatement.executeUpdate();


            winsStatement.setString(1, winner.toString());
            winsStatement.executeUpdate();


            lossStatement.setString(1, loser.toString());
            lossStatement.executeUpdate();
            connection.commit();
            logger.info("Game {} has been finalized", gameID);
        }catch (SQLException e){
            logger.error("An error has occurred during the cleanup of game with ID: {}", gameID);
            connection.rollback();
            throw e;
        }finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Locks the rows of player1 and player2 if they exist
     * @param connection an established connection with the database
     * @param player1 the first player's UUID
     * @param player2 the second player's UUID
     */
    public void lockPlayers(Connection connection, UUID player1, UUID player2) throws SQLException{

        if(player1 != null && player2 != null) {
            String lockPlayer = "SELECT * FROM PLAYERS WHERE UUID= ? OR UUID= ? FOR UPDATE";

            try (PreparedStatement lockStatement = connection.prepareStatement(lockPlayer)) {
                lockStatement.setString(1, player1.toString());
                lockStatement.setString(2, player2.toString());
                lockStatement.executeQuery();
            }
        }else if(player2 == null && player1 == null){
        }else{
            lockPlayer(connection, player1 == null ? player2 : player1);
        }
    }

    private void lockPlayer(Connection connection, UUID player) throws SQLException {
        String lockPlayer = "SELECT * FROM PLAYERS WHERE UUID= ? FOR UPDATE";

        try(PreparedStatement lockStatement = connection.prepareStatement(lockPlayer)){
            lockStatement.setString(1, player.toString());
            lockStatement.executeQuery();
        }
    }

    /**
     * Adds a new move to the MOVES table with the given parameter
     * @param connection an established connection with the database
     * @param gameID the game's UUID
     * @param player the player that moved their piece
     * @param moveNumber the move's number in the game
     * @param move the move to be added(could be multiple)
     * @param promotion if the piece that has been moved has been promoted during the move
     * @return the latest moveNumber
     */
    public int addMove(Connection connection, UUID gameID, UUID player, int moveNumber,
                                    LinkedList<MoveInfo> move, boolean promotion) throws SQLException {

        String addMove = "INSERT INTO MOVES (GAME_ID, MOVE_NUMBER, PLAYER_UUID, FROM_POSITION, TO_POSITION, PROMOTION) " +
                "VALUES ( ?, ?, ?, ?, ?, ?)";
        try(PreparedStatement statement = connection.prepareStatement(addMove)) {

            for (MoveInfo moveInfo : move) {
                statement.setString(1, gameID.toString());
                statement.setInt(2, moveNumber++);
                statement.setString(3, player.toString());
                statement.setString(4, moveInfo.currentRow() + "," + moveInfo.currentCol());
                statement.setString(5, moveInfo.newRow() + "," + moveInfo.newCol());
                statement.setBoolean(6, promotion);
                statement.addBatch();
            }

            statement.executeBatch();

            return moveNumber;
        }
    }

    /**
     * Gets and returns the given player's WINS and LOSSES in the form of a string if they exist
     * @param connection an established connection
     * @param playerID a given UUID of a player
     * @return a string containing the wins and losses of the player in the format of WINS-LOSSES
     */
    public String getPlayerWinRate(Connection connection, UUID playerID) throws SQLException {
        String selectPlayer = "SELECT WINS, LOSSES FROM PLAYERS WHERE UUID = ?";
        try(PreparedStatement statement = connection.prepareStatement(selectPlayer)) {
            statement.setString(1, playerID.toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("WINS") + "-" + resultSet.getString("LOSSES");
            }
        }
        return null;
    }

    /**
     * Gets and returns the list of moves played in the given game in the form of a list
     * @param connection an established connection
     * @param gameID a given game's UUID
     * @return a list of moves played in the given game or null if there are no moves related to the game/the game does not exist
     */
    public ArrayList<LinkedList<MoveInfo>> getGamesMoves(Connection connection, UUID gameID)
            throws SQLException { //make sure to check that the game is over first

        String selectMoves = "SELECT FROM_POSITION, TO_POSITION FROM MOVES WHERE GAME_ID = ?";

        try(PreparedStatement statement = connection.prepareStatement(selectMoves)) {
            statement.setString(1, gameID.toString());
            ResultSet resultSet = statement.executeQuery(); //get all rows related to the given game UUID
            ArrayList<LinkedList<MoveInfo>> moves = new ArrayList<>(); //initialize return list

            if (resultSet.next()) {
                LinkedList<MoveInfo> temp = new LinkedList<>(); //create a new list

                MoveInfo move = MoveInfo.fromString(resultSet.getString("FROM_POSITION"),
                        resultSet.getString("TO_POSITION")); //get the move coordinates and create a new MoveInfo

                temp.add(move); //add to the list

                while (resultSet.next()) {
                    //get the move coordinates and create a new MoveInfo
                    move = MoveInfo.fromString(resultSet.getString("FROM_POSITION"), resultSet.getString("TO_POSITION"));

                    //should never occur
                    if(move == null) {
                        return null;
                    }

                    //its a move that is a part of the same turn because the newRow and newCol equal to the currentRow and currentCol
                    if (temp.getLast().newRow() == move.currentRow() && temp.getLast().newCol() == move.currentCol()) {
                        temp.add(move);
                    } else {
                        moves.add(temp); //add the old move to the return list
                        temp = new LinkedList<>(); //create a new list
                        temp.add(move); //add move to the new list
                    }
                }

                moves.add(temp); //add the final move that was made since loop exited
            } else {
                return null; //if resultSet is empty return null because no moves occurred in the game
            }
            return moves;
        }
    }

    /**
     * Gets the player's username according to their UUID
     * @param connection an established connection
     * @param uuid the user's UUID in string form
     * @return the player's username if the player exists
     */
    public String getPlayerName(Connection connection, String uuid){
        String getPlayer = "SELECT USERNAME FROM PLAYERS WHERE UUID = ?";

        try(PreparedStatement statement = connection.prepareStatement(getPlayer)){
            statement.setString(1, uuid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()){
                return resultSet.getString("USERNAME");
            }
            return "Unknown Player";
        } catch (SQLException e) {
            return "Unknown Player";
        }
    }

    /**
     * Gets the list of finished games from the GAMES table with their respective players(player1,player2,winner) username
     * @param connection an established connection
     * @return a list of FinishedGames
     */
    public ArrayList<FinishedGame> getFinishedGameIDs(Connection connection)throws SQLException{
        String selectMoves = "SELECT ID, PLAYER1_UUID, PLAYER2_UUID, WINNER_UUID FROM GAMES WHERE STATUS = 1";

        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectMoves);
            ArrayList<FinishedGame> idList = new ArrayList<>();

            while(resultSet.next()){
                //gets player usernames from the PLAYERS table, if the username does not exist will return "Unknown Player"
                idList.add(new FinishedGame(UUID.fromString(resultSet.getString("ID")),
                        getPlayerName(connection, resultSet.getString("PLAYER1_UUID")),
                        getPlayerName(connection, resultSet.getString("PLAYER2_UUID")),
                        getPlayerName(connection, resultSet.getString("WINNER_UUID"))
                ));
            }
            if (!idList.isEmpty())
                return idList;
        }
        throw new SQLException();
    }
}