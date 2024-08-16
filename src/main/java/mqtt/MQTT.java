package mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;
import ssl.SslUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class MQTT {
    static final String MQTT_BROKER = "....";
    static final int MQTT_PORT = 1883;
    static final String keystorePath = "resources/mqtt_keystore.jks";
    static final String keystorePassword = "....";

    static final int ONLINE_TIMEOUT = 5; //seconds
    static final int JOIN_TIMEOUT = 180; //seconds
    static final int STATUS_TIMEOUT = 15; //seconds
    static final int LEAVE_TIMEOUT = 15; //seconds



    /**
     * Connect a new local mqtt client using ssl certificates.
     *
     * @return a new istance of a mqtt client
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     */
    public static MqttClient connect() throws MqttException {
        String clientId = MqttClient.generateClientId();
        MqttClient client = new MqttClient("tcp://" + MQTT_BROKER + ":" + MQTT_PORT, clientId, null);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        client.connect(connOpts);
        return client;
    }

    /**
     * Disconnect the mqtt client
     *
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     */
    public static void disconnect(MqttClient mqttClient) throws MqttException {
        mqttClient.disconnect();
    }

    /**
     * Checks if a hub is online.
     * After the timeout the hub is considered offline.
     *
     * @param hubID hubID to connect
     *
     * @return true if hub is online, false otherwise
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     */
    public static boolean isHubOnline(int hubID) throws MqttException {
        if(hubID <= 0) //controlla se l'hub esiste
            return false;

        MqttClient mqttClient = connect();

        AtomicBoolean isOnline = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        mqttClient.subscribe(Topic.BRIDGE_STATE.toString(hubID), (topic, message) -> {
            JSONObject messageContent = new JSONObject(new String((message.getPayload())));

            if(messageContent.optString("state").equals("online"))
                isOnline.set(true);

            latch.countDown();
        });

        try { latch.await(ONLINE_TIMEOUT, TimeUnit.SECONDS); }
        catch (InterruptedException e) { e.printStackTrace(); }

        mqttClient.unsubscribe(Topic.BRIDGE_STATE.toString(hubID));
        disconnect(mqttClient);

        return isOnline.get();
    }

    /**
     * Checks if a device is online.
     * After the timeout the device is considered offline.
     *
     * @param hubID hubID to connect
     * @param IEEEAddress IEEE address of the device to connect
     *
     * @return true if device is online, false otherwise
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     */
    public static boolean isDeviceOnline(int hubID, String IEEEAddress) throws MqttException {
        MqttClient mqttClient = connect();

        AtomicBoolean isOnline = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        mqttClient.subscribe(Topic.DEVICE_AVAILABILITY.toString(hubID, IEEEAddress), (topic, message) -> {
            JSONObject messageContent = new JSONObject(new String((message.getPayload())));

            if(messageContent.optString("state").equals("online"))
                isOnline.set(true);

            latch.countDown();
        });

        try { latch.await(ONLINE_TIMEOUT, TimeUnit.SECONDS); }
        catch (InterruptedException e) { e.printStackTrace(); }

        mqttClient.unsubscribe(Topic.DEVICE_AVAILABILITY.toString(hubID, IEEEAddress));
        disconnect(mqttClient);

        return isOnline.get();
    }

    /**
     * Publish a message on the permit_join topic to start or stop the join.
     *
     * @param hubID hubID to connect
     * @param flag true to start join, false to stop join
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     */
    public static void permitJoin(int hubID, boolean flag) throws MqttException {
        MqttClient mqttClient = connect();

        JSONObject messageContent = new JSONObject();
        messageContent.put("value", flag);
        messageContent.put("time", JOIN_TIMEOUT);

        MqttMessage message = new MqttMessage(messageContent.toString().getBytes());
        message.setQos(0); // Imposta la qualità del servizio del messaggio (0: "at most once", 1: "at least once", 2: "exactly once")

        mqttClient.publish(Topic.PERMIT_JOIN.toString(hubID), message);
        disconnect(mqttClient);
    }

    /**
     * Waits until the timeout for a new device joining the hub.
     *
     * @param hubID hubID to connect
     * @return json in raw string format containing the IEEE address, the type, and the model of the new devide joined. Returns null for no device found
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     */
    private static String waitForDeviceJoin(int hubID) throws MqttException {
        MqttClient mqttClient = connect();

        CountDownLatch latch = new CountDownLatch(2); //1 countdown per il joined e 1 per le info sul device
        JSONObject content = new JSONObject();

        mqttClient.subscribe(Topic.BRIDGE_EVENT.toString(hubID), (topic, message) -> {
            JSONObject jsonObject = new JSONObject(new String(message.getPayload()));

            if (jsonObject.optString("type").equals("device_joined")) {
                latch.countDown();
            }
            else if(jsonObject.optJSONObject("data").has("definition")){ //controlla se il messaggio contiene le info sul device
                content.put("ieeeAddress", jsonObject.optJSONObject("data").optString("ieee_address"));
                content.put("model", jsonObject.optJSONObject("data").optJSONObject("definition").optString("model"));

                JSONArray exposesArray = jsonObject.optJSONObject("data").optJSONObject("definition").optJSONArray("exposes");
                for (int i = 0; i < exposesArray.length(); i++) {
                    JSONObject exposeObject = exposesArray.getJSONObject(i);
                    if (exposeObject != null && exposeObject.optString("property").equals("temperature")) {
                        content.put("type", "thermometer");

                        String command = "{\"id\": \"" + content.optString("ieeeAddress") + "\", \"options\":{\"retain\":true}}";
                        MqttMessage requestMessage = new MqttMessage(command.getBytes());
                        requestMessage.setQos(0);
                        connect().publish(Topic.DEVICE_OPTION.toString(hubID), requestMessage);
                        break;
                    }
                }

                if(content.optString("type").isEmpty()) {
                    content.put("type", jsonObject.optJSONObject("data").optJSONObject("definition").optJSONArray("exposes").getJSONObject(0).optString("type"));
                }

                mqttClient.unsubscribe(topic);
                latch.countDown();
            }
        });

        permitJoin(hubID, true);

        try { latch.await(JOIN_TIMEOUT, TimeUnit.SECONDS); }
        catch (InterruptedException e) { e.printStackTrace(); }

        permitJoin(hubID, false);

        disconnect(mqttClient);

        if(content.isEmpty())
            return null;
        else
            return content.toString();
    }

    /**
     * Add a device to the hub
     *
     * @param hubID hubID to connect
     * @return json in raw string format containing the IEEE address, the type, and the model of the new devide joined. Returns null for no device found
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     * @throws IllegalStateException if hub is unreachable
     */
    public static String addDevice(int hubID) throws MqttException {
        if (!isHubOnline(hubID)) {
            throw new IllegalStateException("L'hub Zigbee non è online.");
        }

        return waitForDeviceJoin(hubID);
    }

    /**
     * Set the status of the device.
     *
     * @param hubID hubID to connect
     * @param IEEEAddress IEEE address of the device to connect
     * @param command new status of the device
     * @return true if the status has been set, false if the device is unreachable or the timeot expires
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     * @throws IllegalStateException if device is unreachable
     */
    public static boolean setStatus(int hubID, String IEEEAddress, String command) throws MqttException {
        MqttClient mqttClient = connect();

        if (!isHubOnline(hubID) || !isDeviceOnline(hubID, IEEEAddress)) {
            disconnect(mqttClient);
            throw new IllegalStateException("Impossibile raggiungere il dispositivo.");
        }


        CountDownLatch latch = new CountDownLatch(1);
        mqttClient.subscribe(Topic.DEVICE.toString(hubID, IEEEAddress), (topic, messageGet) -> {
            messageGet.getPayload();

            mqttClient.unsubscribe(topic);
            latch.countDown();
        });

        //imposta lo stato
        MqttMessage message = new MqttMessage(command.getBytes());
        message.setQos(0);
        mqttClient.publish(Topic.DEVICE_SET.toString(hubID, IEEEAddress), message);

        //controlla se lo stato è stato impostato
        boolean executed = false;
        try { executed = latch.await(STATUS_TIMEOUT, TimeUnit.SECONDS); }
        catch (InterruptedException e) { e.printStackTrace(); }

        disconnect(mqttClient);

        return executed;
    }

    /**
     * Get the current status of the device.
     *
     * @param hubID hubID to connect
     * @param IEEEAddress IEEE address of the device to connect
     *
     * @return json in raw string format containing the status. Returns null if the timeout expires
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     * @throws IllegalStateException if device is unreachable
     */
    public static String getStatus(int hubID, String IEEEAddress, String type) throws MqttException {
        MqttClient mqttClient = connect();

        final String command = "{\"state\": \"\"}";
        MqttMessage requestMessage;

        AtomicReference<String> status = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        switch (type) {
            case "light":
                if (!isHubOnline(hubID) || !isDeviceOnline(hubID, IEEEAddress)) {
                    disconnect(mqttClient);
                    throw new IllegalStateException("Impossibile raggiungere il dispositivo.");
                }
                //ottine lo stato
                mqttClient.subscribe(Topic.DEVICE.toString(hubID, IEEEAddress), (topic, statusMessage) -> {
                    JSONObject jsonObject = new JSONObject(new String(statusMessage.getPayload()));
                    jsonObject.remove("update");
                    status.set(jsonObject.toString());

                    latch.countDown();
                });
                //manda richiesta get
                requestMessage = new MqttMessage(command.getBytes());
                requestMessage.setQos(0);
                mqttClient.publish(Topic.DEVICE_GET.toString(hubID, IEEEAddress), requestMessage);
            break;
            case "switch":
                if (!isHubOnline(hubID) || !isDeviceOnline(hubID, IEEEAddress)) {
                    disconnect(mqttClient);
                    throw new IllegalStateException("Impossibile raggiungere il dispositivo.");
                }
                //ottine lo stato
                mqttClient.subscribe(Topic.DEVICE.toString(hubID, IEEEAddress), (topic, message) -> {
                    JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                    jsonObject.remove("child_lock");
                    jsonObject.remove("power_outage_memory");
                    jsonObject.remove("indicator_mode");
                    status.set(jsonObject.toString());

                    latch.countDown();
                });
                //manda richiesta get
                requestMessage = new MqttMessage(command.getBytes());
                requestMessage.setQos(0);
                mqttClient.publish(Topic.DEVICE_GET.toString(hubID, IEEEAddress), requestMessage);
            break;
            case "thermometer":
                if (!isHubOnline(hubID)) {
                    disconnect(mqttClient);
                    throw new IllegalStateException("Impossibile raggiungere il dispositivo.");
                }
                //ottine lo stato
                mqttClient.subscribe(Topic.DEVICE.toString(hubID, IEEEAddress), (topic, message) -> {
                    JSONObject jsonObject = new JSONObject(new String(message.getPayload()));
                    status.set(jsonObject.toString());

                    latch.countDown();
                });
            break;
        }


        try { latch.await(STATUS_TIMEOUT, TimeUnit.SECONDS); }
        catch (InterruptedException e) { e.printStackTrace(); }

        mqttClient.unsubscribe(Topic.DEVICE.toString(hubID, IEEEAddress));
        disconnect(mqttClient);

        return status.get();
    }

    /**
     * Leave a device from the network
     *
     * @param hubID hubID to connect
     * @param IEEEAddress IEEE address of the device to leave
     * @throws MqttException mqtt exception thrown because of failed mqtt request
     * @throws IllegalStateException if device is unreachable
     */
    public static void leaveDevice(int hubID, String IEEEAddress) throws MqttException {
        MqttClient mqttClient = connect();

        final String command = "{\"id\": \"" + IEEEAddress + "\"}";
        final String commandForced = "{\"id\": \"" + IEEEAddress + "\",\"force\":true}";

        if (!isHubOnline(hubID)) {
            disconnect(mqttClient);
            throw new IllegalStateException("Impossibile raggiungere il dispositivo.");
        }

        //manda richiesta leave
        MqttMessage message = new MqttMessage(command.getBytes());
        message.setQos(0);
        mqttClient.publish(Topic.DEVICE_REMOVE.toString(hubID), message);

        //manda richiesta leave forzata
        message = new MqttMessage(commandForced.getBytes());
        message.setQos(0);
        mqttClient.publish(Topic.DEVICE_REMOVE.toString(hubID), message);

        disconnect(mqttClient);
    }
}
