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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import name.antonsmirnov.javafx.dialog.Dialog;
import sqlboa.db.RemoteConnection;
import sqlboa.db.SQLiteConnection;
import sqlboa.db.VolatileSQLiteConnection;
import sqlboa.model.*;
import sqlboa.modelview.ConnectionTreeNode;
import sqlboa.modelview.ItemTreeNode;
import sqlboa.modelview.TypeTreeNode;
import sqlboa.modelview.TreeItemType;
import sqlboa.parser.StatementCompletionListener;
import sqlboa.state.AppState;
import sqlboa.util.StringUtil;
import sqlboa.view.PopupDialog;
import sqlboa.view.SqlTextArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MainController implements StatementCompletionListener {

    private static final int MAX_STRING_LENGTH = 250;
    private static final int MAX_BINARY_LENGTH = 50;

    @FXML
    private TreeView<Object> connectionTree;

    @FXML
    private TabPane sheetTabs;

    @FXML
    private SplitPane resultsContainer;

    private SQLBoa boa;
    private AppState appState;
    private Stage stage;
    private FileChooser fileChooser = new FileChooser();
    private Application app;
    private List<ResultPanel> resultPanelList = new LinkedList<ResultPanel>();

    public void init(Application app, SQLBoa boa, AppState appState, Stage stage) {
        this.stage = stage;
        this.boa = boa;
        this.app = app;
        this.appState = appState;

        initResultTabs();
        initConnectionTree();
        initSheetTabs();
        initMenus();

        // Initial focus
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sheetTabs.getTabs().get(0).getContent().requestFocus();
            }
        });
    }

    private void initMenus() {

    }

    private void initResultTabs() {
        resultsContainer.getItems().clear();

        addDefaultResultPanel();
    }

    private void addDefaultResultPanel() {
        addResult("Results", null, null, null);
    }

    private void initConnectionTree() {
        List<Database> dbList = appState.getDatabaseList();

        TreeItem root = new TreeItem();
        root.setExpanded(true);
        connectionTree.setRoot(root);
        connectionTree.setShowRoot(false);
        connectionTree.setCellFactory(treeView -> {
            final Label label = new Label();
            final Label anotherLabel = new Label("Item:");
            label.getStyleClass().add("highlight-on-hover");
            final HBox hbox = new HBox(5, anotherLabel, label);
            TreeCell cell =  new TreeCell() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.toString());
                        setStyle("-fx-text-fill: #000000;");
                        if (item instanceof ConnectionTreeNode) {
                            if (!((ConnectionTreeNode)item).db.isOK()) {
                                setStyle("-fx-text-fill: #ff0000;");
                            }
                        }
                    }
                }
            };
            cell.itemProperty().addListener((obs, oldItem, newItem) -> {
                label.setText(newItem != null ? String.valueOf(newItem) : "");
            });
            return cell ;
        });

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

        // Scratch connection
        TreeItem branch = constructDBBranch(new Database("Scratch", new VolatileSQLiteConnection()));
        root.getChildren().add(branch);

        // User connections
        TreeItem selectedBranch = null;
        for (Database db : dbList) {
            branch = constructDBBranch(db);
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

        root.getChildren().add(constructDBBranch(db));

    }

    private void initSheetTabs() {
        // Grab the + tab
        Tab addTab = sheetTabs.getTabs().get(sheetTabs.getTabs().size()-1);
        sheetTabs.getTabs().clear();

        if (appState.getDocumentList().size() > 0) {
            for (BoaDocument document : appState.getDocumentList()) {
                addSheet(document);
            }
        } else {
            addSheet(createDefaultDocument());
        }

        addTab.setOnSelectionChanged(new EventHandler<Event>() {
            boolean updating = false;

            @Override
            public void handle(Event event) {
                if (updating) {
                    return;
                }

                updating = true;
                handleAddNewSheet();
                updating = false;

            }
        });

        sheetTabs.getTabs().add(addTab);
    }

    public Tab addSheet(final BoaDocument doc) {

        SqlTextArea text = new SqlTextArea(doc.getBody());
        text.textProperty().addListener(new DocumentChangeListener(doc));

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

    private void explainCurrentContext() {
        Database db = getActiveDatabase();
        if (db == null) {
            Dialog.showError("Alert", "No active connection");
            return;
        }

        // Current word
        DocumentTab currentTab = (DocumentTab) sheetTabs.getSelectionModel().getSelectedItem();
        if (currentTab == null) {
            // Not sure how, but whatever
            return;
        }

        TextArea textArea = (TextArea) currentTab.getContent();
        String word = StringUtil.findCurrentWord(textArea.getText(), textArea.getCaretPosition());
        if (word == null) {
            return;
        }

        // Table in current db?
        for (String tableName : db.getTableList()) {
            if (word.equalsIgnoreCase(tableName)) {
                boa.executeStatement(db, null, new BoaStatement("Table: " + tableName, "PRAGMA table_info(" + tableName +")", false), this);
                return;
            }
        }

        // Reserved word?
        switch (word.toLowerCase()) {
            case "select":
            case "from":
            case "where":
            default: // Until there is more intelligence here, just assume explain plan on the query
                BoaStatement statement = currentTab.document.getStatementAt(textArea.getCaretPosition());
                if (statement == null || !statement.isValid()) {
                    return;
                }

                statement.setShowQueryPlan();

                boa.executeStatement(db, null, statement, this);
                return;
        }

    }

    public void addResult(String name, String detail, Node content, Object data) {
        if (name == null) {
            name = "Results";
        }

        if (detail == null) {
            detail = "";
        }

        if (content == null) {
            Label emptyPanel = new Label();
            emptyPanel.setFocusTraversable(false);

            content = emptyPanel;
        }

        ResultPanel resultPanel = null;
        for (ResultPanel panel : resultPanelList) {
            if (name.equalsIgnoreCase(panel.name)) {
                resultPanel = panel;
            }
        }

        if (resultPanel == null) {
            resultPanel = new ResultPanel(name);
            resultsContainer.getItems().add(resultPanel.panel);

            resultPanelList.add(resultPanel);
        }

        resultPanel.controller.update(name, detail, content, data);

        // Divider all result panels evenly
        double[] dividerPositions = new double[resultPanelList.size()];
        double ratio = 1.0 / Math.max(resultPanelList.size(), 1);
        for (int i = 0; i < dividerPositions.length; i++) {
            dividerPositions[i] = (1 + i) * ratio;
        }

        resultsContainer.setDividerPositions(dividerPositions);
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

        String detail = result.getStatement().getShowTiming() ? " (" + NumberFormat.getNumberInstance().format(result.getResult().getTotalCount()) + " in " + NumberFormat.getNumberInstance().format(result.getDuration())+ "ms)" : "";

        addResult(result.getStatement().getName(), detail, table, result);

    }

    public void handleAddNewSheet() {
        Tab addTab = sheetTabs.getTabs().remove(sheetTabs.getTabs().size()-1);

        Tab newTab = addSheet(createDefaultDocument());
        sheetTabs.getSelectionModel().select(newTab);
        appState.save();
        newTab.getContent().requestFocus();

        sheetTabs.getTabs().add(addTab);
    }

    public void handleAboutMenu() {

        new PopupDialog("About", stage, "dialog_about") {
            @Override
            protected void configure(Scene scene) {
                Label label = (Label) scene.lookup("#versionLabel");
                label.setText("v" + Configuration.VERSION);

                Hyperlink siteUrl = (Hyperlink) scene.lookup("#siteUrl");
                siteUrl.setOnAction(e -> {
                    HostServicesFactory.getInstance(app).showDocument("http://sqlboa.com");
                });
            }
        }.show();
    }

    public void handleNewSheetMenu() {
        addSheet(createDefaultDocument());
    }

    public void handleExecuteStatementMenu() {
        executeDocument(false);
    }

    public void handleExecuteSheetMenu() {
        executeDocument(true);
    }

    public void handleExplainMenu() {
        explainCurrentContext();
    }

    public void handleExitMenu() {
        Platform.exit();
    }

    public void handleAddNewConnection() {

        new PopupDialog("Add Connection", stage, "dialog_newconnection") {
            @Override
            protected void configure(Scene scene) {
                scene.lookup("#localConnectionButton").setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        promptForLocalConnection();
                        close();
                    }
                });

                scene.lookup("#remoteConnectionButton").setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        promptForRemoteConnection();
                        close();
                    }
                });
            }
        }.show();
    }

    private void promptForRemoteConnection() {

        new PopupDialog("Add Remote Connection", stage, "dialog_remoteconnection") {
            @Override
            protected void configure(Scene scene) {
                TextField hostField = (TextField)scene.lookup("#hostAddressField");

                scene.lookup("#connectButton").setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        String host = hostField.getText();
                        if (host == null || host.trim().length() == 0) {
                            return;
                        }

                        Database db = new Database(host, new RemoteConnection(host));

                        boa.openDB(db);

                        addConnection(db);

                        close();
                    }
                });
            }
        }.show();
    }

    private void promptForLocalConnection() {
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
            return ((ConnectionTreeNode) details).db;
        }

        if (details instanceof TypeTreeNode) {
            return ((TypeTreeNode) details).db;
        }

        if (details instanceof ItemTreeNode) {
            return ((ItemTreeNode)details).db;
        }

        return null;
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

        SqlTextArea textArea = (SqlTextArea) currentTab.getContent();

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

        Dialog.showWarning("Error", "Database '" + statement.getUsingDb() + "' not found", stage.getScene().getWindow());

        return null;
    }

    public BoaDocument createDefaultDocument() {
        return new BoaDocument("Sheet");
    }

    private TreeItem constructDBBranch(Database database) {

        ConnectionTreeNode details = new ConnectionTreeNode(database);
        TreeItem connectionNode = new TreeItem(details);

        connectionNode.expandedProperty().addListener(e -> {
            if (connectionNode.isExpanded()) {
                if (database.isOK()) {
                    connectionNode.getChildren().clear();
                    connectionNode.getChildren().add(constructItemBranch(database, TreeItemType.TABLE));
                    connectionNode.getChildren().add(constructItemBranch(database, TreeItemType.TRIGGER));
                    connectionNode.getChildren().add(constructItemBranch(database, TreeItemType.INDEX));
                }
            }
        });

        // Put in a placeholder so that the node will be expandable.  It will be cleared on first expand
        connectionNode.getChildren().add(new TreeItem());

        return connectionNode;
    }

    private TreeItem constructItemBranch(Database db, TreeItemType type) {

        TypeTreeNode details = new TypeTreeNode(db, type);

        TreeItem itemNode = new TreeItem(details) {
            @Override
            public boolean isLeaf() {
                return false;
            }
        };

        itemNode.expandedProperty().addListener(e -> {

            List<String> itemList = null;
            switch (type) {
                case TABLE:
                    itemList = db.getTableList();
                    break;
                case TRIGGER:
                    itemList = db.getTriggerList();
                    break;
                case INDEX:
                    itemList = db.getIndexList();
                    break;
            };

            if (itemNode.isExpanded()) {
                itemNode.getChildren().clear();
                for (String item : itemList) {
                    ItemTreeNode itemDetails = new ItemTreeNode(db, item, type);

                    TreeItem node = new TreeItem(itemDetails);

                    itemNode.getChildren().add(node);
                }
            }
        });

        // Put in a dummy node so that the node will be expandable, it will be removed on first viewing
        itemNode.getChildren().add(new TreeItem());

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

    private class ResultPanel implements ResultPanelController.OnCloseListener {
        private String name;
        private Node panel;
        private ResultPanelController controller;

        public ResultPanel(String name) {
            this.name = name;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("resultpane.fxml"));

            try {
                panel = loader.load();
                controller = loader.getController();

                controller.setOnCloseListener(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClose() {
            resultPanelList.remove(this);

            resultsContainer.getItems().removeAll(panel);

            if (resultPanelList.size() == 0) {
                addDefaultResultPanel();
            }
        }
    }
}
