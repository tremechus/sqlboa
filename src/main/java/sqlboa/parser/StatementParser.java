package sqlboa.parser;

import sqlboa.model.BoaStatement;

public class StatementParser {

	private String str;
	
	public StatementParser(String str) {
		this.str = str;
	}
	
    public BoaStatement getStatement(int position) {
    	if (position >= str.length()) {
    		position = str.length()-1;
    	}

    	// First rewind to find the previous statement
    	while (position > 0 && Character.isWhitespace(str.charAt(position--)));
    	
    	int start = findStatementBegin(str, position);
    	int end = findStatementEnd(str, position);
    	
    	String substr = str.substring(start, end).trim();

    	String name = null;
    	String sql = substr;
    	
    	// Does it have a label?
    	int endOfFirstLine = substr.indexOf("\n");
    	if (endOfFirstLine > 0) {
    		String firstLine = substr.substring(0, endOfFirstLine).trim();

    		if (firstLine.endsWith(":")) {
    			name = firstLine.substring(0, firstLine.length()-1).trim();
    			sql = substr.substring(endOfFirstLine).trim();
    		}
    	}
    	
    	return new BoaStatement(name, sql);
    }

    private int findStatementBegin(String str, int position) {
    	if (position <= 0) {
    		return 0;
    	}
    	
    	position = getPreviousNewline(str, position);
    	if (position <= 0) {
    		// Start of string
    		return 0;
    	}
    	
    	// We currently point at a newline
    	position --;
    	while (position >= 0) {
    		if (str.charAt(position) == '\n') {
    			// Found two consecutive newlines, beginning of statement
    			return position;
    		}
    		
    		if (!Character.isWhitespace(str.charAt(position))) {
    			return findStatementBegin(str, position);
    		}
    		
    		position--;
    	}

    	// Hit the beginning of the string
    	return 0;
    }
    
    private int findStatementEnd(String str, int position) {
    	if (position > str.length()) {
    		return str.length();
    	}
    	
    	position = getNextNewline(str, position);
    	if (position > str.length()) {
    		// end of string
    		return str.length();
    	}
    	
    	// We currently point at a newline
    	position ++;
    	while (position < str.length()) {
    		if (str.charAt(position) == '\n') {
    			// Found two consecutive newlines, beginning of statement
    			return position;
    		}
    		
    		if (!Character.isWhitespace(str.charAt(position))) {
    			return findStatementEnd(str, position);
    		}
    		
    		position++;
    	}

    	// Hit the end of the string
    	return str.length();
    }
    
    private int getPreviousNewline(String str, int position) {
    	while (position >= 0 && str.charAt(position) != '\n') position--;
    	return position;
    }

    private int getNextNewline(String str, int position) {
    	while (position < str.length() && str.charAt(position) != '\n') position++;
    	return position;
    }	
}
