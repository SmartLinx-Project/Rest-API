package database;

import firebase.Firebase;
import info.DeviceInfo;
import info.HomeInfo;
import info.Info;
import info.RoomInfo;
import mqtt.MQTT;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;
import schedule.Async;
import schedule.ScheduledAction;
import schedule.ScheduledRoutine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DB {
    static final String ADDRESS = "....";
    static final int MYSQL_PORT = 3306;
    static final String NAME = "....";
    static final String USER = "....";
    static final String PASSWORD = "....";

    public static Connection startConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return DriverManager.getConnection("jdbc:mysql://" + ADDRESS + ":" + MYSQL_PORT + "/" + NAME, USER, PASSWORD);
    }
    private static void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }
    public static JSONObject getAppVersion() throws SQLException {
        final String query = "SELECT app_update FROM app_version LIMIT 1";

        Connection connection = startConnection();
        JSONObject json = new JSONObject();

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();


        if (resultSet.next()) {
            String rawJson = resultSet.getString("app_update");
            json = new JSONObject(rawJson);
        }

        closeConnection(connection);
        return json;
    }
    public static void addUser(User newUser) throws SQLException {
        final String query = "INSERT INTO users (firstName, lastName, mail) VALUES (?, ?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, newUser.getFirstName());
        statement.setString(2, newUser.getLastName());
        statement.setString(3, newUser.getMail());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static User getUserByMail(String mail) throws SQLException {
        final String query = "SELECT * FROM users WHERE mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);

        ResultSet resultSet = statement.executeQuery();


        resultSet.next();

        User user = new User();
        user.setMail(resultSet.getString("mail"));
        user.setFirstName(resultSet.getString("firstName"));
        user.setLastName(resultSet.getString("lastName"));
        user.setProfilePicture(Firebase.getProfilePicture(mail));

        closeConnection(connection);

        return user;
    }
    public static void setUser(User user) throws SQLException {
        final String query =    "UPDATE users " +
                                "SET firstName = ?, lastName = ? " +
                                "WHERE mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, user.getFirstName());
        statement.setString(2, user.getLastName());
        statement.setString(3, user.getMail());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delUser(String mail) throws SQLException, MqttException {
        final String delUser = "DELETE FROM users WHERE mail = ?";
        final String delHomesByMail =   "DELETE homes " +
                                        "FROM homes " +
                                        "JOIN user_home USING(homeID) " +
                                        "WHERE user_home.mail = ? AND user_home.owner = true";
        final String selectQuery =  "SELECT hubID, ieeeAddress " +
                                    "FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "JOIN user_home USING(homeID) " +
                                    "WHERE mail=? AND owner=true";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setString(1, mail);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement homesStatement = connection.prepareStatement(delHomesByMail);
        homesStatement.setString(1, mail);
        homesStatement.executeUpdate();

        PreparedStatement userStatement = connection.prepareStatement(delUser);
        userStatement.setString(1, mail);
        userStatement.executeUpdate();

        closeConnection(connection);
    }
    public static void addHome(Home newHome, String mail) throws SQLException {
        final String homeInsertQuery = "INSERT INTO homes (name, address) VALUES (?, ?)";
        final String userHomeInsertQuery = "INSERT INTO user_home (mail, homeID, owner) VALUES (?, ?, true)";

        Connection connection = startConnection();

        //aggiunge record alla tabella home
        PreparedStatement homeStatement = connection.prepareStatement(homeInsertQuery, Statement.RETURN_GENERATED_KEYS);
        homeStatement.setString(1, newHome.getName());
        homeStatement.setString(2, newHome.getAddress());
        homeStatement.executeUpdate();

        //aggiunge record alla tabella utenti_home
        ResultSet generatedKeys = homeStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
            int homeId = generatedKeys.getInt(1);
            PreparedStatement userHomeStatement = connection.prepareStatement(userHomeInsertQuery);
            userHomeStatement.setString(1, mail);
            userHomeStatement.setInt(2, homeId);
            userHomeStatement.executeUpdate();
        } else {
            throw new SQLException("Failed to insert home, no generated key obtained.");
        }

        closeConnection(connection);
    }
    public static ArrayList<HomeOwner> getHomes(String mail) throws SQLException {
        final String query =    "SELECT homes.homeID, homes.hubID, homes.name, homes.address, user_home.owner " +
                                "FROM homes " +
                                "INNER JOIN user_home USING(homeID) " +
                                "INNER JOIN users USING(mail) " +
                                "WHERE users.mail = ?";
        ArrayList<HomeOwner> homeList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            HomeOwner home = new HomeOwner();
            home.setHomeID(resultSet.getInt("homeID"));
            home.setHubID(resultSet.getInt("hubID"));
            home.setName(resultSet.getString("name"));
            home.setAddress(resultSet.getString("address"));
            home.setOwner(resultSet.getBoolean("owner"));

            homeList.add(home);
        }

        closeConnection(connection);

        return homeList;
    }
    public static ArrayList<HomeInfo> getHomesInfo(String mail) throws SQLException {
        final String query =    "SELECT homes.homeID, homes.hubID, homes.name, homes.address, user_home.owner " +
                                "FROM homes " +
                                "INNER JOIN user_home USING(homeID) " +
                                "INNER JOIN users USING(mail) " +
                                "WHERE users.mail = ?";
        ArrayList<HomeInfo> homeList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            HomeInfo home = new HomeInfo();
            home.setHomeID(resultSet.getInt("homeID"));
            home.setHubID(resultSet.getInt("hubID"));
            home.setName(resultSet.getString("name"));
            home.setAddress(resultSet.getString("address"));
            home.setOwner(resultSet.getBoolean("owner"));

            homeList.add(home);
        }

        closeConnection(connection);

        return homeList;
    }
    public static void setHome(Home home) throws SQLException {
        final String query =    "UPDATE homes " +
                                "SET name = ?, address = ? " +
                                "WHERE homeID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, home.getName());
        statement.setString(2, home.getAddress());
        statement.setInt(3, home.getHomeID());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delHome(int homeId) throws SQLException, MqttException {
        final String query = "DELETE FROM homes WHERE homeID = ?";
        final String selectQuery =  "SELECT hubID, ieeeAddress " +
                                    "FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "WHERE homes.homeID = ?";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, homeId);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void setHub(int homeId, int hubId) throws SQLException {
        final String query =    "UPDATE homes " +
                                "SET hubID = ? " +
                                "WHERE homeID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, hubId);
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addFamilyMember(String mail, int homeId) throws SQLException {
        final String query = "INSERT INTO user_home(mail, homeID) VALUES (?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<UserOwner> getFamilyMembers(int homeId) throws SQLException {
        final String query =    "SELECT users.mail, users.firstName, users.lastName, user_home.owner " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "WHERE homes.homeID = ?";
        ArrayList<UserOwner> userList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            UserOwner user = new UserOwner();
            user.setMail(resultSet.getString("mail"));
            user.setFirstName(resultSet.getString("firstName"));
            user.setLastName(resultSet.getString("lastName"));
            user.setOwner(resultSet.getBoolean("owner"));
            user.setProfilePicture(Firebase.getProfilePicture(user.getMail()));

            userList.add(user);
        }

        closeConnection(connection);

        return userList;
    }
    public static void delFamilyMember(String mail, int homeId) throws SQLException {
        final String query = "DELETE FROM user_home WHERE mail = ? AND homeID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addRoom(Room newRoom, int homeId) throws SQLException {
        final String query = "INSERT INTO rooms(name, homeID) VALUES (?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, newRoom.getName());
        statement.setInt(2, homeId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<Room> getRooms(int homeId) throws SQLException {
        final String query =    "SELECT rooms.roomID, rooms.name " +
                                "FROM rooms " +
                                "WHERE rooms.homeID = ?";
        ArrayList<Room> roomList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Room room = new Room();
            room.setRoomID(resultSet.getInt("roomID"));
            room.setName(resultSet.getString("name"));

            roomList.add(room);
        }

        closeConnection(connection);

        return roomList;
    }
    public static ArrayList<RoomInfo> getRoomsInfo(int homeId) throws SQLException {
        final String query =    "SELECT rooms.roomID, rooms.name " +
                                "FROM rooms " +
                                "WHERE rooms.homeID = ?";
        ArrayList<RoomInfo> roomList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            RoomInfo room = new RoomInfo();
            room.setRoomID(resultSet.getInt("roomID"));
            room.setName(resultSet.getString("name"));

            roomList.add(room);
        }

        closeConnection(connection);

        return roomList;
    }
    public static void setRoom(Room room) throws SQLException {
        final String query =    "UPDATE rooms " +
                                "SET name = ? " +
                                "WHERE roomID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, room.getName());
        statement.setInt(2, room.getRoomID());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delRoom(int roomId) throws SQLException, MqttException {
        final String query = "DELETE FROM rooms WHERE roomID = ?";
        final String selectQuery =  "SELECT hubID, ieeeAddress " +
                                    "FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "WHERE rooms.roomID = ?";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, roomId);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addDevice(Device newDevice, int roomId) throws SQLException {
        final String query =    "INSERT INTO devices(name, ieeeAddress, type, model, roomID) " +
                                "VALUES (?, ?, ?, ?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, newDevice.getName());
        statement.setString(2, newDevice.getIeeeAddress());
        statement.setString(3, newDevice.getType());
        statement.setString(4, newDevice.getModel());
        statement.setInt(5, roomId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<Device> getDevices(int roomId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM devices " +
                                "WHERE devices.roomID = ?";
        ArrayList<Device> deviceList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Device device = new Device();

            device.setDeviceID(resultSet.getInt("deviceID"));
            device.setName(resultSet.getString("name"));
            device.setIeeeAddress(resultSet.getString("ieeeAddress"));
            device.setType(resultSet.getString("type"));
            device.setModel(resultSet.getString("model"));

            deviceList.add(device);
        }

        closeConnection(connection);

        return deviceList;
    }
    public static ArrayList<DeviceInfo> getDevicesInfo(int roomId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM devices " +
                                "WHERE devices.roomID = ?";
        ArrayList<DeviceInfo> deviceList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            DeviceInfo device = new DeviceInfo();

            device.setDeviceID(resultSet.getInt("deviceID"));
            device.setName(resultSet.getString("name"));
            device.setIeeeAddress(resultSet.getString("ieeeAddress"));
            device.setType(resultSet.getString("type"));
            device.setModel(resultSet.getString("model"));

            deviceList.add(device);
        }

        closeConnection(connection);

        return deviceList;
    }
    public static void setDevice (Device device) throws SQLException {
        final String query =    "UPDATE devices " +
                                "SET name = ? " +
                                "WHERE deviceID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, device.getName());
        statement.setInt(2, device.getDeviceID());
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delDevice(int deviceId) throws SQLException, MqttException {
        final String deleteQuery = "DELETE FROM devices WHERE deviceID = ?";
        final String selectQuery =  "SELECT hubID, ieeeAddress FROM devices " +
                                    "JOIN rooms USING(roomID) " +
                                    "JOIN homes USING(homeID) " +
                                    "WHERE deviceID = ?";

        Connection connection = startConnection();

        PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
        selectStatement.setInt(1, deviceId);
        ResultSet aboutDevice = selectStatement.executeQuery();

        while(aboutDevice.next()){
            MQTT.leaveDevice(aboutDevice.getInt("hubID"), aboutDevice.getString("ieeeAddress"));
        }

        PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
        deleteStatement.setInt(1, deviceId);
        deleteStatement.executeUpdate();

        closeConnection(connection);
    }
    public static void addFavourite(String mail, int deviceId) throws SQLException {
        final String query = "INSERT INTO favourites(mail, deviceID) VALUES (?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, deviceId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<Integer> getFavourites(String mail) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM devices " +
                                "INNER JOIN favourites USING(deviceID) " +
                                "WHERE mail = ?";
        ArrayList<Integer> deviceList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            deviceList.add(resultSet.getInt("deviceID"));
        }

        closeConnection(connection);

        return deviceList;
    }
    public static void delFavourite(String mail, int deviceId) throws SQLException {
        final String query = "DELETE FROM favourites WHERE mail = ? AND deviceID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, mail);
        statement.setInt(2, deviceId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addAction(Action newAction, int routineId) throws SQLException {
        final String query =    "INSERT INTO actions(value, deviceID, routineID) " +
                                "VALUES (?, ?, ?)";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setBoolean(1, newAction.isValue());
        statement.setInt(2, newAction.getDeviceID());
        statement.setInt(3, routineId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static ArrayList<Action> getActions(int routineId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM actions " +
                                "WHERE actions.routineID = ?";
        ArrayList<Action> actionList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, routineId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Action action = new Action();
            action.setValue(resultSet.getBoolean("value"));
            action.setDeviceID(resultSet.getInt("deviceID"));

            actionList.add(action);
        }

        closeConnection(connection);

        return actionList;
    }
    public static void setAction(Action action, int routineId) throws SQLException {
        final String query =    "UPDATE actions " +
                                "SET value = ? " +
                                "WHERE deviceID = ? AND routineID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setBoolean(1, action.isValue());
        statement.setInt(2, action.getDeviceID());
        statement.setInt(3, routineId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void delAction(int deviceId, int routineId) throws SQLException {
        final String query = "DELETE FROM actions WHERE deviceID = ? AND routineID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, deviceId);
        statement.setInt(2, routineId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static void addRoutine(Routine newRoutine, int homeId) throws SQLException {
        final String query =    "INSERT INTO routines(name, icon, enabled, time, periodicity, homeID) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";


        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, newRoutine.getName());
        statement.setString(2, newRoutine.getIcon());
        statement.setBoolean(3, newRoutine.isEnabled());
        statement.setString(4, newRoutine.getTime());
        statement.setString(5, String.join(",", newRoutine.getPeriodicity()));
        statement.setInt(6, homeId);

        statement.executeUpdate();

        ResultSet generatedKeys = statement.getGeneratedKeys();
        if (generatedKeys.next()) {
            int routineId = generatedKeys.getInt(1);

            for(Action action : newRoutine.getActions())
                addAction(action, routineId);
        } else {
            throw new SQLException("Failed to insert routine, no generated key obtained.");
        }

        closeConnection(connection);
    }
    public static ArrayList<Routine> getRoutines(int homeId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM routines " +
                                "WHERE routines.homeID = ?";
        ArrayList<Routine> routineList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            Routine routine = new Routine();

            routine.setRoutineID(resultSet.getInt("routineID"));
            routine.setName(resultSet.getString("name"));
            routine.setIcon(resultSet.getString("icon"));
            routine.setEnabled(resultSet.getBoolean("enabled"));
            routine.setTime(resultSet.getString("time"));
            routine.setPeriodicity(resultSet.getString("periodicity").split(","));
            routine.setActions(getActions(routine.getRoutineID()));

            routineList.add(routine);
        }

        closeConnection(connection);

        return routineList;
    }
    public static void setRoutine(Routine routine) throws SQLException {
        final String query =    "UPDATE routines " +
                                "SET name = ?, icon = ?, enabled = ?, time = ?, periodicity = ? " +
                                "WHERE routineID = ?";
        final String selectActionsQuery = "SELECT deviceID FROM actions WHERE routineID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, routine.getName());
        statement.setString(2, routine.getIcon());
        statement.setBoolean(3, routine.isEnabled());
        statement.setString(4, routine.getTime());
        statement.setString(5, String.join(",", routine.getPeriodicity()));
        statement.setInt(6, routine.getRoutineID());

        // Recuperare le azioni esistenti nel database per questa routine
        PreparedStatement selectActionsStatement = connection.prepareStatement(selectActionsQuery);
        selectActionsStatement.setInt(1, routine.getRoutineID());
        ResultSet resultSet = selectActionsStatement.executeQuery();

        Set<Integer> existingDeviceIDs = new HashSet<>();
        while (resultSet.next()) {
            existingDeviceIDs.add(resultSet.getInt("deviceID"));
        }

        // Aggiornare o creare azioni e tenere traccia di quelle presenti nella nuova routine
        Set<Integer> deviceIDsInRoutine = new HashSet<>();
        for (Action action : routine.getActions()) {
            setAction(action, routine.getRoutineID());
            deviceIDsInRoutine.add(action.getDeviceID());
        }

        // Eliminare le azioni che non sono presenti nella nuova routine
        for (Integer deviceID : existingDeviceIDs) {
            if (!deviceIDsInRoutine.contains(deviceID)) {
               delAction(deviceID, routine.getRoutineID());
            }
        }

        // Aggiungere le azioni che sono presenti nella nuova routine ma non nel database
        for (Integer deviceID : deviceIDsInRoutine) {
            if (!existingDeviceIDs.contains(deviceID)) {
                for (Action action : routine.getActions()) {
                    if (action.getDeviceID() == deviceID) {
                        addAction(action, routine.getRoutineID());
                    }
                }
            }
        }

        statement.executeUpdate();
        closeConnection(connection);
    }
    public static void delRoutine(int routineId) throws SQLException {
        final String query = "DELETE FROM routines WHERE routineID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, routineId);
        statement.executeUpdate();

        closeConnection(connection);
    }
    public static boolean hasOwnerRightsOnHome(String clientMail, int homeId) throws SQLException {
        final String query =    "SELECT users.mail, user_home.owner " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "WHERE homes.homeID = ? AND users.mail = ? AND user_home.owner = TRUE";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static boolean hasMemberRightsOnHome(String clientMail, int homeId) throws SQLException {
        final String query =    "SELECT users.mail " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "WHERE homes.homeID = ? AND users.mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, homeId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static boolean hasMemberRightsOnRoom(String clientMail, int roomId) throws SQLException {
        final String query =    "SELECT users.mail " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "INNER JOIN rooms USING(homeID) " +
                                "WHERE rooms.roomID = ? AND users.mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, roomId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static boolean hasMemberRightsOnRoutine(String clientMail, int routineId) throws SQLException {
        final String query =    "SELECT users.mail " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "INNER JOIN routines USING(homeID) " +
                                "WHERE routines.routineID = ? AND users.mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, routineId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static boolean hasMemberRightsOnDevice(String clientMail, int deviceId) throws SQLException {
        final String query =    "SELECT users.mail " +
                                "FROM users " +
                                "INNER JOIN user_home USING(mail) " +
                                "INNER JOIN homes USING(homeID) " +
                                "INNER JOIN rooms USING(homeID) " +
                                "INNER JOIN devices USING(roomID) " +
                                "WHERE devices.deviceID = ? AND users.mail = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, deviceId);
        statement.setString(2, clientMail);
        ResultSet resultSet = statement.executeQuery();

        boolean hasRights = resultSet.next();

        closeConnection(connection);
        return hasRights;
    }
    public static int getHomeIDFromHubID(int hubID) throws SQLException {
        final String query =    "SELECT homes.homeID " +
                                "FROM homes " +
                                "WHERE homes.hubID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, hubID);
        ResultSet resultSet = statement.executeQuery();

        int homeID = 0;
        if(resultSet.next())
            homeID = resultSet.getInt("homeID");

        closeConnection(connection);
        return homeID;
    }
    public static boolean ieeeAddressAlreadyInUse(String ieeeAddress) throws SQLException {
        final String query =    "SELECT COUNT(*) " +
                                "FROM devices " +
                                "WHERE ieeeAddress = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setString(1, ieeeAddress);
        ResultSet resultSet = statement.executeQuery();

        resultSet.next();
        boolean alreadyInUse = resultSet.getInt(1) > 0;

        closeConnection(connection);
        return alreadyInUse;

    }
    public static Info getInfo(String mail) {
        Info info = new Info();

        Async asyncInfo = new Async();

        asyncInfo.run(() -> {
            try { info.setFavourites(getFavourites(mail)); }
            catch (SQLException ignored) {}
        }); //favourites
        asyncInfo.run(() -> {
            try { info.setHomes(getHomesInfo(mail)); }
            catch (SQLException ignored) {}

            Async asyncHome = new Async();

            for(HomeInfo home : info.getHomes()) {

                asyncHome.run(() -> {
                    try { home.setOnline(MQTT.isHubOnline(home.getHubID())); }
                    catch (MqttException e) { home.setOnline(false); }
                }); //hubID
                asyncHome.run(() -> {
                    try { home.setFamilyMembers(getFamilyMembers(home.getHomeID())); }
                    catch (SQLException ignored) {}
                }); //familyMembers
                asyncHome.run(() -> {
                    try { home.setRoutines(getRoutines(home.getHomeID())); }
                    catch (SQLException ignored) {}
                }); //routines
                asyncHome.run(() -> {
                    try { home.setRooms(getRoomsInfo(home.getHomeID())); }
                    catch (SQLException ignored) {}

                    Async asyncRoom = new Async();

                    for(RoomInfo room : home.getRooms()) {
                        asyncRoom.run(() -> {
                            try { room.setDevices(getDevicesInfo(room.getRoomID())); }
                            catch (SQLException ignored) {}

                            Async asyncDevice = new Async();

                            for (DeviceInfo device : room.getDevices()) {
                                asyncDevice.run(() -> {
                                    try { device.setStatus(MQTT.getStatus(home.getHubID(), device.getIeeeAddress(), device.getType())); }
                                    catch (MqttException | IllegalStateException e) { device.setStatus(null); }
                                }); //status
                            }

                            asyncDevice.await();
                        }); //devices
                    }
                    asyncRoom.await();
                }); //rooms
            }
            asyncHome.await();
        }); //homes

        asyncInfo.await();
        return info;
    }
    public static ArrayList<ScheduledAction> getScheduledActions(int routineId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM actions " +
                                "JOIN devices USING(deviceID) " +
                                "WHERE actions.routineID = ? AND (type='light' OR  type='switch')";
        ArrayList<ScheduledAction> actionList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, routineId);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            ScheduledAction action = new ScheduledAction();
            action.setValue(resultSet.getBoolean("value"));
            action.setDeviceID(resultSet.getInt("deviceID"));
            action.setIeeeAddress(resultSet.getString("ieeeAddress"));

            actionList.add(action);
        }

        closeConnection(connection);

        return actionList;
    }
    public static ArrayList<ScheduledRoutine> getScheduledRoutines() throws SQLException {
        final String query =    "SELECT * " +
                                "FROM routines " +
                                "JOIN homes USING(homeID) " +
                                "WHERE time IS NOT NULL";
        ArrayList<ScheduledRoutine> routineList = new ArrayList<>();

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            ScheduledRoutine routine = new ScheduledRoutine();

            routine.setHubId(resultSet.getInt("hubID"));
            routine.setRoutineID(resultSet.getInt("routineID"));
            routine.setName(resultSet.getString("name"));
            routine.setIcon(resultSet.getString("icon"));
            routine.setEnabled(resultSet.getBoolean("enabled"));
            routine.setTime(resultSet.getString("time"));
            routine.setPeriodicity(resultSet.getString("periodicity").split(","));
            routine.setActions(getScheduledActions(routine.getRoutineID()));

            routineList.add(routine);
        }

        closeConnection(connection);

        return routineList;
    }
    public static ScheduledRoutine getScheduledRoutine(int routineId) throws SQLException {
        final String query =    "SELECT * " +
                                "FROM routines " +
                                "JOIN homes USING(homeID) " +
                                "WHERE routineID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, routineId);
        ResultSet resultSet = statement.executeQuery();

        resultSet.next();
        ScheduledRoutine routine = new ScheduledRoutine();

        routine.setHubId(resultSet.getInt("hubID"));
        routine.setRoutineID(resultSet.getInt("routineID"));
        routine.setName(resultSet.getString("name"));
        routine.setIcon(resultSet.getString("icon"));
        routine.setEnabled(resultSet.getBoolean("enabled"));
        routine.setTime(resultSet.getString("time"));
        routine.setPeriodicity(resultSet.getString("periodicity").split(","));
        routine.setActions(getScheduledActions(routine.getRoutineID()));

        return routine;
    }
    public static void disableRoutine (int routineId) throws SQLException {
        final String query =    "UPDATE routines " +
                                "SET enabled = false " +
                                "WHERE routineID = ?";

        Connection connection = startConnection();

        PreparedStatement statement = connection.prepareStatement(query);
        statement.setInt(1, routineId);
        statement.executeUpdate();

        closeConnection(connection);
    }

}
