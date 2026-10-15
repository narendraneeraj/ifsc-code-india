package com.bhuvancom.bank_pie;
import android.database.Cursor;
import org.json.*;
import java.util.*;
public final class DirectoryIndex {
 private final BranchRepository.Dataset data;
 public static final class Bank {
  public final String key,name,image;public List<JSONObject> branches=new CountList(0);
  public final SortedMap<String,SortedMap<String,List<JSONObject>>> locations=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  Bank(String key,String name,String image){this.key=key;this.name=name;this.image=image;}
 }
 private static final class CountList extends AbstractList<JSONObject>{final int count;CountList(int count){this.count=count;}public int size(){return count;}public JSONObject get(int i){throw new UnsupportedOperationException("Count-only catalog");}}
 public final SortedMap<String,Bank> banks=new TreeMap<>();
 public final Map<String,JSONObject> byIfsc=new AbstractMap<String,JSONObject>(){public JSONObject get(Object code){try{return data.branch(String.valueOf(code));}catch(Exception e){throw new IllegalStateException(e);}}public Set<Entry<String,JSONObject>> entrySet(){throw new UnsupportedOperationException();}};
 public DirectoryIndex(BranchRepository.Dataset data)throws Exception {
  this.data=data;List<Bank> ordered=new ArrayList<>();for(int i=0;i<data.banks.length();i++){JSONObject row=data.banks.getJSONObject(i);String id=BranchRepository.clean(row,"bank_id");ordered.add(new Bank(id,BranchRepository.clean(row,"bank_name"),BranchRepository.clean(row,"bank_image")));}
  // Linked display order is provided by orderedBanks; IDs remain the lookup keys.
  ordered.sort(Comparator.comparing(b->b.name,String.CASE_INSENSITIVE_ORDER));for(Bank b:ordered)banks.put(b.key,b);
  try(Cursor c=data.db.rawQuery("SELECT bank_id,name FROM states",null)){while(c.moveToNext()){Bank b=banks.get(c.getString(0));if(b!=null)b.locations.computeIfAbsent(c.getString(1),x->new TreeMap<>(String.CASE_INSENSITIVE_ORDER));}}
  try(Cursor c=data.db.rawQuery("SELECT d.bank_id,s.name,d.name FROM districts d JOIN states s ON s.state_id=d.state_id",null)){while(c.moveToNext()){Bank b=banks.get(c.getString(0));if(b!=null)b.locations.computeIfAbsent(c.getString(1),x->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)).putIfAbsent(c.getString(2),new CountList(0));}}
  Map<String,Integer> bankCounts=new HashMap<>();try(Cursor c=data.db.rawQuery("SELECT bank_id,state_name,district_name,COUNT(*) FROM branches GROUP BY bank_id,state_name COLLATE NOCASE,district_name COLLATE NOCASE",null)){while(c.moveToNext()){Bank b=banks.get(c.getString(0));if(b==null)continue;int n=c.getInt(3);bankCounts.put(b.key,bankCounts.getOrDefault(b.key,0)+n);b.locations.computeIfAbsent(c.getString(1),x->new TreeMap<>(String.CASE_INSENSITIVE_ORDER)).put(c.getString(2),new CountList(n));}}
  for(Bank b:banks.values())b.branches=new CountList(bankCounts.getOrDefault(b.key,0));
 }
 public List<Bank> orderedBanks(){List<Bank> list=new ArrayList<>(banks.values());list.sort(Comparator.comparing(b->b.name,String.CASE_INSENSITIVE_ORDER));return list;}
 public static String key(String value){return value.trim().toLowerCase(Locale.ROOT);}
 public static String location(JSONObject row,String key){String v=BranchRepository.clean(row,key);return v.isEmpty()?BranchRepository.UNKNOWN:v;}
 public List<JSONObject> branches(String bank,String state,String district)throws Exception{return BranchRepository.records(data.db,"SELECT raw FROM branches WHERE bank_id=? AND state_name=? COLLATE NOCASE AND district_name=? COLLATE NOCASE ORDER BY name COLLATE NOCASE",new String[]{bank,state,district});}
}
