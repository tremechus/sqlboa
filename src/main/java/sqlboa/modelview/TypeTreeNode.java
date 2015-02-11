package sqlboa.modelview;

import sqlboa.model.Database;

/**
 * Mid level tree node
 */
public class TypeTreeNode {

    public TreeItemType type;
    public Database db;

    public TypeTreeNode(Database db, TreeItemType type) {
        this.type = type;
        this.db = db;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
