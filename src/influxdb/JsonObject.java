package influxdb;

import java.util.ArrayList;

public class JsonObject implements IJsonElement {
	public static final int CONST_JSON_OBJECT_TYPE_OBJECT = 0;
	public static final int CONST_JSON_OBJECT_TYPE_ARRAY = 1;

	private int objType;
	private ArrayList<JsonToken> tokenList;
	// private ArrayList<JsonObject> objList;

	public int getObjType() {
		return objType;
	}

	public void setObjType(int objType) {
		this.objType = objType;
	}

	public ArrayList<JsonToken> getTokenList() {
		if (tokenList == null) {
			tokenList = new ArrayList<JsonToken>();
		}
		return tokenList;
	}

	public void setTokenList(ArrayList<JsonToken> tokenList) {
		this.tokenList = tokenList;
	}

	// public ArrayList<JsonObject> getObjList() {
	// 	if (objList == null) {
	// 		objList = new ArrayList<JsonObject>();
	// 	}
	// 	return objList;
	// }

	// public void setObjList(ArrayList<JsonObject> objList) {
	// 	this.objList = objList;
	// }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Json");
		switch (getObjType()) {
		case CONST_JSON_OBJECT_TYPE_ARRAY:
			sb.append("Array");
			break;
		case CONST_JSON_OBJECT_TYPE_OBJECT:
			sb.append("Object");
			break;
		default:
			break;
		}
		sb.append(" TokenCount: ");
		sb.append(getTokenList().size());
		return sb.toString();
	}

	@Override
	public int getJsonElementType() {
		return JsonElementType.CONST_JSON_OBJECT;
	}

	@Override
	public JsonObject getJsonObject() {
		return this;
	}

	@Override
	public JsonToken getJsonToken() {
		return null;
	}

}
