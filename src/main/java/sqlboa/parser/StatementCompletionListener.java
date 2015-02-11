package sqlboa.parser;

import sqlboa.model.BoaResult;
import sqlboa.model.StatementResult;

public interface StatementCompletionListener {

	public void statementCompleted(BoaResult result);
}
