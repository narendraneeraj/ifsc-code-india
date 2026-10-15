package org.easycalculator.ifsc;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import org.json.*;
public class SyncTest {
 static File live;static String revision="a".repeat(40);static boolean breakBranch=false;static int downloads=0,checks=0;static void check(boolean v,String label){if(!v)throw new AssertionError(label);checks++;}
 static InputStream payload(URL url)throws Exception {
  String path=url.getPath();if(path.endsWith("/git/ref/heads/main"))return bytes("{\"object\":{\"sha\":\""+revision+"\"}}");
  if(path.endsWith("/commits")){String date=url.getQuery().contains("banks.json")?"2026-10-10T19:30:38Z":"2026-10-10T19:29:42Z";return bytes("[{\"commit\":{\"committer\":{\"date\":\""+date+"\"}}}]");}
  String filename=path.substring(path.lastIndexOf('/')+1);if(!List.of("banks.json","state.json","district.json","branch.json").contains(filename))throw new AssertionError(url);
  check(path.contains("/"+revision+"/data/"),"pinned file revision");downloads++;if(breakBranch&&filename.equals("branch.json"))return bytes("broken JSON");return new FileInputStream(new File(live,filename));
 }
 static InputStream bytes(String text){return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));}
 public static void main(String[] args)throws Exception {
  live=new File(args[0]);URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){protected URLConnection openConnection(URL u){return new HttpURLConnection(u){public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}public int getResponseCode(){return 200;}public InputStream getInputStream()throws IOException{try{return payload(url);}catch(Exception e){throw new IOException(e);}}};}}:null);
  File folder=java.nio.file.Files.createTempDirectory("ifsc-sync-test").toFile();BranchRepository.delete(folder);folder.mkdirs();android.content.Context context=new android.content.Context(folder);
  BranchRepository.Dataset first=BranchRepository.fetch(context,null,message->{});
  check(downloads==4,"all four files downloaded");check(first.branchCount==172248,"full database published");check(first.meta.getString("updated_at").equals("2026-10-10T19:30:38Z"),"latest actual JSON file date");
  int previous=downloads;check(BranchRepository.fetch(context,first,message->{})==first,"unchanged revision reuses snapshot");check(downloads==previous,"unchanged files not re-downloaded");
  JSONObject dates=first.meta.getJSONObject("file_modified");check(dates.length()==4,"all per-file dates cached");dates.put("state.json","");check(BranchRepository.latestDate(dates).isEmpty(),"unknown source date is not replaced by download time");
  revision="b".repeat(40);breakBranch=true;boolean failed=false;try{BranchRepository.fetch(context,first,message->{});}catch(Exception e){failed=true;}check(failed,"invalid update rejected");
  BranchRepository.Dataset offline=BranchRepository.load(context);check(offline.branchCount==172248,"previous cache survives failed update");check(offline.search("ABHY0065001",false).size()==1,"offline indexed search");offline.close();first.close();
  System.out.println(checks+" sync/date/cache checks passed against the full data files");
 }
}
