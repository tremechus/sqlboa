package sqlboa;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import name.antonsmirnov.javafx.dialog.Dialog;
import sqlboa.model.*;
import sqlboa.parser.StatementCompletionListener;
import sqlboa.state.AppState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SQLBoa {
	
    private AppState appState;
	private Stage primaryStage;

    public SQLBoa(AppState appState, Stage primaryStage) {
        this.appState = appState;
		this.primaryStage = primaryStage;
    }
    
    public void openDB(Database database) {
    	appState.getDatabaseList().add(database);
    	
		appState.setDefaultDatabase(database.getName());

    	appState.save();
    }

    public void removeDB(Database database) {
        appState.removeDatabase(database.getName());
    }

    public void executeStatement(Database db, BoaDocument document, BoaStatement statement, StatementCompletionListener... callbacks) {
        appState.save();

    	List<SqlParam> paramList = statement.getParamList();
    	if (paramList.size() > 0) {
    		promptForParams(db, document, statement, callbacks);
    	} else {
    		executeStatementInternal(db, statement, callbacks);
    	}
    }

    private void promptForParams(final Database db, BoaDocument document, final BoaStatement statement, final StatementCompletionListener... callbacks) {

		final Stage dialog = new Stage();
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(primaryStage);
		GridPane panel = new GridPane();
        panel.setHgap(10);
        panel.setVgap(10);
        panel.setPadding(new Insets(10, 10, 10, 10));

        final java.util.Map<Object, TextField> inputMap = new HashMap<>();
        List<String> seenItemList = new ArrayList<String>();
        final List<SqlParam> paramList = statement.getParamList();
        int row = 0;
    	for (int i = 0; i < paramList.size(); i++) {
    		SqlParam param = paramList.get(i);

    		// Don't show a row for named parameters that we've already displayed
    		if (!param.getKey().equals("?") && seenItemList.contains(param.getKey())) {
    			continue;
    		}
    		seenItemList.add(param.getKey());

            panel.add(new Label(param.getKey()), 0, row);
    		TextField input = new TextField();
            input.setUserData(param);

    		String value = "";
            switch (param.getType()) {
                case GLOBAL:
                    value = appState.getParam(param.getKey());
                    break;
                case INDEXED:
                case NAMED:
                case SUBSTITUTION:
                    value = document != null ? document.getParamMap().get(param.getKey()) : null;
                    break;
            }
    		if (value == null) {
    			value = "";
    		}

			input.setText(value);

    		panel.add(input, 1, row);

    		inputMap.put(param.getKey().equals("?") ? i : param.getKey(), input);

            row++;
    	}

    	if (inputMap.size() == 0) {
    		executeStatementInternal(db, statement, callbacks);
    		return;
    	}

        Button okButton = new Button("OK");
        okButton.setDefaultButton(true);
        okButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                okButton.fire();
            }
        });
        okButton.setOnAction(e -> {
            boolean globalParamChange = false;
            for (java.util.Map.Entry<Object, TextField> entry : inputMap.entrySet()) {
                String value = entry.getValue().getText();

                // TODO: This doesn't look right - TSC
                if (entry.getKey() instanceof Integer) {
                    paramList.get((Integer)entry.getKey()).setValue(value);
                }

                if (entry.getKey() instanceof String) {
                    for (SqlParam param : paramList) {
                        if (param.getKey().equals(entry.getKey())) {
                            param.setValue(value);

                            // Save value for later
                            switch (param.getType()) {
                                case GLOBAL:
                                    appState.putParam(param.getKey(), value);
                                    globalParamChange = true;
                                    break;
                                case INDEXED:
                                case NAMED:
                                case SUBSTITUTION:
                                    if (document != null) {
                                        document.getParamMap().put(entry.getKey().toString(), value);
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
            if (globalParamChange) {
                appState.save();
            }

            dialog.close();

            executeStatementInternal(db, statement, callbacks);
        });

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                cancelButton.fire();
            }
        });
        cancelButton.setOnAction(e -> {
            dialog.close();
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(okButton);
        buttonBox.getChildren().add(cancelButton);

        panel.add(buttonBox, 0, row + 1, 2, 1);

        Scene dialogScene = new Scene(panel);
        dialog.setScene(dialogScene);
        dialog.sizeToScene();

        dialog.show();
    }

	private StatementResult execute(Database db, BoaStatement statement) throws SQLException {

		if (db == null) {
			return null;
		}

		StatementResult result = db.exec(statement.getSQL(), statement.getParamList());

		return result;
	}

	private void executeStatementInternal(Database db, BoaStatement statement, StatementCompletionListener... callbacks) {

    	try {
    		long start = System.currentTimeMillis();
    		StatementResult result = execute(db, statement);
    		long elapsed = System.currentTimeMillis() - start;

    		if (result == null) {
    			// Nothing happened, assume success
    			fireStatementCompletion(new BoaResult(statement, null, elapsed), callbacks);
    			return;
    		}

    		int totalCount = result.getCount();

    		// Create new result table

    		// Update tabs
//        	int idx = -1;
//        	TabSequence tabs = resultPane.getTabs();
//        	for (idx = 0; idx < tabs.getLength(); idx++) {
//        		Component c = tabs.get(idx);
//        		Object data = TabPane.getTabData(c);
//
//        		if (!(data instanceof ResultTab) || ((ResultTab)data).getName().equals(statement.getName())) {
//        			tabs.remove(c);
//        			break;
//        		}
//        	}
//
//    		resultPane.getTabs().insert(scrollPane, idx);
//
//    		// TODO: get actual count
//    		TabPane.setTabData(scrollPane, new ResultTab(statement.getName(), elapsed, totalCount));
//
//    		resultPane.setSelectedIndex(idx);

    		fireStatementCompletion(new BoaResult(statement, result, elapsed), callbacks);
    	} catch (SQLException e) {
    		Dialog.showError("Error", e.getMessage());
    		fireStatementCompletion(null, callbacks);
    	} finally {
            // FIXME:
//    		try {
//    			getActiveDatabase().refresh();
//    		} catch (SQLException e) {
//    			e.printStackTrace();
//    		}
//    		updateUI();
    	}
    }

	private void fireStatementCompletion(BoaResult result, StatementCompletionListener... callbacks) {
		if (callbacks != null) {
			for (StatementCompletionListener callback : callbacks) {
				callback.statementCompleted(result);
			}
		}
	}

//    private TableView createTable(ResultView result) {
//    	TableView table = new TableView();
//
//		for (int i = 0; i < result.getColumnCount(); i++) {
//			Column column = new Column(result.getColumnName(i), result.getColumnName(i), result.getColumnCount() > 1 ? 80 : -1);
//			column.setCellRenderer(new ResultsCellRenderer());
//			table.getColumns().add(column);
//		}
//
//		org.apache.pivot.collections.List<java.util.Map<String, Object>> rows = new org.apache.pivot.collections.ArrayList<>();
//
//		// Rows
//		ResultRow row = null;
//		while ((row = result.next()) != null) {
//			java.util.Map<String, Object> rowData = new HashMap<>();
//
//			for (int i = 0; i < result.getColumnCount(); i++) {
//				Object obj = row.get(i);
//
//				ColumnRenderer renderer = columnRendererMap.get(obj != null ? obj.getClass() : null);
//				if (renderer == null) {
//					renderer = columnRendererMap.get(null);
//				}
//				if (renderer == null) {
//					renderer = new DefaultRenderer();
//				}
//
//				String value = "";
//				try {
//					value = renderer.render(obj);
//
//					if (result.getColumnCount() > 1 && value.length() > 50) { // TODO: Parameterize
//						value = value.substring(0, 47) + "...";
//					}
//				} catch (Throwable t) {
//					value = "Err";
//				}
//
//				rowData.put(result.getColumnName(i), value);
//			}
//
//			rows.add(rowData);
//		}
//
//		table.setTableData(rows);
//
//		return table;
//    }
//
//    public void refresh() {
//
//    	for (Database db : appState.getDatabaseList()) {
//    		try {
//    			db.refresh();
//    		} catch (SQLException e) {
//    			e.printStackTrace();
//    		}
//    	}
//
//    }
//
//    private static class ResultTab extends ButtonData {
//
//    	private String name;
//
//    	public ResultTab(String name, long duration, int count) {
//    		super(name + " (" + (count >= Configuration.MAX_LOAD_RECORDS ? Configuration.MAX_LOAD_RECORDS + "+" : count) + " in " + duration + " ms)");
//    		this.name = name;
//    	}
//
//    	public String getName() {
//    		return name;
//    	}
//    }
//
//    private class TreeKeyListener implements ComponentKeyListener {
//    	@Override
//    	public boolean keyPressed(Component component, int keyCode, KeyLocation keyLocation) {
//    		return false;
//    	}
//    	@Override
//    	public boolean keyReleased(Component component, int keyCode, KeyLocation keyLocation) {
//    		return false;
//    	}
//    	@Override
//    	public boolean keyTyped(Component component, char character) {
//
//    		if (character == KeyCode.DELETE) {
//
//    			TreeNode node = (TreeNode) treeView.getSelectedNode();
//
//    			final NodeDetail details = (NodeDetail) node.getUserData();
//    			if (details == null) {
//    				return true;
//    			}
//
//    			switch (details.type) {
//    			case DATABASE:
//
//    				final Prompt confirm = new Prompt(MessageType.WARNING, "Delete connection " + details.name + "?", new org.apache.pivot.collections.ArrayList<>("OK", "Cancel"));
//    				confirm.open(getMainWindow(), new SheetCloseListener() {
//
//						@Override
//						public void sheetClosed(Sheet sheet) {
//							if ("OK".equals(confirm.getSelectedOption())) {
//
//								appState.removeDatabase(details.name);
//
//								updateUI();
//							}
//						}
//					});
//
//    				break;
//    			case TRIGGER:
//
//    				final Prompt confirmTrigger = new Prompt(MessageType.WARNING, "Delete trigger " + details.name + "?", new org.apache.pivot.collections.ArrayList<>("OK", "Cancel"));
//    				confirmTrigger.open(getMainWindow(), new SheetCloseListener() {
//
//						@Override
//						public void sheetClosed(Sheet sheet) {
//							if ("OK".equals(confirmTrigger.getSelectedOption())) {
//
//								try {
//									details.db.dropTrigger(details.name);
//									details.db.refresh();
//								} catch (SQLException e) {
//									Alert.alert(MessageType.ERROR, "Could not delete trigger: " + e, window);
//								}
//
//								updateUI();
//							}
//						}
//					});
//
//    				break;
//
//    			case INDEX:
//
//    				final Prompt confirmIndex= new Prompt(MessageType.WARNING, "Delete index " + details.name + "?", new org.apache.pivot.collections.ArrayList<>("OK", "Cancel"));
//    				confirmIndex.open(getMainWindow(), new SheetCloseListener() {
//
//						@Override
//						public void sheetClosed(Sheet sheet) {
//							if ("OK".equals(confirmIndex.getSelectedOption())) {
//
//								try {
//									details.db.dropIndex(details.name);
//									details.db.refresh();
//								} catch (SQLException e) {
//									Alert.alert(MessageType.ERROR, "Could not delete trigger: " + e, window);
//								}
//
//								updateUI();
//							}
//						}
//					});
//
//    				break;
//    			case TABLE:
//    				final Prompt confirmTable = new Prompt(MessageType.WARNING, "Delete connection " + details.name + "?", new org.apache.pivot.collections.ArrayList<>("OK", "Cancel"));
//    				confirmTable.open(getMainWindow(), new SheetCloseListener() {
//
//						@Override
//						public void sheetClosed(Sheet sheet) {
//							if ("OK".equals(confirmTable.getSelectedOption())) {
//
//								try {
//									details.db.dropTable(details.name);
//									details.db.refresh();
//								} catch (SQLException e) {
//									Alert.alert(MessageType.ERROR, "Could not delete trigger: " + e, window);
//								}
//
//								updateUI();
//							}
//						}
//					});
//				default:
//    			}
//
//    			return true;
//    		}
//
//    		return false;
//    	}
//    }
//
//    private class TreeClickListener implements ComponentMouseButtonListener {
//    	private TreeNode lastClickedNode;
//    	@Override
//    	public boolean mouseClick(Component component, Button button, int x, int y, int count) {
//
//			TreeNode node = (TreeNode) treeView.getSelectedNode();
//    		if (button == Button.LEFT && count == 2) {
//    			if (node == null) {
//    				return false;
//    			}
//
//    			if (node != lastClickedNode) {
//    				lastClickedNode = node;
//    				return false;
//    			}
//
//    			NodeDetail details = (NodeDetail) node.getUserData();
//    			if (details == null) {
//    				return true;
//    			}
//
//    			BoaStatement statement = null;
//    			switch (details.type) {
//    			case TABLE:
//    				statement = new BoaStatement("Table: " + node.getText(), "PRAGMA table_info(" + node.getText() +")");
//    				executeStatement(statement);
//    				break;
//
//    			case DATABASE:
//
//    				appState.setDefaultDatabase(details.name);
//
//    				updateUI();
//
//    				break;
//
//    			case INDEX:
//    				statement = new BoaStatement("Index: " + node.getText(), "select sql from sqlite_master where type = 'index' and name = '" + node.getText() +"'");
//    				executeStatement(statement);
//
//    				break;
//
//    			case TRIGGER:
//    				statement = new BoaStatement("Trigger: " + node.getText(), "select sql from sqlite_master where type = 'trigger' and name = '" + node.getText() +"'");
//    				executeStatement(statement);
//
//    				break;
//    			default:
//    			}
//    		}
//    		lastClickedNode = node;
//
//    		return false;
//    	}
//
//    	@Override
//    	public boolean mouseDown(Component component, Button button, int x, int y) {
//    		return false;
//    	}
//    	@Override
//    	public boolean mouseUp(Component component, Button button, int x, int y) {
//    		return false;
//    	}
//    }
//
//    private class NodeDetail {
//    	TreeItemType type;
//    	String name;
//    	Database db;
//    	boolean isOK = true;
//    	String tip;
//
//    	@Override
//    	public String toString() {
//    		return "Node[" + type + ":" + name + "]";
//    	}
//    }
//
//    private class ResultsCellRenderer extends TableViewCellRenderer {
//    	@Override
//    	public String toString(Object row, String columnName) {
//    		Object value = ((java.util.Map<String, Object>)row).get(columnName);
//    		return value != null ? value.toString() : null;
//    	}
//    }
//
//    private class TreeMouseListener implements ComponentMouseListener {
//    	@Override
//    	public boolean mouseMove(Component component, int x, int y) {
//    		Path path = treeView.getNodeAt(y);
//
//    		treeView.setTooltipText(null);
//    		if (path != null && path.getLength() > 0) {
//	    		TreeNode root = (TreeNode) treeView.getTreeData();
//
//	    		for (int i = 0; i < path.getLength(); i++) {
//	    			root = ((TreeBranch)root).get(path.get(i));
//	    		}
//
//	    		NodeDetail details = (NodeDetail) root.getUserData();
//	    		if (details == null) {
//	    			return false;
//	    		}
//
//	    		treeView.setTooltipText(details.tip);
////	    		switch (details.type) {
////	    		case DATABASE:
////	    			Database db = appState.getDatabase(details.name);
////	    			if (db != null) {
////	    				treeView.setTooltipText(db.getConnectionName());
////	    			}
////	    			break;
////    			default:
////	    		}
//    		}
//
//    		return false;
//    	}
//    	@Override
//    	public void mouseOut(Component component) {
//    		component.setTooltipText(null);
//    	}
//    	@Override
//    	public void mouseOver(Component component) {
//    	}
//    }
//
//    private class InputAreaChangeListener implements TextAreaContentListener {
//    	private BoaDocument document;
//
//    	public InputAreaChangeListener(BoaDocument document) {
//    		this.document = document;
//    	}
//
//    	@Override
//    	public void textChanged(TextArea textArea) {
//    		document.setBody(textArea.getText());
//    	}
//    	@Override
//    	public void paragraphInserted(TextArea textArea, int index) {
//    	}
//    	@Override
//    	public void paragraphsRemoved(TextArea textArea, int index, Sequence<Paragraph> removed) {
//    	}
//
//    }
//
//    private class DBTreeViewRenderer extends TreeViewNodeRenderer {
//
//    	@Override
//    	public void render(Object node, Path path, int rowIndex, TreeView treeView, boolean expanded, boolean selected, NodeCheckState checkState, boolean highlighted, boolean disabled) {
//
//            Font font = (Font)treeView.getStyles().get("font");
//            Color color = Color.black;
//
//            if (node != null) {
//	            NodeDetail detail = (NodeDetail)((TreeNode)node).getUserData();
//
//	            if (detail != null) {
//		        	boolean isDefaultDatabase = (detail.type == TreeItemType.DATABASE && detail.name.equals(appState.getDefaultDatabase()));
//		            if (isDefaultDatabase) {
//		            	font = font.deriveFont(Font.BOLD);
//		            }
//
//		            if (!detail.isOK) {
//		            	color = Color.red;
//		            }
//	            }
//            }
//
//            if (node != null) {
//                Image icon = null;
//                String text = null;
//
//                if (node instanceof TreeNode) {
//                    TreeNode treeNode = (TreeNode)node;
//
//                    if (expanded
//                        && treeNode instanceof TreeBranch) {
//                        TreeBranch treeBranch = (TreeBranch)treeNode;
//                        icon = treeBranch.getExpandedIcon();
//
//                        if (icon == null) {
//                            icon = treeBranch.getIcon();
//                        }
//                    } else {
//                        icon = treeNode.getIcon();
//                    }
//                } else if (node instanceof Image) {
//                    icon = (Image)node;
//                }
//                text = toString(node);
//
//                // Update the image view
//                imageView.setImage(icon);
//                imageView.getStyles().put("opacity",
//                    (treeView.isEnabled() && !disabled) ? 1.0f : 0.5f);
//
//                // Update the label
//                label.setText(text != null ? text : "");
//
//                if (text == null) {
//                    label.setVisible(false);
//                } else {
//                    label.setVisible(true);
//
//                    label.getStyles().put("font", font);
//
//                    if (treeView.isEnabled() && !disabled) {
//                        if (selected) {
//                            if (treeView.isFocused()) {
//                                color = (Color)treeView.getStyles().get("selectionColor");
//                            } else {
//                                color = (Color)treeView.getStyles().get("inactiveSelectionColor");
//                            }
//                        }
//                    } else {
//                        color = (Color)treeView.getStyles().get("disabledColor");
//                    }
//
//                    label.getStyles().put("color", color);
//                }
//            }
//    	}
//    }
//
//    private class InputPaneListener implements TabPaneListener {
//    	@Override
//    	public void closeableChanged(TabPane tabPane) {
//    	}
//
//    	@Override
//    	public void collapsibleChanged(TabPane tabPane) {
//    	}
//    	@Override
//    	public void cornerChanged(TabPane tabPane, Component previousCorner) {
//    	}
//    	@Override
//    	public Vote previewRemoveTabs(TabPane tabPane, int index, int count) {
//    		return Vote.APPROVE;
//    	}
//    	@Override
//    	public void removeTabsVetoed(TabPane tabPane, Vote reason) {
//    	}
//    	@Override
//    	public void tabDataRendererChanged(TabPane tabPane, DataRenderer previousTabDataRenderer) {
//    	}
//    	@Override
//    	public void tabInserted(TabPane tabPane, int index) {
//    	}
//    	@Override
//    	public void tabsRemoved(TabPane tabPane, int index, Sequence<Component> tabs) {
//
//    		for (int i = 0; i < tabs.getLength(); i++) {
//    			Component panel = tabs.get(i);
//        		appState.getDocumentList().remove(panel.getUserData().get(USERDATA_DOCUMENT));
//    		}
//
//    		if (appState.getDocumentList().size() == 0) {
//    			addSheet(createDefaultDocument());
//    		}
//
//    		inputPane.setSelectedIndex(index < appState.getDocumentList().size() ? index : appState.getDocumentList().size()-1);
//    	}
//    }
}