package mqtt;

public enum Topic {
    BRIDGE_STATE ("/bridge/state"),
    PERMIT_JOIN ("/bridge/request/permit_join"),
    BRIDGE_EVENT ("/bridge/event"),
    DEVICE(""),
    DEVICE_SET("/set"),
    DEVICE_GET("/get"),
    DEVICE_AVAILABILITY("/availability"),
    DEVICE_REMOVE("/bridge/request/device/remove"),
    DEVICE_OPTION("/bridge/request/device/options");

    private static final String firstLevel = "smartlinx";

    private final String expression;

    Topic(String expression) {
        this.expression = expression;
    }

    private String getExpression() {
        return expression;
    }

    public String toString(int hubID) {
        return firstLevel + "/" + hubID + getExpression();
    }

    public String toString(int hubID, String IEEAddress) {
        return firstLevel + "/" + hubID + "/" + IEEAddress + getExpression();
    }

}
