package schedule;

import database.DefaultRoutine;
import mqtt.MQTT;
import org.eclipse.paho.client.mqttv3.MqttException;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
public class ScheduledRoutine extends DefaultRoutine {
    private int hubId;
    private ArrayList<ScheduledAction> actions;

    public int getHubId() {
        return hubId;
    }

    public void setHubId(int hubId) {
        this.hubId = hubId;
    }

    public ArrayList<ScheduledAction> getActions() {
        return actions;
    }

    public void setActions(ArrayList<ScheduledAction> actions) {
        this.actions = actions;
    }

    public void run() throws MqttException {
        Async async = new Async();

        for (ScheduledAction action : actions) {
            async.run(() -> {
                String command = action.isValue() ? "{\"state\": \"ON\"}" : "{\"state\": \"OFF\"}";
                try { MQTT.setStatus(hubId, action.getIeeeAddress(), command); }
                catch (MqttException ignored) {}
            });
        }
        async.await();
    }
}
