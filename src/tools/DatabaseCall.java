package tools;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseCall implements AutoCloseable {

	private PreparedStatement statement;
	
	private boolean isClosed = false;
	
	private ResultSet result = null;
	private int affectedRows = -1;

	private DatabaseCall(PreparedStatement statement) {
		this.statement = statement;
	}

	public ResultSet resultSet() {
		if (this.isClosed) {
			throw new IllegalStateException("Attempted to retrieve result set for a closed database operation");
		}
		if (this.result == null) {
			throw new IllegalStateException("Attempted to retrieve result set for a non-query database operation.");
		}
		return this.result;
	}
	
	public int affectedRows() {
		if (this.isClosed) {
			throw new IllegalStateException("Attempted to retrieve affected rows for a closed database operation");
		}
		if (this.affectedRows == -1) {
			throw new IllegalStateException("Attempted to retrieve affected rows for a non-update database operation.");
		}
		return this.affectedRows;
	}

	public static DatabaseCall query(PreparedStatement statement) throws SQLException {
		DatabaseCall call = new DatabaseCall(statement);
		call.result = call.statement.executeQuery();
		return call;
	}
	
	public static DatabaseCall update(PreparedStatement statement, boolean getGeneratedKeys) throws SQLException {
		DatabaseCall call= new DatabaseCall(statement);
		call.affectedRows = call.statement.executeUpdate();
		if (getGeneratedKeys) {
			call.result = call.statement.getGeneratedKeys();
		}
		return call;
	}

	@Override
	public void close() throws Exception {
		this.isClosed = true;
		
		final PreparedStatement statementCopy = this.statement;
		if (statementCopy != null) {
			statementCopy.close();
		}

		final ResultSet resultCopy = this.result;
		if (resultCopy != null) {
			resultCopy.close();
		}
	}
}
