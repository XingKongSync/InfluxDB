package influxdb;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据示例
 * {
    "results": [
        {
            "series": [
                {
                    "name": "2017-05",
                    "columns": [
                        "time",
                        "aa",
                        "at",
                        "ff",
                        "fi"
                    ],
                    "values": [
                        [
                            "2017-05-02T07:47:32.0634542Z",
                            "C26_H202_S",
                            "2",
                            2.75,
                            null
                        ]
                    ]
                }
            ]
        }
    ]
}
 */

public class InfluxDataAdapter {
    private JsonObject influxResponse;

    public InfluxDataAdapter(JsonObject influxResponse) {
        this.influxResponse = influxResponse;
    }

    public List<InfluxResult> GetResults() throws Exception {
        ArrayList<InfluxResult> results = new ArrayList<InfluxResult>();
        if (influxResponse != null && influxResponse.getObjType() == JsonObject.CONST_JSON_OBJECT_TYPE_OBJECT) {
            List<JsonToken> tokenList = influxResponse.getTokenList();
            if (!tokenList.isEmpty()) {
                JsonToken resultsToken = tokenList.get(0);
                if (resultsToken.getKey().equals("results")) {
                    if (resultsToken.getValueType() == JsonToken.CONST_JSON_FIELD_TYPE_OBJECT) {
                        JsonObject resultsValue = resultsToken.getoValue();
                        if (resultsValue != null && resultsValue.getObjType() == JsonObject.CONST_JSON_OBJECT_TYPE_ARRAY) {
                            List<JsonToken> resultsList = resultsValue.getTokenList();
                            if (resultsList != null) {
                                for (int i = 0; i < resultsList.size(); i++) {
                                    JsonToken rToken = resultsList.get(i);
                                    if (rToken.getValueType() == JsonToken.CONST_JSON_FIELD_TYPE_OBJECT) {
                                        InfluxResult influxResult = new InfluxResult(i, rToken.getoValue());
                                        results.add(influxResult);
                                    } else {
                                        throw new Exception("result数组中的元素必须是JsonObject");
                                    }
                                }
                            }
                            return results;
                        }
                        throw new Exception("result不能为null，并且必须是JsonArray");
                    }
                    throw new Exception("results的类型必须为JsonArray");
                }
            }
            throw new Exception("无法找到Key为results的JsonToken");
        }
        throw new Exception("influxResponse不能为null，并且不能是JsonArray");
    }

}
