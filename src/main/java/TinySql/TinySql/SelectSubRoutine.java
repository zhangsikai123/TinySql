package TinySql.TinySql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.event.ListSelectionEvent;

import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import storageManager.Block;
import storageManager.Field;
import storageManager.Relation;
import storageManager.Tuple;

public class SelectSubRoutine {
	
	
	private Select select;
	private StorageManager manager;
	public SelectSubRoutine(Select select, StorageManager manager){
		this.manager = manager;
		this.select = select;
		
	}
	
	
	
	
	
	Table order(Table relation){
		String pattern =  " +order +by +(.*)";
		String selectBody = select.getSelectBody().toString().toLowerCase();
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(selectBody);
		if(m.find()){
		String orderBy = m.group(1);
		TupleBuffer[] tps = new TupleBuffer[relation.tuples.size()];
		relation.tuples.toArray(tps);
		Arrays.sort(tps, new Comparator<TupleBuffer>(){
			@Override
			public int compare(TupleBuffer o1, TupleBuffer o2) {
				Value v1 = o1.fields.get(orderBy);
				Value v2 = o2.fields.get(orderBy);
				if(v1.integer!=null){
					if(v1.integer>v2.integer)return 1;
					else return -1;
				}
				return v1.str.compareTo(v2.str);
			}	
		});
		relation.setTuple(Arrays.asList(tps));
		}
		return relation;
	}
	Table duplicate(Table relation){
		LinkedList<TupleBuffer> checked = new LinkedList<>();
		List<TupleBuffer> tuples = relation.tuples;
		
		if(select.getSelectBody().toString().toLowerCase().contains("distinct")){
			checked.add(tuples.get(0));
			int length = tuples.size();
			boolean flag = false;
			int i=1;
			while(i<length){
				TupleBuffer thisTp = tuples.get(i);
				int size = checked.size();
				int j=0;
				flag = false;
				while(j<size){
					if(isDuplicate(checked.get(j),thisTp)){flag = true;break;}
					j++;
				}
				if(!flag){checked.add(thisTp);}
				i++;
			}
			relation.setTuple(checked);
		};
		
		return relation;
	}
	
	boolean isDuplicate(TupleBuffer tp1, TupleBuffer tp2){
		HashMap<String, Value> fields1 = tp1.fields;
		HashMap<String, Value> fields2 = tp2.fields;
		Iterator<Entry<String, Value>> itr = fields1.entrySet().iterator();
		while(itr.hasNext()){
			Entry<String, Value> next = itr.next();
			if(!equalValue(fields2.get(next.getKey()),next.getValue()))return false;
		}
		return true;
	}
	boolean equalValue(Value v1, Value v2){
	if(v1.integer==null)return v1.str.equals(v2.str);
	else return v1.integer==v2.integer;
	}
	
	
	//project operation
	Table project(Table relation){
		SelectVisitorForNonWhere visitor= new SelectVisitorForNonWhere(select);
		visitor.startVisit();
		List<SelectItem> attrs = visitor.getAttrs();
		List<String> tables = visitor.getTableList();
		//if attrs is *
		if(attrs.size()==1&&attrs.get(0).toString().equals("*"))return relation;
		Set<String> schema = new HashSet<>();
		attrs.forEach(fieldName->{
			schema.add(fieldName.toString());
		});
		Table res = new Table(schema);
		
		relation.tuples.forEach(tuple->{
			TupleBuffer tmp = new TupleBuffer();
			TupleBuffer t = tuple;
			attrs.forEach(attr->{
				String att = attr.toString();
				ColumnName columnName = new ColumnName(att);
				if (tables.size()==1&&columnName.hasTableName())att = columnName.fieldName;
				if(!t.fields.containsKey(att.toString()))System.out.println("table does not have such attribute "+attr);
				tmp.fields.put(attr.toString(),t.fields.get(att)); 
			});
			res.tuples.add(tmp);
		});
		return res;
	}
	
	//cross join operation
	Table crossJoin(List<Table> relations){
			Iterator<Table> itr = relations.iterator();
			Table first = itr.next();
			//for each subRelation, do the cross join between them
			while(itr.hasNext()){
				Table second = itr.next();
				//do cross join
				first = crossJoinTwo(first, second);
			}
			
		return first;
	}
	
