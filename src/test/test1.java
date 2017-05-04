package test;

import influxdb.*;
import java.util.ArrayList;
import java.util.List;

public class test1 {

	@SuppressWarnings("unused")
	public static void main(String[] args) {
//		String sql = "select * from \"2017-05\" where time > now() - 10";
		String sql = "SELECT last(ff),at FROM \"2017-05\" WHERE aa = 'C14_L1-9_Ubc' and time>now() - 1h group by time(15s)";
		String ip = "192.168.0.121";
		int port = 8086;
		InfluxDB influxdb = new InfluxDB();
		influxdb.setServerIp(ip);
		influxdb.setServerPort(port);

		InfluxDataAdapter adapter = null;
		
		long startTime = System.currentTimeMillis();
		
		if (influxdb.Connect()) {
			adapter = influxdb.Query("YZ001", sql);
			printAdapter(adapter);
			influxdb.DisConnect();
		}
		
		long endTime = System.currentTimeMillis();
		System.out.println(endTime - startTime);
	}
	
	private static void printAdapter(InfluxDataAdapter adapter) {
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
	}
}
