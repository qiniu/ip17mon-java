IP 17mon java version, 比官方的速度快很多，支持监视文件改动自动加载

IP库请从 ipip.net 下载

使用方法：

首选构建一个 `Locator` ，支持从不同的地方加载IP库，比如远程URL，或者本地的文件。

```
//从URL加载IP库
String ipBaseUrl = "http://xxx.example.com/ip.dat";
Locator locator = Locator.loadFromNet(ipBaseUrl);

//从本地加载IP库
String ipBasePath = "/tmp/ip.dat";
Locator locator = Locator.loadFromLocal(ipBasePath);
```
然后使用 `find` 方法查询：

```
LocationInfo ipInfo = locator.find("180.163.159.7");
System.out.println(ipInfo.toString());
```
