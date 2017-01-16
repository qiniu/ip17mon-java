package qiniu.ip17mon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.InputMismatchException;

/**
 * Created by long on 2017/1/16.
 */
public final class AnotherLocator {
    private int offset;
    private int[] index = new int[256];
    private ByteBuffer dataBuffer;
    private ByteBuffer indexBuffer;

    private AnotherLocator(ByteBuffer buffer) {
        dataBuffer = buffer;

        dataBuffer.position(0);
        int indexLength = dataBuffer.getInt();
        byte[] indexBytes = new byte[indexLength];
        dataBuffer.get(indexBytes, 0, indexLength - 4);
        indexBuffer = ByteBuffer.wrap(indexBytes);
        indexBuffer.order(ByteOrder.LITTLE_ENDIAN);
        offset = indexLength;

        int loop = 0;
        while (loop++ < 256) {
            index[loop - 1] = indexBuffer.getInt();
//            System.out.println(index[loop - 1]);
        }
        indexBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    private static long bytesToLong(byte a, byte b, byte c, byte d) {
        return int2long((((a & 0xff) << 24) | ((b & 0xff) << 16) | ((c & 0xff) << 8) | (d & 0xff)));
    }

    private static int str2Ip(String ip) {
        String[] ss = ip.split("\\.");
        int a, b, c, d;
        a = Integer.parseInt(ss[0]);
        b = Integer.parseInt(ss[1]);
        c = Integer.parseInt(ss[2]);
        d = Integer.parseInt(ss[3]);
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    private static long ip2long(String ip) {
        return int2long(str2Ip(ip));
    }

    private static long int2long(int i) {
        long l = i & 0x7fffffffL;
        if (i < 0) {
            l |= 0x080000000L;
        }
        return l;
    }

    public static AnotherLocator loadFromNet(String netPath) throws IOException {
        URL url = new URL(netPath);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setConnectTimeout(3000);
        httpConn.setReadTimeout(30 * 1000);
        int responseCode = httpConn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return null;
        }

        int length = httpConn.getContentLength();
        if (length <= 0 || length > 10 * 1024 * 1024) {
            throw new InputMismatchException("invalid ip data");
        }
        InputStream is = httpConn.getInputStream();
        byte[] data = new byte[length];
        int downloaded = 0;
        while (downloaded < length) {
            int read = is.read(data, downloaded, length - downloaded);
            if (read < 0) {
                is.close();
                throw new IOException("read error");
            }
            downloaded += read;
        }

        is.close();

        ByteBuffer bb = ByteBuffer.wrap(data);
        return new AnotherLocator(bb);
    }

    public static AnotherLocator loadFromLocal(String filePath) throws IOException {
        File f = new File(filePath);
        FileInputStream fi = new FileInputStream(f);
        ByteBuffer bb = ByteBuffer.allocate((int) f.length());

        try {
            fi.read(bb.array());
        } catch (IOException e) {
            e.printStackTrace();
            try {
                fi.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            throw e;
        }
        fi.close();

        return new AnotherLocator(bb);
    }

    public String[] find(String ip) {
        int ip_prefix_value = new Integer(ip.substring(0, ip.indexOf(".")));
        System.out.println("xxxx " + ip_prefix_value);
        long ip2long_value = ip2long(ip);
        int start = index[ip_prefix_value];
        int max_comp_len = offset - 1028;
        long index_offset = -1;
        int index_length = -1;
        byte b = 0;
        for (start = start * 8 + 1024; start < max_comp_len; start += 8) {
            if (int2long(indexBuffer.getInt(start)) >= ip2long_value) {
                index_offset = bytesToLong(b, indexBuffer.get(start + 6), indexBuffer.get(start + 5),
                        indexBuffer.get(start + 4));
                index_length = 0xFF & indexBuffer.get(start + 7);
                break;
            }
        }

        byte[] areaBytes;

        dataBuffer.position(offset + (int) index_offset - 1024);
        areaBytes = new byte[index_length];
        dataBuffer.get(areaBytes, 0, index_length);

        return new String(areaBytes, Charset.forName("UTF-8")).split("\t", -1);
    }
}
