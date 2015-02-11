package sqlboa.model;

/**
 * Created by trevor on 12/8/2014.
 */
public class BoaResult {

    private BoaStatement statement;
    private StatementResult result;
    private long duration;

    public BoaResult(BoaStatement statement, StatementResult result, long duration) {
        this.statement = statement;
        this.result = result;
        this.duration = duration;
    }

    public BoaStatement getStatement() {
        return statement;
    }

    public StatementResult getResult() {
        return result;
    }

    public long getDuration() {
        return duration;
    }
}
