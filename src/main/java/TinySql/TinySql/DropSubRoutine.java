package TinySql.TinySql;

import java.util.Iterator;
import java.util.List;


import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.drop.Drop;
import storageManager.SchemaManager;

public class DropSubRoutine {
	
	Drop statemnet;
	
	StorageManager manager;
	
	public DropSubRoutine(Drop statement, StorageManager manager){
		
		this.statemnet = statement;
		
		this.manager = manager;
	}
	
	//parse the statement and set up the fields list and tableName
	void execute(){
		
		String tableName = statemnet.getName();
				manager.dropTable(tableName);
		}
	
}
