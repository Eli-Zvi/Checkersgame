package checkersgame.server;

import checkersgame.common.FinishedGame;
import checkersgame.common.MoveInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

//TODO ADD LOCKS AND DOCUMENTATION

public class DatabaseManager {

    private static HikariDataSource ds;
    private static DatabaseManager instance;

    //username and password are not safe - only meant as an example
    private DatabaseManager() throws SQLException {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:mysql://localhost:3306/checkersgame");
        config.setUsername("root");
        config.setPassword("root");

        config.setMaximumPoolSize(20);
        config.setIdleTimeout(30000); //30 seconds

        ds = new HikariDataSource(config);

        initializeDatabase();
    }

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
            System.out.println("DB connection established successfully");
        }catch (SQLException e){
            System.out.println("connection failed to initialize");
            if (connection != null)
                connection.rollback();

            throw e;
        }
    }

    public synchronized static DatabaseManager getInstance() throws SQLException{
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

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
                "CONSTRAINT fk_player1 FOREIGN KEY (PLAYER1_UUID) REFERENCES players(UUID)," + //ensures player exists in players table
                "CONSTRAINT fk_player2 FOREIGN KEY (PLAYER2_UUID) REFERENCES players(UUID)," + //ensures player exists in players table
                "CONSTRAINT fk_winner FOREIGN KEY (WINNER_UUID) REFERENCES players(UUID)" + //ensures player exists in players table
                ")";

        String createMoveTable = "CREATE TABLE IF NOT EXISTS MOVES (" +
                "ID INT UNSIGNED AUTO_INCREMENT PRIMARY KEY," +
                "GAME_ID CHAR(36) NOT NULL," +
                "MOVE_NUMBER SMALLINT UNSIGNED NOT NULL," +
                "PLAYER_UUID CHAR(36) NOT NULL," +
                "FROM_POSITION VARCHAR(3) NOT NULL," +
                "TO_POSITION VARCHAR(3) NOT NULL," +
                "PROMOTION BINARY(1) DEFAULT FALSE," +
                "FOREIGN KEY (GAME_ID) REFERENCES GAMES(ID)," +
                "FOREIGN KEY (PLAYER_UUID) REFERENCES PLAYERS(UUID)," +
                "UNIQUE(GAME_ID, MOVE_NUMBER)" +
                ")";

        try(Statement statement = connection.createStatement()) {
            statement.addBatch(createPlayersTable);
            statement.addBatch(createGamesTable);
            statement.addBatch(createMoveTable);

            statement.executeBatch();
        }
    }

    public synchronized void addPlayer(Connection connection, UUID uuid, String username, String password) throws SQLException,
            NoSuchAlgorithmException, InvalidKeySpecException {
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
        }
    }

    public UUID verifyLogin(Connection connection, String username, String password) throws SQLException,
            NoSuchAlgorithmException, InvalidKeySpecException {

        connection.setAutoCommit(false);

        try {
            String selectPlayer = "SELECT UUID, PASSWORD, SALT, LOGGED_IN FROM PLAYERS WHERE USERNAME = ? FOR UPDATE";

            PreparedStatement statement = connection.prepareStatement(selectPlayer);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next() && !resultSet.getBoolean("LOGGED_IN")) {
                String salt = resultSet.getString("SALT");
                byte[] hashedPassword = resultSet.getBytes("PASSWORD");

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

            lockPlayers(connection, player1UUID, player2UUID);

            statement.setString(1, gameUUID.toString());
            statement.setString(2, player1UUID.toString());
            statement.setString(3, player2UUID.toString());
            statement.executeUpdate();

            winsStatement.setString(1, player1UUID.toString());
            winsStatement.executeUpdate();

            lossStatement.setString(1, player2UUID.toString());
            lossStatement.executeUpdate();

            connection.commit();
        }catch (SQLException e){
            connection.rollback();
            throw e;
        }finally {
            connection.setAutoCommit(true);

        }
    }


    public void updateGame(Connection connection, UUID gameID, UUID winner, UUID loser) throws SQLException {
        String updateGame = "UPDATE GAMES SET STATUS = TRUE, WINNER_UUID = ? WHERE ID = ?";
        String updateWins = "UPDATE PLAYERS SET STATUS = FALSE, WINS = WINS + 1 WHERE UUID= ?";
        String updateLosses = "UPDATE PLAYERS SET STATUS = FALSE, LOSSES = LOSSES + 1 WHERE UUID= ?";

        connection.setAutoCommit(false);

        try(PreparedStatement gameUpdateStatement = connection.prepareStatement(updateGame);
            PreparedStatement winsStatement = connection.prepareStatement(updateWins);
            PreparedStatement lossStatement = connection.prepareStatement(updateLosses)) {

            lockPlayers(connection, winner, loser);

            gameUpdateStatement.setString(1, winner.toString());
            gameUpdateStatement.setString(2, gameID.toString());
            gameUpdateStatement.executeUpdate();


            winsStatement.setString(1, winner.toString());
            winsStatement.executeUpdate();


            lossStatement.setString(1, loser.toString());
            lossStatement.executeUpdate();
            connection.commit();
        }catch (SQLException e){
            connection.rollback();
            throw e;
        }finally {
            connection.setAutoCommit(true);
        }
    }

    public void lockPlayers(Connection connection, UUID player1, UUID player2) throws SQLException{

        String lockPlayer = "SELECT * FROM PLAYERS WHERE UUID= ? OR UUID= ? FOR UPDATE";

        try(PreparedStatement lockStatement = connection.prepareStatement(lockPlayer)){
            lockStatement.setString(1, player1.toString());
            lockStatement.setString(2, player2.toString());
            lockStatement.executeQuery();
        }
    }

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

    public ArrayList<LinkedList<MoveInfo>> getGamesMoves(Connection connection, UUID gameID)
            throws SQLException { //make sure to check that the game is over first

        String selectMoves = "SELECT FROM_POSITION, TO_POSITION FROM MOVES WHERE GAME_ID = ?";

        try(PreparedStatement statement = connection.prepareStatement(selectMoves)) {
            statement.setString(1, gameID.toString());
            ResultSet resultSet = statement.executeQuery();
            ArrayList<LinkedList<MoveInfo>> moves = new ArrayList<>();

            if (resultSet.next()) {
                LinkedList<MoveInfo> temp = new LinkedList<>();
                MoveInfo move = MoveInfo.fromString(resultSet.getString("FROM_POSITION"),
                        resultSet.getString("TO_POSITION"));
                temp.add(move);

                while (resultSet.next()) {
                    move = MoveInfo.fromString(resultSet.getString("FROM_POSITION"), resultSet.getString("TO_POSITION"));

                    if(move == null) {
                        return null;
                    }

                    if (temp.getLast().newRow() == move.currentRow() && temp.getLast().newCol() == move.currentCol()) {
                        temp.add(move);
                    } else {
                        moves.add(temp);
                        temp = new LinkedList<>();
                        temp.add(move);
                    }
                }

                moves.add(temp);
            } else {
                return null;
            }
            return moves;
        }
    }

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

    public ArrayList<FinishedGame> getFinishedGameIDs(Connection connection)throws SQLException{
        String selectMoves = "SELECT ID, PLAYER1_UUID, PLAYER2_UUID, WINNER_UUID FROM GAMES WHERE STATUS = 1";

        try(Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(selectMoves);
            ArrayList<FinishedGame> idList = new ArrayList<>();

            while(resultSet.next()){
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