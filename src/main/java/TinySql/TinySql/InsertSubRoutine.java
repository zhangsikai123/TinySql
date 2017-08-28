package TinySql.TinySql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SubSelect;

public class InsertSubRoutine implements ItemsListVisitor {
	private Insert insertStmt;
	String tableName;
	HashMap<String, String> values;
	List<String> items;
	private StorageManager manager;
	public InsertSubRoutine(Insert insertStmt, StorageManager manager){
		this.insertStmt = insertStmt;	
		this.tableName = null;
		this.values = new HashMap<>();
		this.items = new ArrayList<>();
		this.manager = manager; 
	}
	
	
	//parse the statement and set up the fields list and tableName
	void execute(){
		//if have select in insert
		String insertString = insertStmt.toString().toLowerCase();
		if(insertString.contains("select"))
		{
			String statement = insertString.substring(insertString.indexOf("select"),insertString.length()-1);
			Statement state;
			try {
				
				//parse the select tree then
			state = CCJSqlParserUtil.parse(statement);
			SelectSubRoutine s = new SelectSubRoutine((Select)state, manager);
			Table t = s.getResult();
			String tableName = insertStmt.getTable().getName();
			t.tuples.forEach((tuple)->{
				tuple.fields.forEach((field, value)->{
					values.put(field, (value.str==null)?String.valueOf(value.integer):value.str);	
				});
				manager.insertTable(tableName, values);
			});
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//if not
		else{
		tableName = insertStmt.getTable().getName();
		//get column list
		List<Column> columns = insertStmt.getColumns();
		//get value list
		insertStmt.getItemsList().accept(this);
		//deal with mis match size of values and columns
		if(items.size()!=columns.size())System.out.println("the number of columns and values doesn't match");
		else{
			for(int i=0;i<columns.size();i++){
				values.put(columns.get(i).getColumnName(), items.get(i));
			}
		}
		manager.insertTable(tableName, values);
		}
	}
	


	@Override
	public void visit(SubSelect subSelect) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void visit(ExpressionList expressionList) {
		// TODO Auto-generated method stub
		expressionList.getExpressions().forEach(e ->{
			items.add(e.toString());
		});
		
	}


	@Override
	public void visit(MultiExpressionList multiExprList) {
		// TODO Auto-generated method stub
		
	}
}
