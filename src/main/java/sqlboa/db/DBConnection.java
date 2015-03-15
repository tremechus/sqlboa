package sqlboa.db;

import sqlboa.Configuration;
import sqlboa.model.ResultRow;
import sqlboa.model.SqlParam;
import sqlboa.model.StatementResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public interface DBConnection {

	public List<String> list(String sql) throws SQLException;
	public void exec(String sql) throws SQLException;
	public StatementResult exec(String sql, List<SqlParam> bindParams) throws SQLException;
	public String getName();
    public boolean isOK();
}
