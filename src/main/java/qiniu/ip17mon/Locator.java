package qiniu.ip17mon;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.InputMismatchException;

public final class Locator implements ILocator {
    public static final String VERSION = "0.2.2";
    private static final Charset Utf8 = Charset.forName("UTF-8");
    private final byte[] ipData;
    private final int textOffset;
    private final int[] index;
    private final int[] indexData;
    private final int[] textStartIndex;
    private final short[] textLengthIndex;

    private Locator(byte[] data) {
        this(data, (data[4] == 0 && data[5] == 0 && data[6] == 0 && data[7] == 0) && data[8] == 0 && data[9] == 0);
    }

    private Locator(byte[] data, boolean datx) {
        this.ipData = data;
        int offset = bigEndian(data, 0);
        this.index = new int[256];
        if (datx) {

            textOffset = offset - 256 * 256 * 4;

            for (int i = 0; i < 256; i++) {
                index[i] = littleEndian(data, 4 + i * 4 * 256);
            }

            int nidx = (textOffset - 4 - 256 * 256 * 4) / 9;
            indexData = new int[nidx];
            textStartIndex = new int[nidx];
            textLengthIndex = new short[nidx];

            for (int i = 0, off = 0; i < nidx; i++) {
                off = 4 + 256 * 256 * 4 + i * 9;
                indexData[i] = bigEndian(ipData, off);
                textStartIndex[i] = ((int) ipData[off + 6] & 0xff) << 16 | ((int) ipData[off + 5] & 0xff) << 8
                        | ((int) ipData[off + 4] & 0xff);
                textLengthIndex[i] = (short) (((int) ipData[off + 7] & 0xff) << 8 | ((int) ipData[off + 8] & 0xff));
            }
        } else {

            for (int i = 0; i < 256; i++) {
                index[i] = littleEndian(data, 4 + i * 4);
            }
            textOffset = offset - 1024;

            int nIdx = (textOffset - 4 - 1024) / 8;
            indexData = new int[nIdx];
            textStartIndex = new int[nIdx];
            textLengthIndex = new short[nIdx];

            for (int i = 0, off = 0; i < nIdx; i++) {
                off = 4 + 1024 + i * 8;
                indexData[i] = bigEndian(ipData, off);
                textStartIndex[i] = ((int) ipData[off + 6] & 0xff) << 16 | ((int) ipData[off + 5] & 0xff) << 8
                        | ((int) ipData[off + 4] & 0xff);
                textLengthIndex[i] = ipData[off + 7];
            }
        }
    }

    static int bigEndian(byte[] data, int offset) {
        int a = (((int) data[offset]) & 0xff);
        int b = (((int) data[offset + 1]) & 0xff);
        int c = (((int) data[offset + 2]) & 0xff);
        int d = (((int) data[offset + 3]) & 0xff);
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    static int littleEndian(byte[] data, int offset) {
        int a = (((int) data[offset]) & 0xff);
        int b = (((int) data[offset + 1]) & 0xff);
        int c = (((int) data[offset + 2]) & 0xff);
        int d = (((int) data[offset + 3]) & 0xff);
        return (d << 24) | (c << 16) | (b << 8) | a;
    }

    static byte parseOctet(String ipPart) {
        // Note: we already verified that this string contains only hex digits.
        int octet = Integer.parseInt(ipPart);
        // Disallow leading zeroes, because no clear standard exists on
        // whether these should be interpreted as decimal or octal.
        if (octet < 0 || octet > 255 || (ipPart.startsWith("0") && ipPart.length() > 1)) {
            throw new NumberFormatException("invalid ip part");
        }
        return (byte) octet;
    }

    static byte[] textToNumericFormatV4(String str) {
        String[] s = str.split("\\.");
        if (s.length != 4) {
            throw new NumberFormatException("the ip is not v4");
        }
        byte[] b = new byte[4];
        b[0] = parseOctet(s[0]);
        b[1] = parseOctet(s[1]);
        b[2] = parseOctet(s[2]);
        b[3] = parseOctet(s[3]);
        return b;
    }

    static LocationInfo buildInfo(byte[] bytes, int offset, int len) {
        String str = new String(bytes, offset, len, Utf8);
        String[] ss = str.split("\t", -1);
        if (ss.length == 4) {
            return new LocationInfo(ss[0], ss[1], ss[2], "");
        } else if (ss.length >= 5) {
            return new LocationInfo(ss[0], ss[1], ss[2], ss[4]);
        } else if (ss.length == 3) {
            return new LocationInfo(ss[0], ss[1], ss[2], "");
        } else if (ss.length == 2) {
            return new LocationInfo(ss[0], ss[1], "", "");
        }
        return null;
    }

    public static Locator loadFromNet(String netPath) throws IOException {
        URL url = new URL(netPath);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setConnectTimeout(3000);
        httpConn.setReadTimeout(30 * 1000);
        int responseCode = httpConn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return null;
        }

        int length = httpConn.getContentLength();
        if (length <= 0 || length > 64 * 1024 * 1024) {
            throw new InputMismatchException("invalid ip data");
        }
        InputStream is = httpConn.getInputStream();
        byte[] data = new byte[length];
        int downloaded = 0;
        int read = 0;
        while (downloaded < length) {
            try {
                read = is.read(data, downloaded, length - downloaded);
            } catch (IOException e) {
                is.close();
                throw new IOException("read error");
            }
            if (read < 0) {
                is.close();
                throw new IOException("read error");
            }
            downloaded += read;
        }

        is.close();

        String path = url.getPath();
        if (path.toLowerCase().endsWith("datx")) {
            return loadBinary(data, true);
        } else if (path.toLowerCase().endsWith("dat")) {
            return loadBinary(data, false);
        } else {
            return loadBinaryUnkown(data);
        }
    }

