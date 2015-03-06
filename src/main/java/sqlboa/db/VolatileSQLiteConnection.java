package sqlboa.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class VolatileSQLiteConnection extends LocalConnection implements Serializable {

	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Throwable t) {
			System.err.println(t);
		}
	}

	private transient Connection connection;
	
	public VolatileSQLiteConnection() {
	}
	
	@Override
	protected Connection connectInternal() throws SQLException {
		if (connection == null || connection.isClosed()) {
			connection = DriverManager.getConnection("jdbc:sqlite::memory:");
		}
		
		return connection;
	}

	@Override
	protected void disconnectInternal(Connection connection) throws SQLException {
		// Don't disconnect or it will go away
	}

	@Override
	public String getName() {
		return "Scratch";
	}
}
