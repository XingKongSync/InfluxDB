package influxdb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class UrlEncoder {
    private static Map<String, String> replaceMap;

    private static void CheckReplaceMap()
    {
        if (replaceMap == null) {
            replaceMap = new HashMap<String, String>();
            replaceMap.put("\"", "%22");
            replaceMap.put(">", "%3E");
            replaceMap.put("<", "%3C");
            replaceMap.put(" ", "%20");
        }
    }

    public static String EncodeString(String url)
    {
        CheckReplaceMap();
        Iterator<Entry<String, String>> iter = replaceMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry<String, String>) iter.next();
            url = url.replace(entry.getKey(), entry.getValue());
        }
        return url;
    }
}