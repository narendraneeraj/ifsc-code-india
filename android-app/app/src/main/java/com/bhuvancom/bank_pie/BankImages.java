package com.bhuvancom.bank_pie;
import android.app.Activity;
import android.widget.ImageView;
import android.graphics.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
public final class BankImages {
 private String revision="";
 public void setRevision(String value){if(!revision.equals(value)){revision=value;memory.clear();}}
 private final Activity activity;private final ExecutorService pool=Executors.newFixedThreadPool(2);
 private final Map<String,List<java.lang.ref.WeakReference<ImageView>>> waiting=new HashMap<>();
 private final Map<String,Bitmap> memory=Collections.synchronizedMap(new LinkedHashMap<String,Bitmap>(24,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,Bitmap> e){return size()>24;}});
 public BankImages(Activity activity){this.activity=activity;}
 public void load(ImageView view,String image){String path=BranchRepository.imagePath(image);view.setImageResource(R.drawable.ic_bank);if(path.isEmpty()){view.setTag("");return;}String ref=revision.isEmpty()?"main":revision;String cacheKey=path+"#"+ref;view.setTag(cacheKey);
  Bitmap found=memory.get(cacheKey);if(found!=null){view.setImageBitmap(found);List<java.lang.ref.WeakReference<ImageView>> w=waiting.get(cacheKey);if(w!=null)w.add(new java.lang.ref.WeakReference<>(view));return;}
  List<java.lang.ref.WeakReference<ImageView>> watchers=waiting.get(cacheKey);if(watchers!=null){watchers.add(new java.lang.ref.WeakReference<>(view));return;}watchers=new ArrayList<>();watchers.add(new java.lang.ref.WeakReference<>(view));waiting.put(cacheKey,watchers);
  pool.execute(()->{Bitmap result=null;try{File folder=new File(activity.getCacheDir(),"bank-images");folder.mkdirs();File file=new File(folder,ref.substring(0,Math.min(12,ref.length()))+"-"+path.substring(7));byte[] bytes=null;
   if(file.isFile()&&file.length()<=2*1024*1024)try(InputStream in=new FileInputStream(file)){bytes=readBytes(in);}
   result=decode(bytes);if(result!=null){memory.put(cacheKey,result);publish(cacheKey,result,false);}
   if(!file.isFile()||System.currentTimeMillis()-file.lastModified()>7L*24*60*60*1000){
    HttpURLConnection c=(HttpURLConnection)new URL("https://raw.githubusercontent.com/"+BranchRepository.REPO+"/"+ref+"/"+path).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(10000);
    try{if(c.getResponseCode()==200)try(InputStream in=c.getInputStream()){Bitmap fresh=decode(readBytes(in));if(fresh!=null){result=fresh;memory.put(cacheKey,fresh);android.util.AtomicFile cache=new android.util.AtomicFile(file);FileOutputStream out=null;try{out=cache.startWrite();fresh.compress(Bitmap.CompressFormat.PNG,100,out);cache.finishWrite(out);}catch(Exception e){if(out!=null)cache.failWrite(out);}trim(folder);}}}finally{c.disconnect();}
   }
  }catch(Exception ignored){}finally{publish(cacheKey,result,true);}});
 }
 private void publish(String path,Bitmap bitmap,boolean finish){activity.runOnUiThread(()->{List<java.lang.ref.WeakReference<ImageView>> views=finish?waiting.remove(path):waiting.get(path);if(activity.isDestroyed()||bitmap==null||views==null)return;for(java.lang.ref.WeakReference<ImageView> ref:views){ImageView target=ref.get();if(target!=null&&path.equals(target.getTag()))target.setImageBitmap(bitmap);}});}
 private static byte[] readBytes(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Image too large");out.write(buffer,0,n);}return out.toByteArray();}
 private static Bitmap decode(byte[] bytes){if(bytes==null)return null;BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);o.inSampleSize=1;while(o.outWidth/o.inSampleSize>192||o.outHeight/o.inSampleSize>192)o.inSampleSize*=2;o.inJustDecodeBounds=false;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);}
 private static synchronized void trim(File dir){File[] files=dir.listFiles((f)->!f.getName().endsWith(".new")&&!f.getName().endsWith(".bak"));if(files==null)return;long size=0;for(File f:files)size+=f.length();if(size<=12*1024*1024)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(File f:files){long n=f.length();if(f.delete())size-=n;if(size<=8*1024*1024)break;}}
 public void close(){pool.shutdownNow();}
}
