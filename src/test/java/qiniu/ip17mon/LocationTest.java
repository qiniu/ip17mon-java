package qiniu.ip17mon;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by long on 2017/1/16.
 */
public class LocationTest {
    @Test
    public void testLittleEndian() {
        byte[] x = new byte[]{1, 2, 3, 4};
        Assert.assertEquals(0x04030201, Locator.littleEndian(x, 0));
        byte[] x2 = new byte[]{0, 1, 2, 3, 4};
        Assert.assertEquals(0x04030201, Locator.littleEndian(x2, 1));
    }

    @Test
    public void testBigEndian() {
        byte[] x = new byte[]{1, 2, 3, 4};
        Assert.assertEquals(0x01020304, Locator.bigEndian(x, 0));
        byte[] x2 = new byte[]{0, 1, 2, 3, 4};
        Assert.assertEquals(0x01020304, Locator.bigEndian(x2, 1));
    }

    @Test
    public void testParseOctet() {
        Assert.assertEquals(1, Locator.parseOctet("1"));
        try {
            Locator.parseOctet("-1");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NumberFormatException);
        }

        try {
            Locator.parseOctet("256");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NumberFormatException);
        }
    }

    @Test
    public void testTextToNumericFormatV4() {
        byte[] b = Locator.textToNumericFormatV4("1.2.3.4");
        Assert.assertArrayEquals(new byte[]{1, 2, 3, 4}, b);

        try {
            Locator.textToNumericFormatV4("01.2.3.4");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NumberFormatException);
        }

        try {
            Locator.textToNumericFormatV4("2.3.4");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NumberFormatException);
        }

    }

    @Test
    public void testBuildLocationInfo() {
        String s = "中国\t上海\t上海\t浦东\t电信";
        LocationInfo l = Locator.buildInfo(s.getBytes(), 0, s.getBytes().length);
        Assert.assertEquals(new LocationInfo("中国", "上海", "上海", "电信"), l);
        Assert.assertArrayEquals(new String[]{"中国", "上海", "上海", "电信"}, l.toStringArray());
    }

    private void checkFreeLocator(Locator locator) {
        LocationInfo info = locator.find("8.8.8.8");
        Assert.assertEquals(new LocationInfo("GOOGLE", "GOOGLE", "", ""), info);
        info = locator.find("183.131.7.18");
        Assert.assertEquals(new LocationInfo("中国", "浙江", "杭州", ""), info);
        info = locator.find(new byte[]{(byte) 183, (byte) 131, 7, 18});
        Assert.assertEquals(new LocationInfo("中国", "浙江", "杭州", ""), info);
        info = locator.find(Locator.bigEndian(new byte[]{(byte) 183, (byte) 131, 7, 18}, 0));
        Assert.assertEquals(new LocationInfo("中国", "浙江", "杭州", ""), info);
    }

    private void checkFullLocator(Locator locator) {
        LocationInfo info = locator.find("8.8.8.8");
        Assert.assertEquals(new LocationInfo("GOOGLE.COM", "GOOGLE.COM", "", "level3.com"), info);
        info = locator.find("183.131.7.18");
        Assert.assertEquals(new LocationInfo("中国", "浙江", "杭州", "电信"), info);
        info = locator.find(new byte[]{(byte) 183, (byte) 131, 7, 18});
        Assert.assertEquals(new LocationInfo("中国", "浙江", "杭州", "电信"), info);
        info = locator.find(Locator.bigEndian(new byte[]{(byte) 183, (byte) 131, 7, 18}, 0));
        Assert.assertEquals(new LocationInfo("中国", "浙江", "杭州", "电信"), info);
    }

    @Test
    public void testNetLoadFree() throws IOException {
        Locator l = Locator.loadFromNet("http://7j1xnu.com1.z0.glb.clouddn.com/17monipdb.dat");
        checkFreeLocator(l);
    }

    @Test
    public void testLocalLoadFree() throws IOException {
        Locator l = Locator.loadFromLocal("17monipdb.dat");
        checkFreeLocator(l);
    }

//    @Test
//    public void testLocalLoadFull() throws IOException {
//        Locator l = Locator.loadFromLocal("mydata4vipday2.datx");
//        checkFullLocator(l);
//    }


    public void bench() throws IOException {
        Locator l = Locator.loadFromLocal("17monipdb.dat");
        long t1 = System.currentTimeMillis();
        l.checkDb();
        long t2 = System.currentTimeMillis();
        System.out.println("total time " + (t2 - t1));
        System.out.println("Ops " + (0xffffffffL * 1000 / (t2 - t1)));

    }
}
