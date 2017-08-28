package TinySql.TinySql;

import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;

public class DeleteSubRoutine {
		
		Delete statemnet;
		
		StorageManager manager;
		
		public DeleteSubRoutine(Delete statement, StorageManager manager){
			
			this.statemnet = statement;
			
			this.manager = manager;
		}
		
		//parse the statement and set up the fields list and tableName
		void execute(){
			String tableName = statemnet.getTable().getName();
					manager.deleteTable(tableName);
			}
		
	
}
