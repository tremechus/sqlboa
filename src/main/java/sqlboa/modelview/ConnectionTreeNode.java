package sqlboa.modelview;

import sqlboa.model.Database;

/**
 * represent top level node in connection tree
 */
public class ConnectionTreeNode {

    public Database db;
    public boolean isOK;

    public ConnectionTreeNode(Database db, boolean isOK) {
        this.db = db;
        this.isOK = isOK;
    }

    @Override
    public String toString() {
        return db.getName();
    }
}
