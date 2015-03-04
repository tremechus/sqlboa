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

    		int totalCount = result.getTotalCount();

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


}