	Table crossJoinTwo(Table first,Table second){
		Set<String> schema = new HashSet<>();
		first.schema.forEach(fieldName->{
			String name = fieldName;
			if(first.tableName==null)schema.add(name);
			else schema.add(first.tableName+"."+name);
		});
		second.schema.forEach(fieldName->{
			String name = fieldName;
			if(second.tableName==null)schema.add(name);
			schema.add(second.tableName+"."+name);
		});
		Table res = new Table(schema);
		Iterator<TupleBuffer> fitr = first.tuples.iterator();
		Iterator<TupleBuffer> sitr = second.tuples.iterator();
		//nested loop for cross join two tables
		while(fitr.hasNext()){
			TupleBuffer ftuple = fitr.next();
			sitr = second.tuples.iterator();
			while(sitr.hasNext()){
				TupleBuffer stuple = sitr.next();
				res.tuples.add(concatTuples(ftuple,stuple));
			}
		}
		return res;
	}	
	
	TupleBuffer concatTuples(TupleBuffer first, TupleBuffer second){
		TupleBuffer res = new TupleBuffer();
		String firstTableName = first.tableName;
		String secondTableName = second.tableName;
			first.fields.forEach((k,v)->{
				String key = k;
				Value value = v;
				//if this table is a intermeidate table
				if(firstTableName==null){res.fields.put(key,value);}
				else{
					res.fields.put(firstTableName.concat(".").concat(key),value);
					}
				});
			second.fields.forEach((k,v)->{
				String key = k;
				Value value = v;
				//if this table is a intermeidate table
				if(secondTableName==null){res.fields.put(key,value);}
				else{
					res.fields.put(secondTableName.concat(".").concat(key),value);
					}
				});
			return res;
		}

	
	
	//selection operation
	Table selection(Table table){
		Table res = new Table(table.schema);
		for(int i=0;i<table.tuples.size();i++){
			TupleBuffer tuple = table.tuples.get(i);
			if(evaluate(tuple))res.tuples.add(tuple);
		}
		return res;
	}
	
	//expression evaluation
	private boolean evaluate(TupleBuffer tb){
		//just for testing, change the public to private afterwards
		WhereVisitor visitor= new WhereVisitor(select, tb);
		visitor.startVisit();
		return visitor.evaluateWhere();
	}
	
	
	//logical plan generator
	public void execute(){
		System.out.print("\n=================Selection======================\n");
		
		SelectVisitorForNonWhere visitor= new SelectVisitorForNonWhere(select);
		visitor.startVisit();
		List<String> tablenames =visitor.getTableList();
		List<Table> tables = new ArrayList<>();
		tablenames.forEach((name)->{tables.add(extractTable(name));});
		Table res = order(duplicate(project(selection((crossJoin(tables))))));
		Iterator<TupleBuffer> itr = res.tuples.iterator();
		//rendering the fieldNames
		System.out.println(Arrays.toString(res.schema.toArray()));
		while(itr.hasNext()){
			TupleBuffer tuple =itr.next();
			Iterator<Value> itrV = tuple.fields.values().iterator();
				while(itrV.hasNext()){
					Value v = itrV.next();
					if(v.integer==null)System.out.print(v.str+" ");
					else System.out.print(v.integer+" ");
				}
				System.out.println("");
		}
	}
	
	public Table getResult(){
		
		SelectVisitorForNonWhere visitor= new SelectVisitorForNonWhere(select);
		visitor.startVisit();
		List<String> tablenames =visitor.getTableList();
		List<Table> tables = new ArrayList<>();
		tablenames.forEach((name)->{tables.add(extractTable(name));});
		Table res = order(duplicate(project(selection((crossJoin(tables))))));
		
		return res;
	}
	
	
	
	//appropriately output the table from disk and package it into a list of tupleBuffer
	public Table extractTable(String tableName){
		Relation relation = manager.schema_manager.getRelation(tableName);
		Set<String> schema = new HashSet<>();
		schema.addAll(relation.getSchema().getFieldNames());
		if(relation==null)System.out.println("relation doesn't exists!");
		TupleBuffer tb = new TupleBuffer(tableName,schema);
		Table res = new Table(tableName, schema);
		//read from Disk to mem
		for(int i=0;i<relation.getNumOfBlocks();i++){
			//get one block each time
			relation.getBlock(i, 9);
			Block block =manager.memo.getBlock(9);
			//for each tuple
			for(int j =0; j<block.getNumTuples();j++){
				Tuple tuple = block.getTuple(j);
				tb = new TupleBuffer(tableName,schema);
				//pre-processing the relation by outputting them into the tupleBuffer
				 for(int k=0;k<tuple.getNumOfFields();k++){
					 String name = relation.getSchema().getFieldName(k);
					 Field field = tuple.getField(k);
					 if(field.type.name()=="INT"){
						 tb.fields.put(name, new Value(field.integer));
					 }
					 else
					 {
						 tb.fields.put(name, new Value(field.str));	 
					 }
				 }
				 res.tuples.add(tb);
				 }
		
		}
		return res;
		}
		

}