    public static Locator loadFromLocal(String filePath) throws IOException {
        File f = new File(filePath);
        FileInputStream fi = new FileInputStream(f);
        byte[] b = new byte[(int) f.length()];
        try {
            fi.read(b);
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

        if (filePath.toLowerCase().endsWith("datx")) {
            return loadBinary(b, true);
        } else if (filePath.toLowerCase().endsWith("dat")) {
            return loadBinary(b, false);
        } else {
            return loadBinaryUnkown(b);
        }
    }

    public static Locator loadFromStream(InputStream in, boolean x) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int n;

        while ((n = in.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, n);
        }

        return loadBinary(byteArrayOutputStream.toByteArray(), x);
    }

    public static Locator loadFromStreamUnkown(InputStream in) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int n;

        while ((n = in.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, n);
        }

        return loadBinaryUnkown(byteArrayOutputStream.toByteArray());
    }

    public static Locator loadFromStreamOld(InputStream in) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int n;

        while ((n = in.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, n);
        }

        return loadBinaryOld(byteArrayOutputStream.toByteArray());
    }

    public static Locator loadBinary(byte[] ipdb, boolean x) {
        return new Locator(ipdb, x);
    }

    public static Locator loadBinaryUnkown(byte[] ipdb) {
        return new Locator(ipdb);
    }

    public static Locator loadBinaryOld(byte[] ipdb) {
        return loadBinary(ipdb, false);
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            System.out.println("locator ipfile ip");
            return;
        }
        try {
            Locator l = loadFromLocal(args[0]);
            System.out.println(l.find(args[1]));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int findIndexOffset(long ip, int start, int end) {
        int mid = 0;
        while (start < end) {
            mid = (start + end) / 2;
            long l = 0xffffffffL & ((long) indexData[mid]);
            if (ip > l) {
                start = mid + 1;
            } else {
                end = mid;
            }
        }
        long l = ((long) indexData[end]) & 0xffffffffL;
        if (l >= ip) {
            return end;
        }
        return start;
    }

    public LocationInfo find(String ip) {
        byte[] b;
        try {
            b = textToNumericFormatV4(ip);
        } catch (Exception e) {
            return null;
        }
        return find(b);
    }

    public LocationInfo find(byte[] ipBin) {
        int end = indexData.length - 1;
        int a = 0xff & ((int) ipBin[0]);
        if (a != 0xff) {
            end = index[a + 1];
        }
        long ip = (long) bigEndian(ipBin, 0) & 0xffffffffL;
        int idx = findIndexOffset(ip, index[a], end);
        int off = textStartIndex[idx];
        return buildInfo(ipData, textOffset + off, 0xffff & (int) textLengthIndex[idx]);
    }

    public LocationInfo find(int address) {
        byte[] addr = new byte[4];

        addr[0] = (byte) ((address >> 24) & 0xff);
        addr[1] = (byte) ((address >> 16) & 0xff);
        addr[2] = (byte) ((address >> 8) & 0xff);
        addr[3] = (byte) (address & 0xff);

        return find(addr);
    }

    public void checkDb() throws IOException {
        byte[] addr = new byte[4];
        try {
            for (long x = 0; x < 0xffffffffL; x++) {
                addr[0] = (byte) ((x >> 24) & 0xff);
                addr[1] = (byte) ((x >> 16) & 0xff);
                addr[2] = (byte) ((x >> 8) & 0xff);
                addr[3] = (byte) (x & 0xff);
                find(addr);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}
