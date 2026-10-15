package android.database.sqlite;
import java.sql.*;import java.io.*;import android.database.Cursor;
public class SQLiteDatabase {
 public interface CursorFactory {}public static final int OPEN_READONLY=1;final Connection connection;private boolean success;
 private SQLiteDatabase(String file){try{connection=DriverManager.getConnection("jdbc:sqlite:"+file);}catch(Exception e){throw new RuntimeException(e);}}
 public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){return new SQLiteDatabase(path);}
 public static SQLiteDatabase openOrCreateDatabase(File file,CursorFactory factory){return new SQLiteDatabase(file.getAbsolutePath());}
 public void execSQL(String sql){try(Statement s=connection.createStatement()){s.execute(sql);}catch(SQLException e){throw new RuntimeException(e);}}
 public SQLiteStatement compileStatement(String sql){try{return new SQLiteStatement(connection.prepareStatement(sql));}catch(SQLException e){throw new RuntimeException(e);}}
 public Cursor rawQuery(String sql,String[] args){try{PreparedStatement s=connection.prepareStatement(sql);if(args!=null)for(int i=0;i<args.length;i++)s.setString(i+1,args[i]);ResultSet rs=s.executeQuery();return new Cursor(){public boolean moveToNext(){try{return rs.next();}catch(SQLException e){throw new RuntimeException(e);}}public boolean moveToFirst(){return moveToNext();}public String getString(int i){try{return rs.getString(i+1);}catch(SQLException e){throw new RuntimeException(e);}}public int getInt(int i){try{return rs.getInt(i+1);}catch(SQLException e){throw new RuntimeException(e);}}public void close(){try{rs.close();s.close();}catch(SQLException e){throw new RuntimeException(e);}}};}catch(SQLException e){throw new RuntimeException(e);}}
 public void beginTransaction(){try{success=false;connection.setAutoCommit(false);}catch(SQLException e){throw new RuntimeException(e);}}
 public void setTransactionSuccessful(){success=true;}
 public void endTransaction(){try{if(success)connection.commit();else connection.rollback();connection.setAutoCommit(true);}catch(SQLException e){throw new RuntimeException(e);}}
 public void close(){try{connection.close();}catch(SQLException e){throw new RuntimeException(e);}}
}
