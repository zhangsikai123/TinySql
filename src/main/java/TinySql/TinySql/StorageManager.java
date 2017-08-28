package TinySql.TinySql;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import storageManager.Block;
import storageManager.Disk;
import storageManager.FieldType;
import storageManager.MainMemory;
import storageManager.Relation;
import storageManager.Schema;
import storageManager.SchemaManager;
import storageManager.Tuple;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.*;
public class StorageManager{
	
	MainMemory memo;
	Disk dis;
	SchemaManager schema_manager;
	
	
	public StorageManager(){
		memo= new MainMemory();
		dis = new Disk();
		schema_manager=new SchemaManager(memo,dis);
	}
	
	 // An dummy procedure of appending a tuple to the end of a relation, it will be changed later on
	  // using memory block "memory_block_index" as output buffer
	  public static void appendTupleToRelation(Relation relation_reference, MainMemory mem, int memory_block_index, Tuple tuple) {
	    Block block_reference;
	    if (relation_reference.getNumOfBlocks()==0) {
//	      System.out.print("The relation is empty" + "\n");
//	      System.out.print("Get the handle to the memory block " + memory_block_index + " and clear it" + "\n");
	      block_reference=mem.getBlock(memory_block_index);
	      block_reference.clear(); //clear the block
	      block_reference.appendTuple(tuple); // append the tuple
//	      System.out.print("Write to the first block of the relation" + "\n");
	      relation_reference.setBlock(relation_reference.getNumOfBlocks(),memory_block_index);
	    } else {
//	      System.out.print("Read the last block of the relation into memory block 5:" + "\n");
	      relation_reference.getBlock(relation_reference.getNumOfBlocks()-1,memory_block_index);
	      block_reference=mem.getBlock(memory_block_index);
	     
	      if (block_reference.isFull()) {
//	        System.out.print("(The block is full: Clear the memory block and append the tuple)" + "\n");
	        block_reference.clear(); //clear the block
	        block_reference.appendTuple(tuple); // append the tuple
//	        System.out.print("Write to a new block at the end of the relation" + "\n");
	        relation_reference.setBlock(relation_reference.getNumOfBlocks(),memory_block_index); //write back to the relation
	      } else {
//	        System.out.print("(The block is not full: Append it directly)" + "\n");
	        block_reference.appendTuple(tuple); // append the tuple
//	        System.out.print("Write to the last block of the relation" + "\n");
	        relation_reference.setBlock(relation_reference.getNumOfBlocks()-1,memory_block_index); //write back to the relation
	      }
	    }
	  }
	
		//determine if a string is a Integer
		public static boolean isInteger(String s) {
		    try { 
		        Integer.parseInt(s); 
		    } catch(NumberFormatException e) { 
		        return false; 
		    } catch(NullPointerException e) {
		        return false;
		    }
		    // only got here if we didn't return false
		    return true;
		}	
	
	void createTable(List<ColumnDefinition> fields, String tableName){
	
		//init storageManager
		ArrayList<String> field_names=new ArrayList<String>();
	    ArrayList<FieldType> field_types=new ArrayList<FieldType>();
	    
	    //load fields' names and types
	    fields.forEach(column->{
	    	field_names.add(column.getColumnName());
	    	String type = column.getColDataType().getDataType();
	    	if(type.equals("INT")){
	    		field_types.add(FieldType.INT);
	    	}
	    	else if(type.equals("STR20")){
	    		field_types.add(FieldType.STR20);
	    	}
	    	else{
	    		System.out.print("Undefined type for"+ column.getColumnName());	
	    	}
	    		});
	    
	    Schema schema=new Schema(field_names,field_types);
	    
	    Relation relation_ref = schema_manager.createRelation(tableName, schema); 
	    
	}

	void insertTable(String tableName, HashMap<String, String> values){
		
		Relation relation_ref = schema_manager.getRelation(tableName);
		
		Tuple tuple = relation_ref.createTuple();
		
		//deal with null-pointer error
		
		if(relation_ref==null)System.out.print("No such table");
		
		else{
			
			values.forEach((k,v)->{
				
				if(isInteger(v))
					
				{
					
					tuple.setField(k, Integer.parseInt(v));
					
				}
				
				else{
					
					tuple.setField(k,v);
					
				}
					
			});
			
			 appendTupleToRelation(relation_ref, memo, 9, tuple);
			 
		}
		
	}
	//drop table
	void dropTable(String tableName){
		schema_manager.deleteRelation(tableName);
	}
	
	//delete table
	void deleteTable(String tableName){
		schema_manager.getRelation(tableName).deleteBlocks(0);
	}
}
