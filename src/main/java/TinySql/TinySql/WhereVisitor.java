package TinySql.TinySql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExtractExpression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.ExpressionListItem;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.UnionOp;
import net.sf.jsqlparser.statement.select.ValuesList;
import net.sf.jsqlparser.statement.select.WithItem;
import storageManager.Block;
import storageManager.Tuple;

public class WhereVisitor implements SelectVisitor, FromItemVisitor, ItemsListVisitor, ExpressionVisitor {
	Select select;
	String where;
	TupleBuffer tb;
	Stack<Integer> arithmeticExpression;
	Stack<Boolean> logicalOperationExpression;
	HashMap<String, Boolean> comparisonResults;
	Stack<String> stringExpression;
	boolean thisComparison;

	public WhereVisitor(Select select, TupleBuffer tb) {
		this.select = select;
		this.where = "";
		arithmeticExpression = new Stack<>();
		stringExpression = new Stack<>();
		logicalOperationExpression = new Stack<>();
		// ini the logicalOperationExpression stack in case there is no where
		logicalOperationExpression.push(true);
		this.tb = tb;
		thisComparison = false;
		comparisonResults = new HashMap<>();
	}

	public void startVisit() {
		select.getSelectBody().accept(this);
	}

	// start visit the select body
	public void visit(PlainSelect plainSelect) {
		// find if there is conditions
		if (plainSelect.getWhere() != null) {
			where = plainSelect.getWhere().toString();
			logicalOperationExpression.clear();
			plainSelect.getWhere().accept(this);
		}

	}

	// test if this tuple fulfill the conditions
	public boolean evaluateWhere() {
		// this statement does not have a where expression
		if (where == "")
			return true;
		// where has one comparison
		else if (isLogicalTerminal(where)) {
			return thisComparison;
		} else
			return logicalOperationExpression.pop();
	}

	public boolean evaluateComparison(String expression) {
		if (comparisonResults.containsKey(expression)) {
			return comparisonResults.get(expression);
		}
		// this should never happen
		else {
			System.out.print("No such expression!" + expression + "\n");
			return thisComparison;
		}
	}

	public int evaluateExpression(String expression) {
		try {
			arithmeticExpression = new Stack<>();
			Expression parseExpression = CCJSqlParserUtil.parseExpression(expression);
			parseExpression.accept(this);
		} catch (JSQLParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// if the expression is just a terminal: number or string
		if (isTerminal(expression)) {
			return parseTerminal(expression);
		}
		// else
		else
			return arithmeticExpression.pop();
	}

	public boolean isLogicalTerminal(String s) {
		return !s.contains("AND") && !s.contains("OR");

	}

	public void visitLogicalOperationExpression(BinaryExpression binaryExpression) {
		binaryExpression.getLeftExpression().accept(this);
		binaryExpression.getRightExpression().accept(this);
		boolean left = false;
		boolean right = false;
		boolean res = false;
		String leftExpression = binaryExpression.getLeftExpression().toString();
		String rightExpression = binaryExpression.getRightExpression().toString();

		boolean leftIsTerminal = isLogicalTerminal(leftExpression);
		boolean rightIsTerminal = isLogicalTerminal(rightExpression);
		// visit leaf of the tree
		if (leftIsTerminal && rightIsTerminal) {
			left = evaluateComparison(leftExpression);
			right = evaluateComparison(rightExpression);
		}
		// non leaf
		else {
			// left is terminal
			if (leftIsTerminal) {
				left = evaluateComparison(leftExpression);
				right = logicalOperationExpression.pop();
			}
			// right is terminal
			else if (rightIsTerminal) {
				right = evaluateComparison(rightExpression);
				left = logicalOperationExpression.pop();
			}
			// both non-terminal
			else {
				right = logicalOperationExpression.pop();
				left = logicalOperationExpression.pop();
			}
		}
		if (binaryExpression instanceof AndExpression) {
			res = left && right;
		} else if (binaryExpression instanceof OrExpression) {
			res = left || right;
		}
		// this should never happen
		else {
			System.out.print("Undefined operation!");
		}
		logicalOperationExpression.push(res);
	}

	// check if the column name is valid
	public boolean isValidStringColumnName(String s) {
		// if there is a "."
//		ColumnName columnName = new ColumnName(s);
//		if (columnName.hasTableName())
//			s = columnName.fieldName;
		// the table we scan has the key and this value is an integer value
		return tb.fields.containsKey(s) && tb.fields.get(s).str != null;
	}

	// parse the string column
	public String parseStringColumn(String string) {
		// if the terminal is a variable(a column)
		if (isValidStringColumnName(string)) {
//			ColumnName columnName = new ColumnName(string);
//			if (columnName.hasTableName())
//				string = columnName.fieldName;
			return tb.fields.get(string).str;
		} else
			System.out.print("Invalid column name");
		// this should never happen
		return "Invalid column name";
	}

	// determine the expression is actually a string
	public boolean isStringExpression(Expression expression) {
		return isValidStringColumnName(expression.toString()) || isStringValue(expression);
	}

	public boolean isStringValue(Expression expression) {
		String s = expression.toString();
		return s.startsWith("\"") && s.endsWith("\"");
	}

	public String parseStringTerm(Expression expression) {
		String s = expression.toString();
		String res = "";
		// get column value
		if (isValidStringColumnName(s)) {
			res = parseStringColumn(s);
		}

		// get escaped string
		else if (isStringValue(expression)) {
			res = expression.toString();
		}
		// this should never happen
		else {
			System.out.print("undefined expression!");
		}
		return res;

	}

	// recursively visit the tree about comparison
	public void visitComparisonExpression(BinaryExpression binaryExpression) {
		String left = binaryExpression.getLeftExpression().toString();
		String right = binaryExpression.getRightExpression().toString();
		// for simplicity, I don't want to write a specific method dealing with
		// string, I'll deal with string in this method directly
		if (isStringExpression(binaryExpression.getLeftExpression())
				&& isStringExpression(binaryExpression.getRightExpression())) {
			left = parseStringTerm(binaryExpression.getLeftExpression());
			right = parseStringTerm(binaryExpression.getRightExpression());
			thisComparison = left.equals(right);
		} else if (binaryExpression instanceof EqualsTo) {
			thisComparison = (evaluateExpression(left) == evaluateExpression(right));
		}
		// >
		else if (binaryExpression instanceof GreaterThan) {
			thisComparison = (evaluateExpression(left) > evaluateExpression(right));
		}
		// >=
		else if (binaryExpression instanceof GreaterThanEquals) {
			thisComparison = (evaluateExpression(left) >= evaluateExpression(right));
		}
		// <
		else if (binaryExpression instanceof MinorThan) {
			thisComparison = (evaluateExpression(left) < evaluateExpression(right));
		}
		// <=
		else if (binaryExpression instanceof MinorThanEquals) {
			thisComparison = (evaluateExpression(left) <= evaluateExpression(right));
		}
		// this should never happens
		else {

			System.out.print("Undefined comparison operation!");
		}
		comparisonResults.put(binaryExpression.toString(), thisComparison);

	}

	// determine if a string is a Integer
	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
	}

