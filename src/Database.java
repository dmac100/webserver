import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hsqldb.cmdline.SqlFile;

public class Database implements AutoCloseable, Closeable {
	private static boolean memDbInitialized = false;
	
	private final Connection connection;
	
	public Database() {
		try {
			this.connection = DriverManager.getConnection("jdbc:hsqldb:mem:test");
			if(!memDbInitialized) {
				init();
				memDbInitialized = true;
			}
		} catch(SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Database(File file) {
		try {
			boolean initialized = new File(file.getParentFile(), file.getName() + ".script").exists();
			this.connection = DriverManager.getConnection("jdbc:hsqldb:file:" + file.toString());
			if(!initialized) {
				init();
			}
		} catch(SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void init() {
		try {
			try(InputStream inputStream = ResourceFileSystem.getInputStream("/sql/init.sql")) {
				SqlFile sqlFile = new SqlFile(new InputStreamReader(inputStream), "init", System.out, "UTF-8", false, new File("."));
				sqlFile.setConnection(connection);
				sqlFile.execute();
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void reset() {
		runSql("drop schema public cascade");
		init();
	}
	
	public List<Map<String, Object>> runSql(String sql, Object... parameters) {
		List<Map<String, Object>> results = new ArrayList<>();
		
		try(PreparedStatement statement = connection.prepareStatement(sql)) {
			for(int i = 0; i < parameters.length; i++) {
				statement.setObject(i + 1, parameters[i]);
			}
			
			if(statement.execute()) {
				try(ResultSet resultSet = statement.getResultSet()) {
					while(resultSet.next()) {
						Map<String, Object> row = new LinkedHashMap<>();
						for(int column = 1; column <= resultSet.getMetaData().getColumnCount(); column++) {
							String name = resultSet.getMetaData().getColumnName(column);
							Object value = resultSet.getObject(name);
							row.put(name, value);
						}
						results.add(row);
					}
				}
			}
		} catch(SQLException e) {
			throw new RuntimeException(e);
		}
		
		return results;
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch(SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
