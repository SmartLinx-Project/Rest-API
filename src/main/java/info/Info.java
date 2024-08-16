package info;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
@XmlRootElement
public class Info {
    private ArrayList<HomeInfo> homes;
    private ArrayList<Integer> favourites;

    public Info() {
    }

    public ArrayList<HomeInfo> getHomes() {
        return homes;
    }

    public void setHomes(ArrayList<HomeInfo> homes) {
        this.homes = homes;
    }

    public ArrayList<Integer> getFavourites() {
        return favourites;
    }

    public void setFavourites(ArrayList<Integer> favourites) {
        this.favourites = favourites;
    }
}