	public int parseTerminal(String string) {
		// if the terminal is a integer string
		if (isInteger(string))
			return Integer.parseInt(string);
		// if the terminal is a variable(a column)
		else if (isValidIntegerColumnName(string)) {
//			ColumnName columnName = new ColumnName(string);
//			if (columnName.hasTableName())
//				string = columnName.fieldName;
			return tb.fields.get(string).integer;
		} else
			System.out.print("In valid column name");
		// this should never happen
		return Integer.MAX_VALUE;
	}

	// check if the column name is valid
	public boolean isValidIntegerColumnName(String s) {
		// if there is a "."
//		ColumnName columnName = new ColumnName(s);
//		if (columnName.hasTableName())
//			s = columnName.fieldName;
		// the table we scan has the key and this value is an integer value
		return tb.fields.containsKey(s) && tb.fields.get(s).str == null;
	}

	// determine if it is an expression or terminal
	public boolean isTerminal(String s) {
		return isInteger(s) || isValidIntegerColumnName(s);
	}

	public void visitArithmeticExpression(BinaryExpression binaryExpression) {
		binaryExpression.getLeftExpression().accept(this);
		binaryExpression.getRightExpression().accept(this);
		int left = 0;
		int right = 0;
		int res = 0;
		String leftExpression = binaryExpression.getLeftExpression().toString();
		String rightExpression = binaryExpression.getRightExpression().toString();
		boolean leftIsTerminal = isTerminal(leftExpression);
		boolean rightIsTerminal = isTerminal(rightExpression);
		// visit leaf of the tree
		if (rightIsTerminal && leftIsTerminal) {
			left = parseTerminal(binaryExpression.getLeftExpression().toString());
			right = parseTerminal(binaryExpression.getRightExpression().toString());
		}
		// non-leaf
		else {
			// left is terminal
			if (leftIsTerminal) {
				right = arithmeticExpression.pop();
				left = parseTerminal(binaryExpression.getLeftExpression().toString());
			}
			// right is terminal
			else if (rightIsTerminal) {
				left = arithmeticExpression.pop();
				right = parseTerminal(binaryExpression.getRightExpression().toString());
			}
			// both not terminal
			else {
				right = arithmeticExpression.pop();
				left = arithmeticExpression.pop();
			}
		}
		if (binaryExpression instanceof Addition) {
			res = left + right;
		} else if (binaryExpression instanceof Subtraction) {
			res = left - right;
		} else if (binaryExpression instanceof Multiplication) {
			res = left * right;
		} else if (binaryExpression instanceof Division) {
			res = left / right;
		} else {
			System.out.print("Undefined operation!");
		}
		arithmeticExpression.push(res);
	}

	public void visit(ExpressionList expressionList) {
		// TODO Auto-generated method stub

	}

	public void visit(MultiExpressionList arg0) {
		// TODO Auto-generated method stub

	}

