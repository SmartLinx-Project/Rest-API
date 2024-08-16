package rest;

import database.*;
import firebase.Firebase;
import mqtt.MQTT;
import org.eclipse.paho.client.mqttv3.MqttException;

import info.Info;
import org.json.JSONObject;
import schedule.ScheduledRoutine;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;

@Path("/smartlinx")
public class RestResources {
    private static final boolean ENABLE_LOG = true;

    private Response error(SQLException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("SQL_ERROR_CODE: " + e.getErrorCode() + "\n" + e.getMessage()).build();
    }

    private Response error(MqttException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("MQTT_ERROR_CODE: " + e.getReasonCode() + "\n" + e.getMessage()).build();
    }

    private void log(String uid) {
        if (!ENABLE_LOG)
            return;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Verifica il terzo elemento nello stack trace (indice 2), che è il chiamante diretto di log()
        if (stackTrace.length >= 3) {
            String callerMethod = stackTrace[2].getMethodName();
            System.out.println("USER: " + uid + " INVOKED: " + callerMethod);
        }
    }
    private void log(String uid, String command, String ieeeAddress) {
        if (!ENABLE_LOG)
            return;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Verifica il terzo elemento nello stack trace (indice 2), che è il chiamante diretto di log()
        if (stackTrace.length >= 3) {
            String callerMethod = stackTrace[2].getMethodName();
            System.out.println("USER: " + uid + " INVOKED: " + callerMethod + " WITH " + command + " ON DEVICE: " + ieeeAddress);
        }
    }

    @Path("/apiStatus")
    @GET
    public Response getApiStatus() {
        return Response.status(Response.Status.OK).build();
    }

