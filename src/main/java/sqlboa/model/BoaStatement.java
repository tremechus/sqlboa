package sqlboa.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BoaStatement implements Serializable {

	private static final long serialVersionUID = 1L;

    private static final String PREFIX_QUERY_PLAN = "explain query plan ";

	private String name;
	private String sql;
	private List<SqlParam> paramList = new LinkedList<>();
    private boolean showTiming;
    private boolean showQueryPlan;
    private String usingDb;

    public BoaStatement(String name, String sql) {
        this(name, sql, true);
    }

	public BoaStatement(String name, String sql, boolean showTiming) {
		this.name = name != null && name.trim().length() > 0 ? name : "Results";
		this.sql = sql;
        this.showTiming = showTiming;

        captureUsing();
        captureBindParams();
	}

    public void setShowQueryPlan() {
        if (!sql.startsWith(PREFIX_QUERY_PLAN)) {
            sql = PREFIX_QUERY_PLAN + sql;
        }

        name = name + ": Plan";
    }

    private void captureUsing() {
        if (!sql.toLowerCase().startsWith("using ")) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        boolean complete = false;
        Character quoteChar = null;
        int idx = "using ".length();
        for (; !complete && idx < sql.length(); idx++) {
            char ch = sql.charAt(idx);
            switch (ch) {
                case '\n':
                case ' ':
                case '\t':
                    if (quoteChar == null) {
                        usingDb = builder.toString();
                        sql = sql.substring(idx);
                        complete = true;
                        continue;
                    }
                    break;
                case '\'':
                    if (quoteChar == null) {
                        quoteChar = '\'';
                        continue;
                    }
                    if (quoteChar == '\'') {
                        quoteChar = null;
                        continue;
                    }
                    break;
                case '"':
                    if (quoteChar == null) {
                        quoteChar = '"';
                        continue;
                    }
                    if (quoteChar == '"') {
                        quoteChar = null;
                        continue;
                    }
                    break;
            }
            builder.append(ch);
        }
    }

    public boolean getShowTiming() {
        return showTiming;
    }

    public String getUsingDb() {
        return usingDb;
    }

	public String getName() {
		return name;
	}
	
	public String getSQL() {
		return sql;
	}

	public boolean isValid() {
		return sql != null && sql.trim().length() > 0;
	}
	
	private void captureBindParams() {

		// First wipe out all string literals
        int indexedParamCount = 0;
		StringBuilder sqlBuilder = new StringBuilder();
		StringBuilder paramNameBuilder = new StringBuilder();
		Character quote = null;
		Character captureType = null;
		for (int i = 0; i < sql.length(); i++) {
			
			char ch = sql.charAt(i);
			switch (ch) {
			case '\'':
			case '"':
				// String literal start?
				if (quote == null) {
					quote = ch;
					break;
				}
				
				if (quote == ch) {
					// Escaped?
					if (sql.charAt(i-1) == '\\') {
						break;
					}
					
					// String literal end
					quote = null;
				}
				break;
			case '?':
			case ':':
			case '{':
			case '#':
				if (quote == null) {
					captureType = ch;
				}
				
				break;
			case '}':
				if (captureType != null && captureType == '{') {
					addBindParam(captureType, paramNameBuilder.toString());

					// Put the capture back so that we can replace it later
					sqlBuilder.append(" {" + paramNameBuilder.toString() + "}");

					captureType = null;
					paramNameBuilder.setLength(0);
					
					continue;
				}
			case ' ':
			case '\n':
				if (captureType != null && captureType != '{') {

                    String paramName = paramNameBuilder.toString();
                    if (captureType == '?') {
                        paramName = "(" + (++indexedParamCount) + ")";
                    }

					addBindParam(captureType, paramName);
					
					captureType = null;
					paramNameBuilder.setLength(0);
					sqlBuilder.append(" ? "); // bound name placeholder
				}
			default:
				if (captureType != null) {
					paramNameBuilder.append(ch);
				}
			}
			
			// Rebuild the sql statement replacing all named params with '?'
			if (captureType == null) {
				sqlBuilder.append(ch);
			}
		}
		if (paramNameBuilder.length() > 0 || captureType != null) {
            String paramName = paramNameBuilder.toString();
            if (captureType == '?') {
                paramName = "(" + (++indexedParamCount) + ")";
            }

            addBindParam(captureType, paramName);
			sqlBuilder.append(" ? ");
		}
		
		sql = sqlBuilder.toString();
	}

	private void addBindParam(char type, String name) {
		SqlParam.Type paramType = null;
        if (type == ':') {
            paramType = SqlParam.Type.NAMED;
        }
		if (type == '?' && (name != null && name.length() == 0)) {
			name = "?";
			paramType = SqlParam.Type.INDEXED;
		}
		if (type == '{') {
			paramType = SqlParam.Type.SUBSTITUTION;
		}
		if (type == '#') {
			paramType = SqlParam.Type.GLOBAL;
		}

        if (paramType != null) {
            paramList.add(new SqlParam(paramType, name, null));
        }
	}
	
	public List<SqlParam> getParamList() {
		return paramList;
	}

	public void applyParams(List<SqlParam> paramList) {
		// TODO: how to handle orphaned values?
		for (SqlParam param : paramList) {
			for (SqlParam original : this.paramList) {
				if (original.getKey().equals(param.getKey())) {
					original.setValue(param.getValue());
				}
			}
		}
	}
	
}
