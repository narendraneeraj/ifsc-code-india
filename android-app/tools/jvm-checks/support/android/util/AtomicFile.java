package android.util;
import java.io.*;import java.nio.file.*;
public class AtomicFile {
 private final File base,temp;
 public AtomicFile(File base){this.base=base;this.temp=new File(base+".new");}
 public FileInputStream openRead()throws FileNotFoundException{return new FileInputStream(base);}
 public FileOutputStream startWrite()throws IOException{base.getParentFile().mkdirs();return new FileOutputStream(temp);}
 public void finishWrite(FileOutputStream out){try{out.getFD().sync();out.close();Files.move(temp.toPath(),base.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException e){throw new RuntimeException(e);}}
 public void failWrite(FileOutputStream out){try{out.close();}catch(IOException ignored){}temp.delete();}
}
