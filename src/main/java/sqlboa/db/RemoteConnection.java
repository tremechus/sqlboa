package sqlboa.db;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import sqlboa.Configuration;
import sqlboa.model.ResultRow;
import sqlboa.model.SqlParam;
import sqlboa.model.StatementResult;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RemoteConnection implements DBConnection, Serializable {

    private String host;

    private transient Socket socket;
    private transient Hessian2Output out;
    private transient Hessian2Input in;

    public RemoteConnection(String host) {
        this.host = host;
    }

    @Override
    public synchronized List<String> queryForStringList(String sql) throws SQLException {
        try {
            ensureConnection();

            out.writeString("queryForStringList");
            out.writeString(sql);
            out.flush();

            List<String> response = (List<String>) in.readObject();

            return response;

        } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to server");
        }
    }

    @Override
    public synchronized void rawExec(String sql) throws SQLException {
        try {
            ensureConnection();

            out.writeString("rawExec");
            out.writeString(sql);
            out.flush();

            String response = in.readString();

            // TODO: Check for error response

        } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to server");
        }
    }

    @Override
    public synchronized StatementResult exec(String sql, List<SqlParam> bindParams) throws SQLException {

        List<Object> paramList = new ArrayList<>();

        // Run substitutions
        for (int i = 1; i <= bindParams.size(); i++) {
            SqlParam param = bindParams.get(i-1); // 1-based system
            Object value = param.getValue();

            if (param.isSubstitution()) {
                sql = sql.replaceAll("\\{" + param.getKey() + "\\}", value != null ? value.toString() : "");
            } else {
                paramList.add(value.toString());
            }
        }

        try {
            ensureConnection();

            out.writeString("exec");
            out.writeString(sql);
            out.writeObject(paramList);
            out.writeInt(Configuration.PAGE_SIZE);
            out.flush();

            StatementResult result = (StatementResult) in.readObject();

            return result;
        } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to server");
        }
    }

    @Override
    public String getName() {
        return host;
    }

    private void ensureConnection() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(host, 1234);

            out = new Hessian2Output(socket.getOutputStream());
            in = new Hessian2Input(socket.getInputStream());
        }
    }

    @Override
    public boolean isOK() {
        // TODO: Do an ACK test
        return true;
    }

}
