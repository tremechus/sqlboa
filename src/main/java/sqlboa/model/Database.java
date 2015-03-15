package sqlboa.model;

import sqlboa.db.DBConnection;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class Database implements Comparable<Database>, Serializable {

	private static final long serialVersionUID = 1L;

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
        try {
            List<String> tableList = connection.list("select name from sqlite_master where type = 'table'");
            Collections.sort(tableList);
            return tableList;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
	}
	
	public List<String> getTriggerList() {
        try {
            List<String> triggerList = connection.list("select name from sqlite_master where type = 'trigger'");
            Collections.sort(triggerList);
            return triggerList;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
	}
	
	public List<String> getIndexList() {
        try {
            List<String> indexList = connection.list("select name from sqlite_master where type = 'index'");
            Collections.sort(indexList);
            return indexList;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

    }
	
	public StatementResult exec(String sql, List<SqlParam> bindParams) throws SQLException {
		return connection.exec(sql, bindParams);
	}
	
	public String getConnectionName() {
		return connection.getName();
	}
	
	public void dropTrigger(String triggerName) throws SQLException {
		connection.exec("drop trigger if exists " + triggerName);
	}
	
	public void dropIndex(String indexName) throws SQLException {
		connection.exec("drop index if exists " + indexName);
	}
	
	public void dropTable(String tableName) throws SQLException {
		connection.exec("drop table if exists " + tableName);
	}

	@Override
	public int compareTo(Database o) {
		return name.compareTo(o.name);
	}

    public boolean isOK() {
        return connection.isOK();
    }
}
