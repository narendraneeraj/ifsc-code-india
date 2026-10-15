package com.bhuvancom.bank_pie;
import java.io.*;import java.util.*;import org.json.*;import android.database.sqlite.*;
public class StoreTest {
 static int checks=0;static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception {
  File dir=new File(args[0]),dbFile=new File(java.nio.file.Files.createTempDirectory("ifsc-store-test").toFile(),"directory.db");dbFile.delete();SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(dbFile,null);
  long start=System.currentTimeMillis();JSONObject stats=BranchRepository.importFiles(dir,db);System.out.println("Import stats: "+stats);
  BranchRepository.Dataset data=new BranchRepository.Dataset(stats,db,dir);DirectoryIndex index=new DirectoryIndex(data);
  check(data.banks.length()==301,"301 banks");check(stats.getInt("downloaded_records")==169999,"source records");check(stats.getInt("state_count")==1716,"state fetch");check(stats.getInt("district_count")==17735,"district fetch");
  DirectoryIndex.Bank b=index.banks.get("2");check(b!=null&&!b.locations.isEmpty(),"bank to state browsing");check(b.locations.containsKey("MAHARASHTRA"),"state names loaded");
  check(b.locations.get("MAHARASHTRA").containsKey("Aurangabad_Mh"),"district names loaded");List<JSONObject> branches=index.branches("2","MAHARASHTRA","Aurangabad_Mh");check(!branches.isEmpty(),"location branch listing");
  JSONObject first=index.byIfsc.get("ABHY0065001");check(first!=null,"IFSC lookup");check(first.getString("state").equals("MAHARASHTRA"),"state join");check(first.getString("district").equals("Aurangabad_Mh"),"district join");check(first.getString("bank_contact").equals("022-25260172"),"bank table phone mapping");
  List<JSONObject> matches=data.search("ABHY",false);check(matches.size()==8,"eight-result cap");check(data.search("ABHY0065001",false).size()==1,"exact IFSC");check(!data.search("400",true).isEmpty(),"new MICR source supported");
  check(data.branch("ABHY0065001").getString("bank_image").equals("images/abhyudaya_cooperative_bank.png"),"GitHub logo paths");
  check(first.getString("bank_website").equals("https://www.abhyudayabank.co.in/"),"bank website join");check(first.getString("bank_email").equals("neftcell@abhyudayabank.net"),"bank email join");check(first.getString("micr").equals("400065001"),"numeric MICR preserved");check(data.branchCount==169964,"orphan bank rows excluded");
  data.close();SQLiteDatabase reopened=SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);BranchRepository.Dataset offline=new BranchRepository.Dataset(stats,reopened,dir);check(offline.search("ABHY0065001",false).size()==1,"offline reopen");offline.close();
  System.out.println(checks+" full-dataset import/query/offline checks passed in "+(System.currentTimeMillis()-start)+" ms");
 }
}
