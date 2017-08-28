package TinySql.TinySql;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.*;


public class CreateSubRoutine {
	private CreateTable createStmt;
	 List<ColumnDefinition> fields;
	 String tableName;
	private StorageManager manager;
	public CreateSubRoutine(CreateTable createStmt, StorageManager manager){
		this.createStmt = createStmt;	
		this.fields = null;
		this.tableName = null;
		this.manager = manager;
		
	}
	private void getTableName(){
		this.tableName =  createStmt.getTable().getName();
	}
	private void getFields(){
			List<ColumnDefinition> list = new ArrayList<ColumnDefinition>();
			this.fields =  createStmt.getColumnDefinitions();
	}
	//parse the statement and set up the fields list and tableName
	void execute(){
		getTableName();
		getFields();
		manager.createTable(fields, tableName);
	}

}
