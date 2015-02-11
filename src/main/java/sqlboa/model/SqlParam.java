package sqlboa.model;

public class SqlParam {

	public enum Type {
        SUBSTITUTION,
		NAMED,
		INDEXED,
        GLOBAL
	}
	
	private Type type;
	private String key;
	private Object value;
	
	public SqlParam(Type type, String key, Object value) {
		this.type = type;
		this.key = key;
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}

    public Type getType() {
        return type;
    }

	public boolean isNamed() {
		return type == Type.NAMED;
	}
	
	public boolean isIndexed() {
		return type == Type.INDEXED;
	}
	
	public boolean isSubstitution() {
		return type == Type.SUBSTITUTION;
	}
	
	public boolean isGlobalLookup() {
		return type == Type.GLOBAL;
	}
}
