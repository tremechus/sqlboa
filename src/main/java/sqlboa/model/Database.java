package sqlboa.model;

import sqlboa.db.DBConnection;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Database implements Comparable<Database>, Serializable {

	private static final long serialVersionUID = 1L;

	private transient List<String> tableList;
	private transient List<String> triggerList;
	private transient List<String> indexList;
	
	private String name;
	private DBConnection connection;
	
	public Database(String name, DBConnection connection) {
		this.name = name;
		this.connection = connection;
	}
	
	public String getName() {
		return name;
	}
	
	public List<String> getTableList() {
		return tableList;
	}
	
	public List<String> getTriggerList() {
		return triggerList;
	}
	
	public List<String> getIndexList() {
		return indexList;
	}
	
	public StatementResult exec(String sql, List<SqlParam> bindParams) throws SQLException {
		return connection.exec(sql, bindParams);
	}
	
	public String getConnectionName() {
		return connection.getName();
	}
	
	public void dropTrigger(String triggerName) throws SQLException {
		connection.rawExec("drop trigger if exists " + triggerName);
	}
	
	public void dropIndex(String indexName) throws SQLException {
		connection.rawExec("drop index if exists " + indexName);
	}
	
	public void dropTable(String tableName) throws SQLException {
		connection.rawExec("drop table if exists " + tableName);
	}
	
	public void refresh() throws SQLException {

		List<String> tableList = new ArrayList<>(0);
		List<String> triggerList = new ArrayList<>(0);
		List<String> indexList = new ArrayList<>(0);

		try {
	
			tableList = connection.queryForStringList("select name from sqlite_master where type = 'table'");
			Collections.sort(tableList);
			
			triggerList = connection.queryForStringList("select name from sqlite_master where type = 'trigger'");
			Collections.sort(triggerList);
			
			indexList = connection.queryForStringList("select name from sqlite_master where type = 'index'");
			Collections.sort(indexList);
			
		} finally {
			
			this.tableList = Collections.unmodifiableList(tableList);
			this.triggerList = Collections.unmodifiableList(triggerList);
			this.indexList = Collections.unmodifiableList(indexList);
		}
		
	}

	@Override
	public int compareTo(Database o) {
		return name.compareTo(o.name);
	}
}
