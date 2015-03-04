package sqlboa.model;

import java.util.LinkedList;
import java.util.List;

public class StatementResult {

	private String[] colNames;
	private int totalCount;
	private List<ResultRow> rowList;
	private int row;
	
	public StatementResult(String[] colNames) {
		this.colNames = colNames;

		rowList = new LinkedList<>();
		row = 0;
	}

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

	public void add(ResultRow row) {
		rowList.add(row);
	}
	
	public int getTotalCount() {
		return totalCount;
	}
	
	public int getCount() {
		return rowList.size();
	}
	
	public ResultRow next() {
		return row < rowList.size() ? rowList.get(row++) : null;
	}
	
	public int getColumnIndex(String colName) {
		for (int i = 0; i < colNames.length; i++) {
			if (colNames[i].equalsIgnoreCase(colName)) {
				return i;
			}
		}
		return -1;
	}

	public int getColumnCount() {
		return colNames.length;
	}
	
	public String getColumnName(int idx) {
		return colNames[idx];
	}
}
