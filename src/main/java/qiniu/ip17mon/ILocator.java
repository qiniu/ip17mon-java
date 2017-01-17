package qiniu.ip17mon;

/**
 * Created by long on 2017/1/17.
 */
public interface ILocator {
    LocationInfo find(String ip);

    LocationInfo find(byte[] ipBin);

    LocationInfo find(int address);
}
