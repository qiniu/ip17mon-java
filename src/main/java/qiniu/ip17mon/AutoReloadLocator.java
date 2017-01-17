package qiniu.ip17mon;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by long on 2017/1/17.
 */
public final class AutoReloadLocator implements ILocator, Observer {

    private final SimpleFileWatchService watchService;
    private final String filePath;
    private Locator locator;

    public AutoReloadLocator(String filePath, int intervalSeconds) throws IOException {
        this.filePath = filePath;
        locator = Locator.loadFromLocal(filePath);
        watchService = new SimpleFileWatchService(filePath, intervalSeconds);
        watchService.addObserver(this);
        watchService.excute();
    }

    // only for test
    public static void main(String[] args) {
        String filePath = "17monipdb.dat";
        if (args != null && args.length > 0) {
            filePath = args[0];
        }
        AutoReloadLocator l;
        try {
            l = new AutoReloadLocator(filePath, 10);
            System.out.println(l.find("8.8.8.8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(60 * 60 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public LocationInfo find(String ip) {
        return locator.find(ip);
    }

    @Override
    public LocationInfo find(byte[] ipBin) {
        return locator.find(ipBin);
    }

    @Override
    public LocationInfo find(int address) {
        return locator.find(address);
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            locator = Locator.loadFromLocal(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        watchService.deleteObserver(this);
        watchService.shutdown();
    }
}
