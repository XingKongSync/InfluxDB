# InfluxDB
InfluxDB Query Utils in Java

## 简介/Summary

与InfluxDB请求数据时保持长连接，内置Json解析工具，无其他任何依赖。<br/>
This tool can keep alive while querying data from InfluxDB and serialize result with build-in Json serializer without any other dependence.<br/>

## 用法/Usage
```Java
String sql = "SELECT last(ff),at FROM \"2017-05\" WHERE aa = 'C14_L1-9_Ubc' and time>now() - 1h group by time(15s)";
String ip = "192.168.0.121";
int port = 8086;
InfluxDB influxdb = new InfluxDB();
influxdb.setServerIp(ip);
influxdb.setServerPort(port);

InfluxDataAdapter adapter = null;

if (influxdb.Connect()) {
	adapter = influxdb.Query("YZ001", sql);
	influxdb.DisConnect();
}

if (adapter != null) {
	try {
		InfluxResult result = adapter.GetResults().get(0);
		InfluxColumn timeColumn = result.GetColumn("time");
		InfluxColumn ffColumn = result.GetColumn("last");

		if (timeColumn != null && ffColumn != null) {
			InfluxResult.Iterator iter = result.GetIterator();
			while (iter.hasNext()) {
				float ff = iter.GetFloat(ffColumn);
				String time = iter.GetString(timeColumn);
				System.out.println("time: " + time + ", last: " + ff);
				iter.Next();
			}
		}
	} catch (Exception e) {
		System.err.println(e.getMessage());
	}
}

```
