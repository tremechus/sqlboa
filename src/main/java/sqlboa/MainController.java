package sqlboa;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import name.antonsmirnov.javafx.dialog.Dialog;
import sqlboa.db.SQLiteConnection;
import sqlboa.model.*;
import sqlboa.modelview.ConnectionTreeNode;
import sqlboa.modelview.ItemTreeNode;
import sqlboa.modelview.TypeTreeNode;
import sqlboa.modelview.TreeItemType;
import sqlboa.parser.StatementCompletionListener;
import sqlboa.state.AppState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class MainController implements StatementCompletionListener {

    private static final int MAX_STRING_LENGTH = 250;
    private static final int MAX_BINARY_LENGTH = 50;

    @FXML
    private TreeView<Object> connectionTree;

    @FXML
    private TabPane sheetTabs;

    @FXML
    private TabPane resultTabs;

    private SQLBoa boa;
    private AppState appState;
    private Stage stage;
    private FileChooser fileChooser = new FileChooser();
    private Application app;

    public void init(Application app, SQLBoa boa, AppState appState, Stage stage) {
        this.stage = stage;
        this.boa = boa;
        this.app = app;
        this.appState = appState;

        initResultTabs();
        initConnectionTree();
        initSheetTabs();
        initMenus();

    }

    private void initMenus() {

    }

    private void initResultTabs() {
        resultTabs.getTabs().clear();

        addResult("Results", null, new TextArea(), null);
    }

    private void initConnectionTree() {
        List<Database> dbList = appState.getDatabaseList();

        TreeItem root = new TreeItem();
        root.setExpanded(true);
        connectionTree.setRoot(root);
        connectionTree.setShowRoot(false);

        // Initial state
        if (dbList == null || dbList.size() == 0) {
            root.getChildren().add(new TreeItem<>("No connections"));
            return;
        }

        // Construct tree
        Collections.sort(dbList);

        if (appState.getDefaultDatabase() == null && dbList.size() > 0) {
            appState.setDefaultDatabase(dbList.get(0).getName());
        }

        // Verify that we still have a default database
        boolean found = false;
        for (Database db : dbList) {
            if (appState.getDefaultDatabase().equals(db.getName())) {
                found = true;
                break;
            }
        }
        if (!found) {
            if (dbList.size() > 0) {
                appState.setDefaultDatabase(dbList.get(0).getName());
            } else {
                appState.setDefaultDatabase(null);
            }
        }

        TreeItem selectedBranch = null;
        for (Database db : dbList) {
            TreeItem branch = constructDBBranch(db);
            root.getChildren().add(branch);

            if (appState.getDefaultDatabase().equals(db.getName())) {
                selectedBranch = branch;
            }
        }

        if (selectedBranch != null) {
            connectionTree.getSelectionModel().select(selectedBranch);
            connectionTree.getFocusModel().focus(connectionTree.getRow(selectedBranch));
        } else {
            connectionTree.getSelectionModel().select(0);
            connectionTree.getFocusModel().focus(0);
        }

        connectionTree.getSelectionModel().selectedItemProperty().addListener(e -> {
            if (connectionTree.getSelectionModel().getSelectedItem() == null) {
                return;
            }

            appState.setDefaultDatabase(connectionTree.getSelectionModel().getSelectedItem().getValue().toString());
            appState.save();
        });
        connectionTree.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2) {
                TreeItem<Object> item = connectionTree.getSelectionModel().getSelectedItem();

                if (item != null) {
                    Object value = item.getValue();
                    if (value != null && value instanceof ItemTreeNode) {
                        ItemTreeNode details = (ItemTreeNode) value;

                        switch (details.type) {
                            case TABLE:
                                boa.executeStatement(details.db, null, new BoaStatement("Table: " + details.name, "PRAGMA table_info(" + details.name +")", false), this);
                                break;
                            case DATABASE:
                                break;
                            case INDEX:
                                boa.executeStatement(details.db, null, new BoaStatement("Index: " + details.name, "select sql from sqlite_master where type = 'index' and name = '" + details.name +"'", false), this);
                                break;
                            case TRIGGER:
                                boa.executeStatement(details.db, null, new BoaStatement("Trigger: " + details.name, "select sql from sqlite_master where type = 'trigger' and name = '" + details.name +"'", false), this);
                                break;
                        }
                    }
                }
            }
        });
    }

    private void addConnection(Database db) {

        TreeItem root = connectionTree.getRoot();
        if (root.getChildren().size() == 0 || ((TreeItem) root.getChildren().get(0)).getValue() instanceof String) {
            // Clear out the "No connections" label
            root.getChildren().clear();
        }

        root.getChildren().add(constructDBBranch(db)); // TODO: Check isOK value

    }

    private void initSheetTabs() {
        sheetTabs.getTabs().clear();

        if (appState.getDocumentList().size() > 0) {
            for (BoaDocument document : appState.getDocumentList()) {
                addSheet(document);
            }
        } else {
            addSheet(createDefaultDocument());
        }

    }

    public Tab addSheet(final BoaDocument doc) {

        TextArea text = new TextArea();
        text.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                    executeDocument(false);
                }
                if (event.isControlDown() && event.getCode() == KeyCode.R) {
                    executeDocument(true);
                }
            }
        });
        text.textProperty().addListener(new DocumentChangeListener(doc));
        text.setText(doc.getBody());

        Tab tab = new DocumentTab(doc.getName(), doc);
        tab.setContent(text);
        tab.setOnClosed(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                appState.getDocumentList().remove(doc);
                if (sheetTabs.getTabs().size() == 0) {
                    addSheet(createDefaultDocument());
                }
                appState.save();
            }
        });

        sheetTabs.getTabs().add(tab);

        if (!appState.getDocumentList().contains(doc)) {
            appState.getDocumentList().add(doc);
        }

        return tab;
    }

    public void addResult(String name, String detail, Node content, Object data) {
        if (name == null) {
            name = "Results";
        }

        if (detail == null) {
            detail = "";
        }

        if (content == null) {
            TextArea emptyPanel = new TextArea();
            emptyPanel.setEditable(false);
            emptyPanel.setFocusTraversable(false);

            content = emptyPanel;
        }

        ResultTab tab = null;
        for (Tab t : resultTabs.getTabs()) {
            ResultTab currTab = (ResultTab) t;
            if (name.equalsIgnoreCase(currTab.name)) {
                tab = currTab;
                break;
            }
        }

        if (tab == null) {
            tab = new ResultTab(name, data);
            resultTabs.getTabs().add(tab);
        }

        tab.setText(name + detail);

        tab.setContent(content);

        resultTabs.getSelectionModel().select(tab);
    }

    public void handleRemoveConnection() {
        TreeItem treeItem = (TreeItem) connectionTree.getSelectionModel().getSelectedItem();
        if (treeItem == null || treeItem.getValue() instanceof String) {
            return;
        }

        Object data = treeItem.getValue();
        if (data instanceof ConnectionTreeNode) {
            boa.removeDB(((ConnectionTreeNode) treeItem.getValue()).db);
            connectionTree.getRoot().getChildren().remove(treeItem);
        }
    }

    @Override
    public void statementCompleted(BoaResult result) {
        if (result == null) {
            System.err.println("Null result: " + result);
            return;
        }

        TableView<ResultRow> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.addEventHandler(KeyEvent.KEY_PRESSED, e -> {

            if (e.isControlDown() && e.getCode() == KeyCode.C) {

                ObservableList<TablePosition> posList = table.getSelectionModel().getSelectedCells();
                int old_r = -1;
                StringBuilder clipboardString = new StringBuilder();
                for (TablePosition p : posList) {
                    int r = p.getRow();
                    int c = p.getColumn();

                    ResultRow row = table.getItems().get(p.getRow());
                    if (row == null) {
                        continue;
                    }

                    Object cell = row.get(p.getColumn());
                    if (cell == null)
                        cell = "";
                    if (old_r == r)
                        clipboardString.append('\t');
                    else if (old_r != -1)
                        clipboardString.append('\n');
                    clipboardString.append(cell);
                    old_r = r;
                }
                final ClipboardContent content = new ClipboardContent();
                content.putString(clipboardString.toString());
                Clipboard.getSystemClipboard().setContent(content);

                e.consume();
            }
        });

        for (int i = 0; i < result.getResult().getColumnCount(); i++) {
            String colName = result.getResult().getColumnName(i);

            // FIXME:
            final int idx = i;
            TableColumn<ResultRow, String> col = new TableColumn<>(colName);
            col.setCellValueFactory(p -> {
                Object obj = p.getValue().get(idx);
                if (obj instanceof String) {
                    String str = (String) obj;
                    if (str.length() > MAX_STRING_LENGTH) {
                        obj = str.substring(0, MAX_STRING_LENGTH) + "...";
                    }
                }

                if (obj instanceof byte[]) {
                    byte[] data = (byte[]) obj;
                    StringBuilder builder = new StringBuilder();
                    builder.append ("[");
                    for (int j = 0; j < data.length && j < MAX_BINARY_LENGTH; j++) {
                        int value = data[j] + 128;
                        if (builder.length() > 2) {
                            builder.append(",");
                        }
                        builder.append(value);
                    }
                    if (data.length >= MAX_BINARY_LENGTH) {
                        builder.append(", ...");
                    } else {
                        builder.append("]");
                    }

                    obj = builder.toString();
                }

                return new ReadOnlyObjectWrapper(obj);
            });
            table.getColumns().add(col);
        }

        ObservableList<ResultRow> rows = table.getItems();

        ResultRow row = null;
        while ((row = result.getResult().next()) != null) {
            rows.add(row);
        }

        String plus = result.getResult().getCount() >= Configuration.MAX_LOAD_RECORDS ? "+" : "";
        String detail = result.getStatement().getShowTiming() ? " (" + result.getResult().getCount() + plus + " in " + result.getDuration()+ "ms)" : "";

        addResult(result.getStatement().getName(), detail, table, result);

    }

    public void handleAddNewSheet() {
        Tab newTab = addSheet(createDefaultDocument());
        sheetTabs.getSelectionModel().select(newTab);
        appState.save();
        newTab.getContent().requestFocus();
    }

    public void handleAboutMenu() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("about.fxml"));
            Parent root = loader.load();

            final Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            Scene dialogScene = new Scene(root);

            Label label = (Label) dialogScene.lookup("#versionLabel");
            label.setText("v" + Configuration.VERSION);

            Hyperlink siteUrl = (Hyperlink) dialogScene.lookup("#siteUrl");
            siteUrl.setOnAction(e -> {
                HostServicesFactory.getInstance(app).showDocument("http://sqlboa.com");
            });

            dialog.setScene(dialogScene);
            dialog.sizeToScene();

            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleExitMenu() {
        Platform.exit();
    }

    public void handleAddNewConnection() {
        // Prompt for the db file(s)
        List<File> list = fileChooser.showOpenMultipleDialog(stage);
        if (list != null) {
            for (File file : list) {
                String name = file.getName().trim();
                int idx = name.lastIndexOf(".");
                if (idx > 0) {
                    // Trim the file extension
                    name = name.substring(0, idx);
                }

                Database db = new Database(name, new SQLiteConnection(Paths.get(file.toURI())));
                boa.openDB(db);

                addConnection(db);
            }
        }

    }

    private Database getActiveDatabase() {
        TreeItem item = connectionTree.getSelectionModel().getSelectedItem();
        if (item == null) {
            return null;
        }

        Object details = item.getValue();
        if (details instanceof ConnectionTreeNode) {
            System.out.println("Acti: details)");
            return ((ConnectionTreeNode) details).db;
        }

        if (details instanceof TypeTreeNode) {
            System.out.println("Type: " + details);
            return ((TypeTreeNode) details).db;
        }

        // TODO: Default to first?
        return appState.getDatabaseList().get(0);
    }

    public void executeDocument(boolean asScript) {

        appState.save();

        // Default connection
        Database db = getActiveDatabase();
        if (db == null) {
            Dialog.showError("Alert", "No active connection");
            return;
        }

        // Input
        DocumentTab currentTab = (DocumentTab) sheetTabs.getSelectionModel().getSelectedItem();
        if (currentTab == null) {
            // Not sure how, but whatever
            return;
        }

        TextArea textArea = (TextArea) currentTab.getContent();

        // Execute
        BoaStatement statement = null;
        if (asScript) {
            String sql = currentTab.document.getBody().trim();
            String name = null;
            int endOfFirstLine = sql.indexOf("\n");
            if (endOfFirstLine > 0) {
                String firstLine = sql.substring(0, endOfFirstLine).trim();

                if (firstLine.endsWith(":")) {
                    name = firstLine.substring(0, firstLine.length()-1).trim();
                    sql = sql.substring(endOfFirstLine).trim();
                }
            }

            statement = new BoaStatement(name, sql);
        } else {
            statement = currentTab.document.getStatementAt(textArea.getCaretPosition());
            if (statement == null || !statement.isValid()) {
                return;
            }
        }

        db = selectDatabase(statement, db);
        if (db == null) {
            return;
        }

        boa.executeStatement(db, currentTab.document, statement, this);
    }

    private Database selectDatabase(BoaStatement statement, Database defaultDb) {
        if (statement == null || statement.getUsingDb() == null) {
            return defaultDb;
        }

        for (Database db : appState.getDatabaseList()) {
            if (db.getName().equalsIgnoreCase(statement.getUsingDb())) {
                return db;
            }
        }

        Dialog.showWarning("Error", "Database '" + statement.getUsingDb() + "' not found", stage.getScene().getWindow() );

        return null;
    }

    public BoaDocument createDefaultDocument() {
        return new BoaDocument("Sheet");
    }

    private TreeItem constructDBBranch(Database database) {

        Exception error = null;
        try {
            database.refresh();
        } catch (SQLException e) {
            error = e;
        }

        ConnectionTreeNode details = new ConnectionTreeNode(database, error == null);
        TreeItem connectionNode = new TreeItem(details);

//        boolean isDefaultDatabase = appState.getDefaultDatabase().equals(database.getName());
//        TreeBranch branch = new TreeBranch((isDefaultDatabase ? "* " : "" ) + database.getName());

        if (details.isOK) {
            connectionNode.getChildren().add(constructItemBranch(database, TreeItemType.TABLE, database.getTableList()));
            connectionNode.getChildren().add(constructItemBranch(database, TreeItemType.TRIGGER, database.getTriggerList()));
            connectionNode.getChildren().add(constructItemBranch(database, TreeItemType.INDEX, database.getIndexList()));
        }

        return connectionNode;
    }

    private TreeItem constructItemBranch(Database db, TreeItemType type, List<String> itemList) {

        TypeTreeNode details = new TypeTreeNode(db, type);

        TreeItem itemNode = new TreeItem(details) {
            @Override
            public boolean isLeaf() {
                return false;
            }
        };

        for (String item : itemList) {

            ItemTreeNode itemDetails = new ItemTreeNode(db, item, type);

            TreeItem node = new TreeItem(itemDetails);

            itemNode.getChildren().add(node);
        }

        return itemNode;
    }

    private static class DocumentTab extends Tab {
        private BoaDocument document;

        public DocumentTab(String label, BoaDocument document) {
            super(label);
            this.document = document;
        }
    }

    private static class DocumentChangeListener implements ChangeListener<String> {
        BoaDocument document;

        public DocumentChangeListener(BoaDocument document) {
            this.document = document;
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            document.setBody(newValue);
        }
    };

    private static class ResultTab extends Tab {
        private String name;
        private Object data;

        public ResultTab(String name, Object data) {
            this.name = name;
            this.data = data;
        }
    }
}
