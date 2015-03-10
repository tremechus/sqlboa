package sqlboa.modelview;

import sqlboa.model.Database;

/**
 * represent top level node in connection tree
 */
public class ConnectionTreeNode {

    public Database db;

    public ConnectionTreeNode(Database db) {
        this.db = db;
    }

    @Override
    public String toString() {
        return db.getName();
    }
}
