package sqlboa.db;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import jdk.nashorn.api.scripting.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RemoteConnection implements DBConnection, Serializable {

    private static final int ISOK_DURATION = 1000 * 10; // Duration between isOK checks

    private String host;

    private transient Socket socket;
    private transient Hessian2Output out;
    private transient Hessian2Input in;

    private transient boolean isOK;
    private transient long lastOKCheck;

    public RemoteConnection(String host) {
        this.host = host;
    }

    @Override
    public synchronized List<String> list(String sql) throws SQLException {
        try {
            ensureConnection();

            JSONObject request = new JSONObject();
            request.put("command", "list");
            request.put("query", sql);
            out.writeString(request.toString());
            out.flush();

            JSONObject response = new JSONObject(in.readString());
            checkResponse(response);

            JSONArray list = response.getJSONArray("list");
            List<String> resultList = new ArrayList<>();

            for (int i = 0; i < list.length(); i++) {
                resultList.add(list.getString(i));
            }

            return resultList;

        } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to server");
        }
    }

    @Override
    public synchronized void exec(String sql) throws SQLException {
        try {
            ensureConnection();

            JSONObject request = new JSONObject();
            request.put("command", "exec");
            request.put("sql", sql);
            out.writeString(request.toString());
            out.flush();

            JSONObject response = new JSONObject(in.readString());
            checkResponse(response);

        } catch (IOException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to server");
        }
    }

    @Override
    public synchronized StatementResult exec(String sql, List<SqlParam> bindParams) throws SQLException {

        JSONArray paramList = new JSONArray();

        // Run substitutions
        for (int i = 1; i <= bindParams.size(); i++) {
            SqlParam param = bindParams.get(i-1); // 1-based system
            Object value = param.getValue();

            if (param.isSubstitution()) {
                sql = sql.replaceAll("\\{" + param.getKey() + "\\}", value != null ? value.toString() : "");
            } else {
                paramList.put(value.toString());
            }
        }

        try {
            ensureConnection();

            JSONObject request = new JSONObject();
            request.put("command", "query");
            request.put("query", sql);
            request.put("params", paramList);
            request.put("page", 0);
            request.put("perPage", Configuration.PAGE_SIZE);
            out.writeString(request.toString());
            out.flush();

            JSONObject response = new JSONObject(in.readString());
            checkResponse(response);

            JSONArray columns = response.getJSONArray("columns");
            String[] colNames = new String[columns.length()];
            for (int i = 0; i < columns.length(); i++) {
                colNames[i] = columns.getString(i);
            }

            StatementResult result = new StatementResult(colNames);
            result.setTotalCount(response.optInt("totalCount", 0));

            JSONArray rows = response.getJSONArray("rows");
            for (int i = 0; i < rows.length(); i++) {
                Object[] vals = new Object[colNames.length];

                JSONArray rowVals = rows.getJSONArray(i);

                for (int j = 0; j < vals.length; j++) {
                    vals[j] = rowVals.get(j);
                }

                result.add(new ResultRow(i, vals));
            }

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
        // For now just create a new socket
        if (socket != null) {
            socket.close();
        }

        socket = new Socket(host, 1234);

        out = new Hessian2Output(socket.getOutputStream());
        in = new Hessian2Input(socket.getInputStream());
    }

    @Override
    public boolean isOK() {

        if (System.currentTimeMillis() - lastOKCheck < ISOK_DURATION) {
            return isOK;
        }

        lastOKCheck = System.currentTimeMillis();
        isOK = false;
        try {
            ensureConnection();

            JSONObject request = new JSONObject();
            request.put("command", "handshake");
            out.writeString(request.toString());
            out.flush();

            JSONObject response = new JSONObject(in.readString());
            checkResponse(response);

            isOK = true;
        } catch (IOException|SQLException e) {
            // Expected
            System.out.println("Can't connect to " + getName());
        }

        return isOK;
    }

    private void checkResponse(JSONObject response) throws SQLException {
        if (response == null) {
            throw new SQLException("No response");
        }

        if (response.getInt("status") == 0) {
            throw new SQLException("Error code " + response.getInt("code") + ": " + response.getString("message"));
        }
    }

}
