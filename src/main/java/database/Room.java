package database;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Room {
    private int roomID;
    private String name;

    public Room() {}

    public int getRoomID() {
        return roomID;
    }

    public void setRoomID(int roomID) {
        this.roomID = roomID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
