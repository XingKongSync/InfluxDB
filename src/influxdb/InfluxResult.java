package influxdb;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据示例
 * 
 * resultObject
 * {
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

 *
 * seriesValueObject
 * {
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
 */

public class InfluxResult {
    private int resultIndex;
    private JsonObject resultObject;
    private JsonObject seriesValueObject;
    private JsonObject columnsObject;
    private JsonObject valuesObject;

    public InfluxResult() {

    }

    public InfluxResult(int resultIndex, JsonObject resultObject) throws Exception {
        setResultIndex(resultIndex);
        setResultObject(resultObject);
    }

    public int getResultIndex() {
        return resultIndex;
    }

    public void setResultIndex(int resultIndex) {
        this.resultIndex = resultIndex;
    }

    public JsonObject getResultObject() {
        return resultObject;
    }

    public void setResultObject(JsonObject resultObject) throws Exception {
        this.resultObject = resultObject;

        seriesValueObject = GetSeriesValueObject();
        columnsObject = GetcolumnsObject();
        valuesObject = GetValueObject();
    }

    private JsonObject GetSeriesValueObject() throws Exception {
        if (resultObject != null) {
            List<JsonToken> tokenList = resultObject.getTokenList();
            if (tokenList != null && !tokenList.isEmpty()) {
                JsonToken seriesToken = tokenList.get(0);
                if (seriesToken.getKey().equals("series")) {
                    JsonObject seriesValue = seriesToken.getoValue();
                    if (seriesValue != null) {
                        if (seriesValue.getObjType() == JsonObject.CONST_JSON_OBJECT_TYPE_ARRAY) {
                            List<JsonToken> seriesInside = seriesValue.getTokenList();
                            if (!seriesInside.isEmpty()) {
                                return seriesInside.get(0).getoValue();
                            }
                            throw new Exception("series的值数组长度不能为0");
                        }
                        throw new Exception("series的值必须为JsonArray");
                    }
                    throw new Exception("series的值不能为null");
                }
                throw new Exception("找不到series的JsonToken");
            }
            throw new Exception("找不到series的JsonToken");
        }
        throw new Exception("resultObject不能为null");
    }

    private JsonObject GetcolumnsObject() throws Exception {
        if (seriesValueObject != null) {
            List<JsonToken> tokenList = seriesValueObject.getTokenList();
            if (tokenList != null && !tokenList.isEmpty()) {
                for (int i = 0; i < tokenList.size(); i++) {
                    JsonToken token = tokenList.get(i);
                    if (token.getKey().equals("columns")) {
                        return token.getoValue();
                    }
                }
                throw new Exception("在series中找不到columns");
            }
            throw new Exception("series的tokenList不能为空");
        }
        throw new Exception("seriesValue不能为null");
    }

    private JsonObject GetValueObject() throws Exception {
        if (seriesValueObject != null) {
            List<JsonToken> tokenList = seriesValueObject.getTokenList();
            if (tokenList != null && !tokenList.isEmpty()) {
                for (int i = 0; i < tokenList.size(); i++) {
                    JsonToken token = tokenList.get(i);
                    if (token.getKey().equals("values")) {
                        return token.getoValue();
                    }
                }
                throw new Exception("在series中找不到values");
            }
            throw new Exception("series的tokenList不能为空");
        }
        throw new Exception("seriesValue不能为null");
    }

    public List<InfluxColumn> Getcolumns() {
        ArrayList<InfluxColumn> columns = new ArrayList<InfluxColumn>();
        if (columnsObject != null) {
            List<JsonToken> tokenList = columnsObject.getTokenList();
            if (tokenList != null && !tokenList.isEmpty()) {
                for (int i = 0; i < tokenList.size(); i++) {
                    JsonToken token = tokenList.get(i);
                    InfluxColumn column = new InfluxColumn(i, token.getsValue());
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    public InfluxColumn GetColumn(String colName) {
        List<InfluxColumn> columns = Getcolumns();
        for (int i = 0; i < columns.size(); i++) {
            InfluxColumn col = columns.get(i);
            if (col.getColumName().equals(colName)) {
                return col;
            }
        }
        return null;
    }

    public int getValueCount() throws Exception {
        if (valuesObject != null) {
            return valuesObject.getTokenList().size();
        }
        throw new Exception("valuesObject不能为null");
    }

    public Iterator GetIterator() throws Exception {
        Iterator iter = new Iterator(valuesObject);
        return iter;
    }

    public class Iterator {
        private JsonObject valueObject;
        private List<JsonToken> tokenList;

        private JsonObject currentRow;
        private int i;

        public Iterator(JsonObject valueObject) throws Exception {
            this.valueObject = valueObject;
            if (valueObject == null || valueObject.getObjType() != JsonObject.CONST_JSON_OBJECT_TYPE_ARRAY) {
                throw new Exception("Iterator的valueObject不能合法");
            }
            tokenList = valueObject.getTokenList();
            if (tokenList == null) {
                throw new Exception("values的TokenList不能为null");
            }
            i = 0;
        }

        public boolean hasNext() {
            if (i < valueObject.getTokenList().size()) {
                return true;
            } else {
                return false;
            }
        }

        public void SetPointer(int newpointer) {
            i = newpointer;
        }

        public void Reset() {
            i = 0;
        }

        public void Next() {
            i++;
        }

        public boolean IsNull(InfluxColumn column) {
            currentRow = tokenList.get(i).getoValue();
            return currentRow.getTokenList().get(column.getColumIndex()).isNull();
        }

        public int GetColumnValueType(InfluxColumn column) {
            currentRow = tokenList.get(i).getoValue();
            return currentRow.getTokenList().get(column.getColumIndex()).getValueType();
        }

        public int GetInt(InfluxColumn column) {
            currentRow = tokenList.get(i).getoValue();
            return currentRow.getTokenList().get(column.getColumIndex()).getiValue();
        }

        public float GetFloat(InfluxColumn column) {
            currentRow = tokenList.get(i).getoValue();
            JsonToken currentValue = currentRow.getTokenList().get(column.getColumIndex());
            if (currentValue.getNumberType() == JsonToken.CONST_JSON_NUMBER_TYPE_INT) {
				return currentValue.getiValue();
			} else {
				return currentValue.getfValue();
			}
        }

        public JsonObject GetObject(InfluxColumn column) {
            currentRow = tokenList.get(i).getoValue();
            return currentRow.getTokenList().get(column.getColumIndex()).getoValue();
        }

        public String GetString(InfluxColumn column) {
            currentRow = tokenList.get(i).getoValue();
            return currentRow.getTokenList().get(column.getColumIndex()).getsValue();
        }
    }
}
