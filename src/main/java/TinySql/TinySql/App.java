package TinySql.TinySql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
//us JAVAcc as the parser lib
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

/**
 * Hello world!
 *
 */
public class App 
{	
    public StorageManager manager;
	
	public App(){
		manager = new StorageManager();
	}
	
	 void evaluate(String expr){
		
		 final Stack<String> stack = new Stack<String>();
		 Statement statement;
		try {
			statement = CCJSqlParserUtil.parse(expr);
			 //if it is select, call selectSubRoutine
			 if (statement instanceof Select){
				 if(((Select) statement).getSelectBody().toString().contains("(")){
					 expr = expr.replaceAll("\\(", "");expr = expr.replaceAll("\\)", "");
					 statement= CCJSqlParserUtil.parse(expr);
					 }
				 
					SelectSubRoutine s = new SelectSubRoutine((Select)statement, manager);
					s.execute();
			 }
			 //if it is select, call createSubRoutine
			 else if(statement instanceof CreateTable){
				 CreateSubRoutine c = new CreateSubRoutine((CreateTable)statement, manager);
				 c.execute();	 
			 }
			 else if(statement instanceof Insert){
				 InsertSubRoutine i = new InsertSubRoutine((Insert)statement, manager);
				 i.execute();
			 }
			 else if(statement instanceof Drop){
				 DropSubRoutine d = new DropSubRoutine((Drop)statement, manager);
				 d.execute();
			 }
			 else if(statement instanceof Delete){
				 DeleteSubRoutine del = new DeleteSubRoutine((Delete)statement, manager);
				 del.execute();
			 }
			 else {System.out.print("this is not a valid expr");
			 }
		}catch (JSQLParserException e) {
			// TODO Auto-generated catch block
			System.out.println("Invalid input, check your syntax");
		}
	 }
	 
	 
	 
    public static void main( String[] args ) throws JSQLParserException
    {
    	App app = new App();
//    	Scanner scanFile;
//    	long startTime = System.currentTimeMillis();
//		try {
//			scanFile = new Scanner(new File("/Users/zhangsikai/Documents/workspace/TinySql/src/main/java/TinySql/TinySql/script"));
//			while(scanFile.hasNextLine()){
//	    		app.evaluate(scanFile.nextLine());	
//	    	}
//			long endTime   = System.currentTimeMillis();
//			long totalTime = endTime - startTime;
//			System.out.println("\n Finished! it takes "+totalTime);
//			
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			System.out.println("Can't find the file!");
//		}
    	
    	
    	System.out.println("********************************************************************");
    	System.out.println("Welcome to Tiny Sql Parser written by Sikai Zhang 223009428");
    	System.out.println("********************************************************************");
    	while(true){
    		System.out.println("Choose your way to input the queries:");
    		System.out.println("\tif you want to input using .txt file, please type 1");
    		System.out.println("\tif you want to input using manually typing, please type 2");
    		System.out.println("\tif you want to terminate the program, please type exit");
    		System.out.println("************************");
    		System.out.print("\nI choose:");
    		Scanner scanFirstMenu=new Scanner(System.in);
    		String inputFirstMenu = scanFirstMenu.nextLine().toLowerCase();
    		
    		//chosse 1
    		if(inputFirstMenu.contains("1")){
    			// change the set out
    			final PrintStream oldStdout = System.out;
    			System.out.println("Now you can start to input your query using the address of the file in your system.\nType back to go 'back' to main menu!");
    			while(true){
    				System.out.print("\nyour input:");
    				Scanner scanFileAddress = new Scanner(System.in);
    				String input = scanFileAddress.nextLine().toLowerCase();
    				
    				if(input.contains("back"))break;
    				else{
//    				"/Users/zhangsikai/Documents/workspace/TinySql/src/main/java/TinySql/TinySql/script"
    					System.out.print("\nyour output address:");
        				Scanner output = new Scanner(System.in);
        				String address = output.nextLine().toLowerCase();
    		    	Scanner scanFile;
    		    	long startTime = System.currentTimeMillis();
    				try {
    				
    					scanFile = new Scanner(new File(input));
    					PrintStream out = new PrintStream(new FileOutputStream(address));
        				System.setOut(out);
    					while(scanFile.hasNextLine()){
    			    		app.evaluate(scanFile.nextLine());	
    			    	}
    					long endTime   = System.currentTimeMillis();
        				long totalTime = endTime - startTime;
        				System.setOut(oldStdout);
        				System.out.println("\n Finished! it takes "+totalTime);
    					
    				} catch (FileNotFoundException e) {
    					// TODO Auto-generated catch block
    					System.out.println("Can't find the file!");
    				}
    				}
    			}	
    		}
    		
    		//choose 2
    		else if(inputFirstMenu.contains("2"))
    		{
    			System.out.println("Now you can start to input your query using SQL lanuguage\nType 'back' to go back to main menu!");
    			while(true){
    			System.out.print("\nyour input here:");
    			Scanner scanSingleInput = new Scanner(System.in);
    			String input = scanSingleInput.nextLine();
    	
    			//exit 
    			if(input.contains("back")){
    				System.out.println("********************************************************************");
    				System.out.println("you are going back the main menu");break;}
    			else
    				//input	
    				System.out.print("Your result is :\n");
    				app.evaluate(input);
    			}
    		}
    		
    		
    		//choose to go out
    		else if(inputFirstMenu.contains("exit"))break;
    		else System.out.println("Unrecognized command!");
    		
    	}
    	
    }
    
    
    
    
    
}
