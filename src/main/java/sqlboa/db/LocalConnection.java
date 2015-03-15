package sqlboa.db;

import sqlboa.Configuration;
import sqlboa.model.ResultRow;
import sqlboa.model.StatementResult;
import sqlboa.model.SqlParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public abstract class LocalConnection implements DBConnection {

	public List<String> list(String sql) throws SQLException {
		Connection connection = null;
		try {
			connection = grabConnection();
			
			List<String> results = new ArrayList<>();

			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				results.add(rs.getString(1));
			}

			return results;
		} finally {
			releaseConnection(connection);
		}
	}
	
	public void exec(String sql) throws SQLException {
		Connection connection = grabConnection();
		try {
			Statement stmt = connection.createStatement();
			stmt.execute(sql);
			stmt.close();
		} finally {
			releaseConnection(connection);
		}
	}
	
	public StatementResult exec(String sql, List<SqlParam> bindParams) throws SQLException {
		Connection connection = null;
		try {
			connection = grabConnection();

			// Run substitutions
			for (int i = 1; i <= bindParams.size(); i++) {
				SqlParam param = bindParams.get(i-1); // 1-based system
				Object value = param.getValue();
				
				if (param.isSubstitution()) {
					sql = sql.replaceAll("\\{" + param.getKey() + "\\}", value != null ? value.toString() : "");
				}
			}

			PreparedStatement stmt = connection.prepareStatement(sql);

			// Bind
			int paramOffset = 0;
			if (bindParams != null) {
				
				for (int i = 1; i <= bindParams.size(); i++) {
					SqlParam param = bindParams.get(i-1); // 1-based system
					Object value = param.getValue();
					
					if (param.isSubstitution()) {
						paramOffset++;
						continue;
					}

					if (param.isGlobalLookup()) {
						
					}
					
					stmt.setObject(i - paramOffset, value);
				}
			}
			
			if (stmt.execute()) {
				ResultSet resultSet = stmt.getResultSet();
				ResultSetMetaData meta = resultSet.getMetaData();
				
				String[] colNames = new String[meta.getColumnCount()];
				for (int i = 0; i < colNames.length; i++) {
					colNames[i] = meta.getColumnName(i+1);
				}

				StatementResult result = new StatementResult(colNames);

                // Grab the current page of data
                int rowCount = 0;
				for (; rowCount < Configuration.PAGE_SIZE && resultSet.next(); rowCount++) {
					Object[] colData = new Object[colNames.length];
					for (int j = 0; j < colData.length; j++) {
						colData[j] = resultSet.getObject(j+1);
					}
					ResultRow row = new ResultRow(rowCount, colData);
	
					result.add(row);
				}

                // Skip to the end of the cursor to get total count
                while (resultSet.next()) {
                    rowCount++;
                }

                result.setTotalCount(rowCount);

                return result;
				
			} else {
				
				StatementResult result = new StatementResult(new String[]{"Status"});
				result.add(new ResultRow(-1, new Object[]{stmt.getUpdateCount() + " updated"}));
				
				return result;
			}
		} finally {
			releaseConnection(connection);
		}
	}

	private Connection grabConnection() throws SQLException {
		return connectInternal();
	}

	private void releaseConnection(Connection connection) throws SQLException {
		disconnectInternal(connection);
	}
	
	protected abstract Connection connectInternal() throws SQLException;
	protected abstract void disconnectInternal(Connection connection) throws SQLException;
	public abstract String getName();

}
