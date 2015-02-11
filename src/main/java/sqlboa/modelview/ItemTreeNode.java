package sqlboa.modelview;

import sqlboa.model.Database;

/**
 * Leaf node in the connection tree
 */
public class ItemTreeNode {

    public TreeItemType type;
    public String name;
    public Database db;

    public ItemTreeNode(Database db, String name, TreeItemType type) {
        this.type = type;
        this.name = name;
        this.db = db;
    }

    @Override
    public String toString() {
        return name;
    }
}
