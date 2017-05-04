package influxdb;

public class JsonToken implements IJsonElement {
	public static final int CONST_JSON_FIELD_TYPE_STRING = 0;
	public static final int CONST_JSON_FIELD_TYPE_NUMBER = 1;
	public static final int CONST_JSON_FIELD_TYPE_OBJECT = 2;
	public static final int CONST_JSON_NUMBER_TYPE_INT = 10;
	public static final int CONST_JSON_NUMBER_TYPE_FLOAT = 11;

	private boolean hasKey;
	private String key;
	private int valueType;
	private int numberType;
	private JsonObject oValue;
	private int iValue;
	private float fValue;
	private String sValue;

	public boolean isHasKey() {
		return hasKey;
	}

	public void setHasKey(boolean hasKey) {
		this.hasKey = hasKey;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getValueType() {
		return valueType;
	}

	public void setValueType(int valueType) {
		this.valueType = valueType;
	}

	public int getNumberType() {
		return numberType;
	}

	public void setNumberType(int numberType) {
		this.numberType = numberType;
	}

	public JsonObject getoValue() {
		return oValue;
	}

	public void setoValue(JsonObject oValue) {
		this.oValue = oValue;
	}

	public int getiValue() {
		return iValue;
	}

	public void setiValue(int iValue) {
		this.iValue = iValue;
	}

	public float getfValue() {
		return fValue;
	}

	public void setfValue(float fValue) {
		this.fValue = fValue;
	}

	public String getsValue() {
		return sValue;
	}

	public void setsValue(String sValue) {
		this.sValue = sValue;
	}

	public boolean isNull() {
		if (getValueType() == CONST_JSON_FIELD_TYPE_OBJECT && getoValue() == null) {
			return true;
		} else if (getValueType() == CONST_JSON_FIELD_TYPE_STRING && getsValue() == null) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("JsonToken Key: ");
		if (isHasKey()) {
			sb.append(getKey());
		} else {
			sb.append("null");
		}
		sb.append(" , Value: ");
		switch (getValueType()) {
		case CONST_JSON_FIELD_TYPE_NUMBER:
			if (getValueType() == CONST_JSON_NUMBER_TYPE_FLOAT) {
				sb.append(getfValue());
			} else {
				sb.append(getiValue());
			}
			break;
		case CONST_JSON_FIELD_TYPE_OBJECT:
			if (getoValue() == null) {
				sb.append("null");
			} else {
				sb.append("JsonObject");
			}
			break;
		case CONST_JSON_FIELD_TYPE_STRING:
			sb.append(getsValue());
			break;
		default:
			break;
		}
		return sb.toString();
	}

	@Override
	public int getJsonElementType() {
		return JsonElementType.CONST_JSON_TOKEN;
	}

	@Override
	public JsonObject getJsonObject() {
		return null;
	}

	@Override
	public JsonToken getJsonToken() {
		return this;
	}
}
