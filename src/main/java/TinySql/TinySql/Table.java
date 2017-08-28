package TinySql.TinySql;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Table {
		
		List<TupleBuffer> tuples;
		String tableName;
		Set<String> schema;
		public Table(String tableName, Set<String> schema){
			this.tableName = tableName;
			this.schema = schema;
			tuples = new ArrayList<>();
			
		}
		public Table(Set<String> schema){
			this.schema = schema;	
			tuples = new ArrayList<>();
		}
		public void setTuple(List<TupleBuffer> tuples){
			this.tuples = tuples; 
		}
}
