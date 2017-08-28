package TinySql.TinySql;


//container and pre-processor of columnName
class ColumnName{
	public String fieldName;
	public String tableName;
	public ColumnName(String s){
		if(s.contains(".")){
			int index = s.indexOf(".");
			tableName = s.substring(0, index);
			fieldName = s.substring(index+1);
		}
		else{
			fieldName = s;
			tableName = null; 
			
		}
		
	}
	public boolean hasTableName(){
		return (tableName != null);
	}
}