    @Path("/authStatus")
    @GET
    public Response getAuthStatus() {
        if (Firebase.isAuthServiceReachable()) {
            return Response.status(Response.Status.OK).build();
        } else{
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
    }

    @Path("/appVersion")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppVersion() {
        String appVersion;

        try { appVersion = DB.getAppVersion().toString(); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).entity(appVersion).build();
    }

    @Path("/user")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(User newUser, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try { DB.addUser(newUser); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/user")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserByMail(@QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        User user;
        try { user = DB.getUserByMail(mail); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).entity(user).build();
    }

    @Path("/user")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setUser(User user, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        //imposta la mail a quella del token per evitare problemi di autorizzazione
        user.setMail(mail);

        try { DB.setUser(user); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/user")
    @DELETE
    public Response delUser(@QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try { DB.delUser(mail); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }catch(MqttException e){
            e.printStackTrace();
            return Response.status(Response.Status.REQUEST_TIMEOUT).build();
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/home")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addHome(Home newHome, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try { DB.addHome(newHome, mail); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/home")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHomesByMail(@QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        ArrayList<HomeOwner> homeList;
        try { homeList = DB.getHomes(mail); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).entity(homeList).build();
    }

    @Path("/home")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setHome(Home home, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasOwnerRightsOnHome(mail, home.getHomeID()))
                DB.setHome(home);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/home")
    @DELETE
    public Response delHome(@QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasOwnerRightsOnHome(mail, homeId))
                DB.delHome(homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        } catch(MqttException e){
            e.printStackTrace();
            return error(e);
        } catch (IllegalStateException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/home/hub")
    @PUT
    public Response setHub(@QueryParam("homeId") int homeId, @QueryParam("hubId") int hubId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        //controlla autorizzazioni
        try {
            if(!DB.hasOwnerRightsOnHome(mail, homeId))
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        //controlla se l'hub è online
        try {
            if(!MQTT.isHubOnline(hubId))
                return Response.status(Response.Status.NOT_FOUND).build();
        } catch (MqttException e) {
            e.printStackTrace();
            return error(e);
        }

        try { DB.setHub(homeId, hubId); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/home/hub")
    @POST
    public Response disableJoin(@QueryParam("hubId") int hubId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        //controlla autorizzazioni
        try {
            if(!DB.hasMemberRightsOnHome(mail, DB.getHomeIDFromHubID(hubId)))
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        try { MQTT.permitJoin(hubId, false); }
        catch (MqttException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).build();
    }

    @Path("/familyMember")
    @POST
    public Response addFamilyMember(@QueryParam("mail") String mail, @QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mailAuth = Firebase.verifyToken(token, uid);

        if(mailAuth == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasOwnerRightsOnHome(mailAuth, homeId))
                DB.addFamilyMember(mail, homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/familyMember")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFamilyMembers(@QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        ArrayList<UserOwner> userList;
        try {
            if(DB.hasMemberRightsOnHome(mail, homeId))
                userList = DB.getFamilyMembers(homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();

        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).entity(userList).build();
    }

    @Path("/familyMember")
    @DELETE
    public Response delFamilyMember(@QueryParam("mail") String mail, @QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mailAuth = Firebase.verifyToken(token, uid);
        if(mailAuth == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasOwnerRightsOnHome(mailAuth, homeId))
                DB.delFamilyMember(mail, homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();

        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/room")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRoom(Room newRoom, @QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnHome(mail, homeId))
                DB.addRoom(newRoom, homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/room")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRooms(@QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        ArrayList<Room> roomList;
        try {
            if (DB.hasMemberRightsOnHome(mail, homeId))
                roomList = DB.getRooms(homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).entity(roomList).build();
    }

    @Path("/room")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setRoom(Room room, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnRoom(mail, room.getRoomID()))
                DB.setRoom(room);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/room")
    @DELETE
    public Response delRoom(@QueryParam("roomId") int roomId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnRoom(mail, roomId))
                DB.delRoom(roomId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        } catch(MqttException e){
            e.printStackTrace();
            return error(e);
        } catch (IllegalStateException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/device")
    @POST
    public Response addDevice(Device newDevice, @QueryParam("roomId") int roomId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnRoom(mail, roomId))
                DB.addDevice(newDevice, roomId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/device")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices(@QueryParam("roomId") int roomId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        ArrayList<Device> deviceList;
        try {
            if(DB.hasMemberRightsOnRoom(mail, roomId))
                deviceList = DB.getDevices(roomId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).entity(deviceList).build();
    }

    @Path("/device")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setDevice(Device device, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnDevice(mail, device.getDeviceID()))
                DB.setDevice(device);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/device")
    @DELETE
    public Response delDevice(@QueryParam("deviceId") int deviceId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnDevice(mail, deviceId))
                DB.delDevice(deviceId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        } catch (MqttException e) {
            return error(e);
        } catch (IllegalStateException e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/device/mqtt")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response joinDevice(@QueryParam("hubId") int hubId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        //controlla autorizzazioni
        //se l'hubID non esiste getHomeIDFromHubID restituisce 0 e allora
        //il controllo sull'autorizzazione è sempre negativo perché non esiste nessina casa con ID=0
        try {
            if(!DB.hasMemberRightsOnHome(mail, DB.getHomeIDFromHubID(hubId)))
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        String content;
        try { content = MQTT.addDevice(hubId); }
        catch (MqttException e) {
            return error(e);
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        //controlla se il dispositivo non è stato trovato
        if(content == null)
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("DISPOSITIVO NON TROVATO").build();

        //controlla se l'ieeeAddress del nuovo dispositivo esiste già
        try {
            if(DB.ieeeAddressAlreadyInUse(new JSONObject(content).optString("ieeeAddress")))
                return Response.status(Response.Status.CONFLICT).build();
        } catch (SQLException e) {
            e.printStackTrace();
            error(e);
        }

        return Response.status(Response.Status.OK).entity(content).build();
    }

    @Path("/device/mqtt")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus(@QueryParam("hubId") int hubId, @QueryParam("ieee_address") String ieee_address, @QueryParam("type") String type,@QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        //controlla autorizzazioni
        //se l'hubID non esiste getHomeIDFromHubID restituisce 0 e allora
        //il controllo sull'autorizzazione è sempre negativo perché non esiste nessina casa con ID=0
        try {
            if(!DB.hasMemberRightsOnHome(mail, DB.getHomeIDFromHubID(hubId)))
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        String status = null;
        try {
            status = MQTT.getStatus(hubId, ieee_address, type);
        } catch (MqttException e) {
            error(e);
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        if(status == null)
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("DISPOSITIVO NON RAGGIUNGIBILE").build();
        else
            return Response.status(Response.Status.OK).entity(status).build();
    }

    @Path("/device/mqtt")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setStatus(StatusRequest statusRequest, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        //controlla autorizzazioni
        //se l'hubID non esiste getHomeIDFromHubID restituisce 0 e allora
        //il controllo sull'autorizzazione è sempre negativo perché non esiste nessina casa con ID=0
        try {
            if(!DB.hasMemberRightsOnHome(mail, DB.getHomeIDFromHubID(statusRequest.getHubId())))
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        boolean executed = false;
        try {
            executed = MQTT.setStatus(statusRequest.getHubId(), statusRequest.getIeee_address(), statusRequest.getCommand());
        } catch (MqttException e) {
            error(e);
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        log(uid, statusRequest.getCommand(), statusRequest.getIeee_address());

        if(executed)
            return Response.status(Response.Status.OK).build();
        else
            return Response.status(Response.Status.REQUEST_TIMEOUT).entity("DISPOSITIVO NON RAGGIUNGIBILE").build();

    }

    @Path("/device/mqtt")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response leaveDevice(@QueryParam("hubId") int hubId, @QueryParam("ieee_address") String ieee_address, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        //controlla autorizzazioni
        //se l'hubID non esiste getHomeIDFromHubID restituisce 0 e allora
        //il controllo sull'autorizzazione è sempre negativo perché non esiste nessina casa con ID=0
        try {
            if(!DB.hasMemberRightsOnHome(mail, DB.getHomeIDFromHubID(hubId)))
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        try {
            MQTT.leaveDevice(hubId, ieee_address);
        } catch (MqttException e) {
            error(e);
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.status(Response.Status.OK).build();
    }

    @Path("/favourite")
    @POST
    public Response addFavourite(@QueryParam("deviceId") int deviceId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnDevice(mail, deviceId))
                DB.addFavourite(mail, deviceId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/favourite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFavourites(@QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        ArrayList<Integer> deviceList;
        try { deviceList = DB.getFavourites(mail); }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).entity(deviceList).build();
    }

    @Path("/favourite")
    @DELETE
    public Response delFavourite(@QueryParam("deviceId") int deviceId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if (DB.hasMemberRightsOnDevice(mail, deviceId))
                DB.delFavourite(mail, deviceId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).build();
    }

    @Path("/routine")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRoutine(Routine newRoutine, @QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnHome(mail, homeId))
                DB.addRoutine(newRoutine, homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        log(uid);
        return Response.status(Response.Status.CREATED).build();
    }

    @Path("/routine")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoutines(@QueryParam("homeId") int homeId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        ArrayList<Routine> routineList;
        try {
            if (DB.hasMemberRightsOnHome(mail, homeId))
                routineList = DB.getRoutines(homeId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        return Response.status(Response.Status.OK).entity(routineList).build();
    }

    @Path("/routine")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setRoutine(Routine routine, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if(DB.hasMemberRightsOnRoutine(mail, routine.getRoutineID()))
                DB.setRoutine(routine);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/routine")
    @DELETE
    public Response delRoutine(@QueryParam("routineId") int routineId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            if (DB.hasMemberRightsOnRoutine(mail, routineId))
                DB.delRoutine(routineId);
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        }

        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/routine/run")
    @POST
    public Response runRoutine(@QueryParam("routineId") int routineId, @QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            ScheduledRoutine routine = DB.getScheduledRoutine(routineId);
            if(DB.hasMemberRightsOnRoutine(mail, routine.getRoutineID()))
                routine.run();
            else
                return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (SQLException e) {
            e.printStackTrace();
            return error(e);
        } catch (MqttException e) {
            e.printStackTrace();
            return error(e);
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        log(uid);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/user/info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo(@QueryParam("token") String token, @QueryParam("uid") String uid) {
        String mail = Firebase.verifyToken(token, uid);
        if(mail == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();

        Info info = DB.getInfo(mail);

        log(uid);
        return Response.status(Response.Status.OK).entity(info).build();
    }

}
