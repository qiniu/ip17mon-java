package qiniu.ip17mon;

/**
 * Created by long on 2017/1/16.
 */
public final class LocationInfo {
    public final String country;
    public final String state;
    public final String city;
    public final String isp;


    public LocationInfo(String country, String state, String city, String isp) {
        this.country = country;
        this.state = state;
        this.isp = isp;
        this.city = city;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof LocationInfo)) {
            return false;
        }
        LocationInfo l = (LocationInfo) o;
        return country.equals(l.country) && state.equals(l.state) && city.equals(l.city) && isp.equals(l.isp);
    }

    public int hashCode() {
        return country.hashCode() * 31 * 31 * 31 + state.hashCode() * 31 * 31 + city.hashCode() * 31 + isp.hashCode();
    }

    public String toString() {
        return country + " " + state + " " + city + " " + isp;
    }
}
