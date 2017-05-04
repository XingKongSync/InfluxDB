package influxdb;

public interface IJsonElement {
    public int getJsonElementType();

    public JsonObject getJsonObject();

    public JsonToken getJsonToken();
}
