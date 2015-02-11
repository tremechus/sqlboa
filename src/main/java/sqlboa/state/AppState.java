package sqlboa.state;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import sqlboa.model.BoaDocument;
import sqlboa.model.Database;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AppState implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final String APP_PATH = System.getProperty("user.home") + "/.sqlboa2";
	private static final String INPUT_SAVE_FILE = "appstate.bin";
	
	private String defaultDatabase;
	private List<Database> databaseList = new ArrayList<>();
	private List<BoaDocument> documentList = new ArrayList<>();
    private Map<String, String> globalParamMap;

	public static AppState restore() {
		
		Path inputSavePath = Paths.get(APP_PATH, INPUT_SAVE_FILE);
		if (Files.exists(inputSavePath)) {
			
			Hessian2Input hessin = null;
			try {
				hessin = new Hessian2Input(new FileInputStream(inputSavePath.toFile()));
				
				return (AppState) hessin.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (hessin != null) {
					try {
						hessin.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return new AppState();
	}

    public String getParam(String key) {
        checkParamMap();
        return globalParamMap.get(key);
    }

    public void putParam(String key, String value) {
        checkParamMap();
        globalParamMap.put(key, value);
    }

    private void checkParamMap() {
        if (globalParamMap == null) {
            globalParamMap = new HashMap<>();
        }
    }

	public void save() {

		Path inputSavePath = Paths.get(APP_PATH, INPUT_SAVE_FILE);

		Hessian2Output hessout = null;
		try {
			inputSavePath.getParent().toFile().mkdirs();

			hessout = new Hessian2Output(new FileOutputStream(inputSavePath.toFile()));
			hessout.writeObject(this);
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (hessout != null) {
				try {
					hessout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public List<Database> getDatabaseList() {
		return databaseList;
	}

	public void removeDatabase(String name) {
		// Make a copy to avoid concurrent mods
		databaseList = new ArrayList<>(databaseList);
		for (ListIterator<Database> iter = databaseList.listIterator(); iter.hasNext();) {
			Database db = iter.next();
			if (name.equals(db.getName())) {
				iter.remove();
				break;
			}
		}
		
		save();
	}
	
	public List<BoaDocument> getDocumentList() {
		if (documentList == null) {
			documentList = new ArrayList<>();
		}
		return documentList;
	}
	
	public Database getDatabase(String name) {
		for (Database db : getDatabaseList()) {
			if (db.getName().equals(name)) {
				return db;
			}
		}
		return null;
	}
	
	public String getDefaultDatabase() {
		return defaultDatabase;
	}

	public void setDefaultDatabase(String defaultDatabase) {
		this.defaultDatabase = defaultDatabase;

		save();
	}
	
	
}
