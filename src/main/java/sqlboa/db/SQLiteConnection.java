package sqlboa.db;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteConnection extends DBConnection implements Serializable {

	private static final long serialVersionUID = 1L;

	private File file;
	
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Throwable t) {
			System.err.println(t);
		}
	}
	
	public SQLiteConnection(Path path) {
		file = path.toFile();
	}
	
	@Override
	protected Connection connectInternal() throws SQLException {
		String connectionString = "jdbc:sqlite:" + file;
		return DriverManager.getConnection(connectionString);
	}

	@Override
	protected void disconnectInternal(Connection connection) throws SQLException {
		if (connection != null && !connection.isClosed()) {
			connection.close();
		}
	}

	@Override
	public String getName() {
		return file.getAbsolutePath();
	}
}
