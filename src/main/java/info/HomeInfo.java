package info;

import database.HomeOwner;
import database.Routine;
import database.UserOwner;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
@XmlRootElement
public class HomeInfo extends HomeOwner {
    private boolean online;
    private ArrayList<UserOwner> familyMembers;
    private ArrayList<Routine> routines;
    private ArrayList<RoomInfo> rooms;

    public HomeInfo() {
        super();
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public ArrayList<UserOwner> getFamilyMembers() {
        return familyMembers;
    }

    public void setFamilyMembers(ArrayList<UserOwner> familyMembers) {
        this.familyMembers = familyMembers;
    }

    public ArrayList<Routine> getRoutines() {
        return routines;
    }

    public void setRoutines(ArrayList<Routine> routines) {
        this.routines = routines;
    }

    public ArrayList<RoomInfo> getRooms() {
        return rooms;
    }

    public void setRooms(ArrayList<RoomInfo> rooms) {
        this.rooms = rooms;
    }
}
