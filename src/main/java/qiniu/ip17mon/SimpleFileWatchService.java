package qiniu.ip17mon;

import java.io.File;
import java.util.Observable;

/**
 * Created by long on 2017/1/17.
 */
final class SimpleFileWatchService extends Observable implements Runnable {
    private File file;
    private Thread thread;
    private long lastModified = 0;
    private int intervalSeconds;
    private volatile boolean flag;

    SimpleFileWatchService(String filePath, int intervalSeconds) {
        this.file = new File(filePath);
        this.intervalSeconds = intervalSeconds;
        this.lastModified = file.lastModified();
        this.thread = new Thread(this);
        this.flag = true;
    }

    private void monitorFile() {
        //delay monitor
        try {
            Thread.sleep(5 * 60 * 1000);
        } catch (InterruptedException e) {
//            e.printStackTrace();
        }
        while (flag) {
            long l = file.lastModified();
            if (l != lastModified) {
                lastModified = l;
                setChanged();
                notifyObservers();
            }
            try {
                Thread.sleep(intervalSeconds * 1000);
            } catch (InterruptedException e) {
//                e.printStackTrace();
            }
        }
    }

    void excute() {
        thread.start();
    }

    @Override
    public void run() {
        monitorFile();
    }

    public void shutdown() {
        flag = false;
    }
}
