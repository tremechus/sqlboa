package sqlboa.model;

import sqlboa.parser.StatementParser;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BoaDocument implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private String body;
	private Map<String, String> paramMap;

	public BoaDocument(String name) {
		this.name = name;
	}
	
	public BoaStatement getStatementAt(int position) {
		return new StatementParser(body).getStatement(position);
	}

	public String getName() {
		return name != null ? name : "";
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getBody() {
		return body != null ? body : "";
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Map<String, String> getParamMap() {
		if (paramMap == null) {
			paramMap = new HashMap<>();
		}
		return paramMap;
	}
	

}