	public void visit(SubJoin subjoin) {
		subjoin.getLeft().accept(this);
		subjoin.getJoin().getRightItem().accept(this);
	}

	public void visit(LateralSubSelect arg0) {
		// TODO Auto-generated method stub

	}

	public void visit(ValuesList arg0) {
		// TODO Auto-generated method stub

	}

	public void visit(SetOperationList arg0) {
		// TODO Auto-generated method stub

	}

	public void visit(WithItem arg0) {
		// TODO Auto-generated method stub

	}

	public void visit(SubSelect arg0) {
		// TODO Auto-generated method stub

	}

	public void visit(NullValue nullValue) {
		// TODO Auto-generated method stub

	}

	public void visit(Function function) {
		// TODO Auto-generated method stub

	}

	public void visit(SignedExpression signedExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(JdbcParameter jdbcParameter) {
		// TODO Auto-generated method stub

	}

	public void visit(JdbcNamedParameter jdbcNamedParameter) {
		// TODO Auto-generated method stub

	}

	public void visit(DoubleValue doubleValue) {
		// TODO Auto-generated method stub

	}

	public void visit(LongValue longValue) {
		// TODO Auto-generated method stub

	}

	public void visit(DateValue dateValue) {
		// TODO Auto-generated method stub

	}

	public void visit(TimeValue timeValue) {
		// TODO Auto-generated method stub

	}

	public void visit(TimestampValue timestampValue) {
		// TODO Auto-generated method stub

	}

	public void visit(Parenthesis parenthesis) {
		// TODO Auto-generated method stub
	}

	public void visit(StringValue stringValue) {
		// TODO Auto-generated method stub

	}

	public void visit(Addition addition) {
		// TODO Auto-generated method stub
		visitArithmeticExpression(addition);
	}

	public void visit(Division division) {
		// TODO Auto-generated method stub
		visitArithmeticExpression(division);
	}

	public void visit(Multiplication multiplication) {
		// TODO Auto-generated method stub
		visitArithmeticExpression(multiplication);

	}

	public void visit(Subtraction subtraction) {
		// TODO Auto-generated method stub
		visitArithmeticExpression(subtraction);

	}

	public void visit(AndExpression andExpression) {
		// TODO Auto-generated method stub;
		visitLogicalOperationExpression(andExpression);

	}

	public void visit(OrExpression orExpression) {
		// TODO Auto-generated method stub
		visitLogicalOperationExpression(orExpression);
	}

	public void visit(Between between) {
		// TODO Auto-generated method stub

	}

	public void visit(EqualsTo equalsTo) {
		// TODO Auto-generated method stub
		visitComparisonExpression(equalsTo);

	}

	public void visit(GreaterThan greaterThan) {
		// TODO Auto-generated method stub
		visitComparisonExpression(greaterThan);
	}

	public void visit(GreaterThanEquals greaterThanEquals) {
		// TODO Auto-generated method stub
		visitComparisonExpression(greaterThanEquals);
	}

	public void visit(InExpression inExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(IsNullExpression isNullExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(LikeExpression likeExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(MinorThan minorThan) {
		// TODO Auto-generated method stub
		visitComparisonExpression(minorThan);
	}

	public void visit(MinorThanEquals minorThanEquals) {
		// TODO Auto-generated method stub
		visitComparisonExpression(minorThanEquals);
	}

	public void visit(NotEqualsTo notEqualsTo) {
		// TODO Auto-generated method stub
		visitComparisonExpression(notEqualsTo);
	}

	public void visit(Column tableColumn) {
		// TODO Auto-generated method stub

	}

	public void visit(CaseExpression caseExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(WhenClause whenClause) {
		// TODO Auto-generated method stub

	}

	public void visit(ExistsExpression existsExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(AllComparisonExpression allComparisonExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(AnyComparisonExpression anyComparisonExpression) {
		// TODO Auto-generated method stub

	}

	public void visit(Concat concat) {
		// TODO Auto-generated method stub

	}

	public void visit(Matches matches) {
		// TODO Auto-generated method stub

	}

	public void visit(BitwiseAnd bitwiseAnd) {
		// TODO Auto-generated method stub

	}

	public void visit(BitwiseOr bitwiseOr) {
		// TODO Auto-generated method stub

	}

	public void visit(BitwiseXor bitwiseXor) {
		// TODO Auto-generated method stub

	}

	public void visit(CastExpression cast) {
		// TODO Auto-generated method stub

	}

	public void visit(Modulo modulo) {
		// TODO Auto-generated method stub

	}

	public void visit(AnalyticExpression aexpr) {
		// TODO Auto-generated method stub

	}

	public void visit(ExtractExpression eexpr) {
		// TODO Auto-generated method stub

	}

	public void visit(IntervalExpression iexpr) {
		// TODO Auto-generated method stub

	}

	public void visit(OracleHierarchicalExpression oexpr) {
		// TODO Auto-generated method stub

	}

	public void visit(RegExpMatchOperator rexpr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(Table tableName) {
		// TODO Auto-generated method stub

	}

}
