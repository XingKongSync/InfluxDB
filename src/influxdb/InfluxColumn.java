package influxdb;

public class InfluxColumn {
    private int columIndex;
    private String columName;

    @Override
	public String toString() {
		return "InfluxColum index=" + columIndex + ", name="
				+ columName;
	}

	public InfluxColumn() {

    }

    public InfluxColumn(int columIndex, String columName) {
        setColumIndex(columIndex);
        setColumName(columName);
    }

    public int getColumIndex() {
        return columIndex;
    }

    public void setColumIndex(int columIndex) {
        this.columIndex = columIndex;
    }

    public String getColumName() {
        return columName;
    }

    public void setColumName(String columName) {
        this.columName = columName;
    }
}
