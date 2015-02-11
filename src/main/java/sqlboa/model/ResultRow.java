package sqlboa.model;

public class ResultRow {

	private long rowId;
	private Object[] colValues;
	
	public ResultRow(long rowId, Object[] colValues) {
		this.rowId = rowId;
		this.colValues = colValues;
	}
	
	public long getRowId() {
		return rowId;
	}
	
	public Object get(int idx) {
		return colValues[idx];
	}
}
