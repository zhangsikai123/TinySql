package TinySql.TinySql;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TupleBuffer {
	
	HashMap<String, Value> fields;
	String tableName;
	Set<String> schema;
	public TupleBuffer(){
		fields = new HashMap<String, Value>();
	}
	public TupleBuffer(String tableName, Set<String> schema){
		fields = new HashMap<String, Value>();
		this.tableName = tableName;
	}
}
class Value{
	String str;
	Integer integer;
	public Value(String str){
		this.str = str;
	}
	public Value(int integer){
		this.integer = integer;
	}

}
