package com.bhuvancom.bank_pie;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.AtomicFile;
import android.util.JsonReader;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BranchRepository {
 public static final String REPO="narendraneeraj/ifsc-code-india",BASE="https://raw.githubusercontent.com/"+REPO+"/main/",UNKNOWN="Not provided";
 public static final String DATA_PATH="json-datasets/json/", IMAGE_PATH="json-datasets/";
 private static final String API="https://api.github.com/repos/"+REPO;
 private static final String[] FILES={"banks.json","state.json","district.json","branch.json"};
 public static final class HttpError extends IOException {public final int code;HttpError(int code){super("HTTP "+code);this.code=code;}}
 public static final class Dataset {
  public final JSONObject meta;public final JSONArray banks;public final SQLiteDatabase db;public final int branchCount;public final File directory;
  Dataset(JSONObject meta,SQLiteDatabase db,File directory)throws Exception{this.meta=meta;this.db=db;this.directory=directory;banks=new JSONArray();try(Cursor c=db.rawQuery("SELECT raw FROM banks ORDER BY name COLLATE NOCASE",null)){while(c.moveToNext())banks.put(new JSONObject(c.getString(0)));}branchCount=count(db,"branches");}
  public void close(){db.close();}
  public List<JSONObject> searchSwift(String query)throws Exception{
   if(meta.optInt("schema_version")<3)return Collections.emptyList();
   return records(db,"SELECT raw FROM branches WHERE swift <> '' AND swift LIKE ? ORDER BY swift,ifsc LIMIT 8",new String[]{query+"%"});
  }
  public boolean hasSwift(){if(meta.optInt("schema_version")<3)return false;try(Cursor c=db.rawQuery("SELECT 1 FROM branches WHERE swift <> '' LIMIT 1",null)){return c.moveToFirst();}}

  public JSONObject branch(String ifsc)throws Exception{try(Cursor c=db.rawQuery("SELECT raw FROM branches WHERE ifsc=?",new String[]{ifsc})){return c.moveToFirst()?new JSONObject(c.getString(0)):null;}}
  public List<JSONObject> search(String query,boolean micr)throws Exception{String column=micr?"micr":"ifsc";return records(db,"SELECT raw FROM branches WHERE "+column+" LIKE ? ORDER BY CASE WHEN "+column+"=? THEN 0 ELSE 1 END, "+column+" LIMIT 8",new String[]{query+"%",query});}
 }
 public static String clean(JSONObject row,String key){return row.isNull(key)?"":row.optString(key,"").trim();}
 public static String first(JSONObject row,String... keys){for(String k:keys){String v=clean(row,k);if(!v.isEmpty()&&!v.equalsIgnoreCase("null"))return v;}return "";}
 public static String imagePath(String value){String p=value.trim();if(p.startsWith(BASE))p=p.substring(BASE.length());if(p.startsWith(IMAGE_PATH))p=p.substring(IMAGE_PATH.length());if(p.matches("[A-Za-z0-9_.-]+\\.(png|webp|jpg|jpeg)"))p="images/"+p;return p.matches("images/[A-Za-z0-9_.-]+\\.(png|webp|jpg|jpeg)")&&!p.contains("..")?p:"";}
 private static int count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getInt(0);}}
 public static List<JSONObject> records(SQLiteDatabase db,String sql,String[] args)throws Exception{List<JSONObject> rows=new ArrayList<>();try(Cursor c=db.rawQuery(sql,args)){while(c.moveToNext())rows.add(new JSONObject(c.getString(0)));}return rows;}
 public static Dataset load(Context context)throws Exception {
  File root=new File(context.getFilesDir(),"github-directory-v5");AtomicFile pointer=new AtomicFile(new File(root,"current.json"));JSONObject meta;try(InputStream in=pointer.openRead()){meta=new JSONObject(read(in,1024*1024));}
  String folder=clean(meta,"folder");if(!folder.matches("snapshot-[A-Za-z0-9-]+"))throw new IOException("Invalid cache pointer");File directory=new File(root,folder),dbFile=new File(directory,"directory.db");if(!dbFile.isFile())throw new IOException("Missing cache database");SQLiteDatabase db=SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);try{return new Dataset(meta,db,directory);}catch(Exception e){db.close();throw e;}
 }
 public interface Progress {void update(String message);}
 public static Dataset fetch(Context context,Dataset previous,Progress progress)throws Exception {
  String revision="";try{JSONObject ref=new JSONObject(downloadMetadata(API+"/git/ref/heads/main",128*1024));revision=ref.getJSONObject("object").getString("sha");}catch(Exception ignored){}
  if(previous!=null&&!revision.isEmpty()&&revision.equals(clean(previous.meta,"revision"))&&previous.meta.optInt("schema_version")==4){if(clean(previous.meta,"updated_at").isEmpty()){updateDates(previous.meta,revision,progress);writePointer(context,previous.meta);}return previous;}
  File root=new File(context.getFilesDir(),"github-directory-v5");root.mkdirs();String folder="snapshot-"+UUID.randomUUID();File stage=new File(root,folder);stage.mkdirs();SQLiteDatabase db=null;
  try {
   String ref=revision.isEmpty()?"main":revision,raw="https://raw.githubusercontent.com/"+REPO+"/"+ref+"/";
   for(String name:FILES){progress.update("Downloading "+name);downloadFile(raw+DATA_PATH+name,new File(stage,name),name.equals("branch.json")?160L*1024*1024:16L*1024*1024,progress);}
   db=SQLiteDatabase.openOrCreateDatabase(new File(stage,"directory.db"),null);progress.update("Preparing offline search…");JSONObject meta=importFiles(stage,db);meta.put("folder",folder);meta.put("revision",revision);meta.put("source","GitHub: "+REPO+" / "+DATA_PATH);meta.put("sample_data",false);
   updateDates(meta,revision,progress);db.close();db=null;
   SQLiteDatabase ready=SQLiteDatabase.openDatabase(new File(stage,"directory.db").getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);Dataset dataset;try{dataset=new Dataset(meta,ready,stage);}catch(Exception e){ready.close();throw e;}
   AtomicFile pointer=new AtomicFile(new File(root,"current.json"));FileOutputStream out=null;try{out=pointer.startWrite();out.write(meta.toString().getBytes(StandardCharsets.UTF_8));pointer.finishWrite(out);}catch(Exception e){if(out!=null)pointer.failWrite(out);dataset.close();throw e;}
   for(String name:FILES)new File(stage,name).delete();return dataset;
  }catch(Exception e){if(db!=null)db.close();delete(stage);throw e;}
 }
 private static void updateDates(JSONObject meta,String revision,Progress progress)throws Exception {
  progress.update("Checking JSON modification dates…");JSONObject modified=new JSONObject();for(String name:FILES){String date="";try{String url=API+"/commits?path=json-datasets%2Fjson%2F"+name+"&per_page=1"+(revision.isEmpty()?"":"&sha="+revision);JSONArray commits=new JSONArray(downloadMetadata(url,1024*1024));if(commits.length()>0)date=commits.getJSONObject(0).getJSONObject("commit").getJSONObject("committer").getString("date");}catch(Exception ignored){}modified.put(name,date);}meta.put("file_modified",modified);meta.put("updated_at",latestDate(modified));
 }
 public static String latestDate(JSONObject modified){String latest="";for(String name:FILES){String date=clean(modified,name);if(date.isEmpty())return "";if(date.compareTo(latest)>0)latest=date;}return latest;}
 private static void writePointer(Context context,JSONObject meta)throws Exception{File root=new File(context.getFilesDir(),"github-directory-v5");AtomicFile pointer=new AtomicFile(new File(root,"current.json"));FileOutputStream out=null;try{out=pointer.startWrite();out.write(meta.toString().getBytes(StandardCharsets.UTF_8));pointer.finishWrite(out);}catch(Exception e){if(out!=null)pointer.failWrite(out);throw e;}}
 private static String downloadMetadata(String url,int limit)throws Exception{HttpURLConnection c=connection(url);c.setConnectTimeout(5000);c.setReadTimeout(5000);try{int code=c.getResponseCode();if(code!=200)throw new HttpError(code);try(InputStream in=c.getInputStream()){return read(in,limit);}}finally{c.disconnect();}}
 private static final class Location {String id,bank,state,name;Location(String id,String bank,String state,String name){this.id=id;this.bank=bank;this.state=state;this.name=name;}}
 interface RowConsumer {void accept(JSONObject row)throws Exception;}
 static void each(File file,RowConsumer consumer)throws Exception{try(JsonReader reader=new JsonReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))){reader.beginArray();while(reader.hasNext()){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();consumer.accept(object(reader));}reader.endArray();if(reader.peek()!=android.util.JsonToken.END_DOCUMENT)throw new IOException("Trailing JSON data");}}
 private static JSONObject object(JsonReader reader)throws Exception{JSONObject row=new JSONObject();reader.beginObject();while(reader.hasNext()){String key=reader.nextName();row.put(key,value(reader));}reader.endObject();return row;}
 private static Object value(JsonReader r)throws Exception{switch(r.peek()){case BEGIN_OBJECT:return object(r);case BEGIN_ARRAY:JSONArray a=new JSONArray();r.beginArray();while(r.hasNext())a.put(value(r));r.endArray();return a;case BOOLEAN:return r.nextBoolean();case NULL:r.nextNull();return JSONObject.NULL;case NUMBER:return r.nextString();case STRING:return r.nextString();default:throw new IOException("Invalid JSON value");}}
 public static JSONObject importFiles(File directory,SQLiteDatabase db)throws Exception {
  db.execSQL("CREATE TABLE banks (bank_id TEXT PRIMARY KEY, name TEXT NOT NULL, image TEXT, raw TEXT NOT NULL)");
  db.execSQL("CREATE TABLE states (state_id TEXT PRIMARY KEY, bank_id TEXT, name TEXT)");
  db.execSQL("CREATE TABLE districts (district_id TEXT PRIMARY KEY, bank_id TEXT, state_id TEXT, name TEXT)");
  db.execSQL("CREATE TABLE branches (ifsc TEXT PRIMARY KEY, micr TEXT, swift TEXT, bank_id TEXT, state_id TEXT, district_id TEXT, name TEXT, state_name TEXT, district_name TEXT, raw TEXT NOT NULL)");
  Map<String,JSONObject> banks=new HashMap<>();Map<String,Location> states=new HashMap<>(),districts=new HashMap<>();int[] stats=new int[5];
  db.beginTransaction();try {
   SQLiteStatement bankInsert=db.compileStatement("INSERT INTO banks VALUES (?,?,?,?)");try{each(new File(directory,"banks.json"),b->{String id=first(b,"bank_id","id"),name=first(b,"bank_name","name");if(id.isEmpty()||name.isEmpty())throw new IOException("Bank ID/name missing");b.put("bank_id",id);b.put("bank_image",imagePath(first(b,"bank_image","logo_url")));banks.put(id,b);bind(bankInsert,id,name,clean(b,"bank_image"),b.toString());bankInsert.executeInsert();});}finally{bankInsert.close();}
   SQLiteStatement stateInsert=db.compileStatement("INSERT INTO states VALUES (?,?,?)");try{each(new File(directory,"state.json"),s->{String id=first(s,"state_id","id"),bank=first(s,"bank_id","Bank_id"),name=first(s,"state_name","state");if(id.isEmpty()||!banks.containsKey(bank)||name.isEmpty()){stats[2]++;return;}states.put(id,new Location(id,bank,"",name));bind(stateInsert,id,bank,name);stateInsert.executeInsert();});}finally{stateInsert.close();}
   SQLiteStatement districtInsert=db.compileStatement("INSERT INTO districts VALUES (?,?,?,?)");try{each(new File(directory,"district.json"),d->{String id=first(d,"district_id","id"),bank=first(d,"bank_id","Bank_id"),state=first(d,"state_id","State_id"),name=first(d,"district_name","district");Location parent=states.get(state);if(id.isEmpty()||parent==null||!parent.bank.equals(bank)||name.isEmpty()){stats[3]++;return;}districts.put(id,new Location(id,bank,state,name));bind(districtInsert,id,bank,state,name);districtInsert.executeInsert();});}finally{districtInsert.close();}
   SQLiteStatement branchInsert=db.compileStatement("INSERT INTO branches VALUES (?,?,?,?,?,?,?,?,?,?)");try{each(new File(directory,"branch.json"),b->{stats[0]++;String code=first(b,"ifsc","ifsc_code").toUpperCase(Locale.ROOT),bankId=clean(b,"bank_id"),stateId=clean(b,"state_id"),districtId=clean(b,"district_id");JSONObject bank=banks.get(bankId);String name=first(b,"branch_name","branch"),address=clean(b,"address");if(!code.matches("[A-Z]{4}0[A-Z0-9]{6}")||bank==null||name.isEmpty()){stats[1]++;return;}
    Location district=districts.get(districtId);if(stateId.isEmpty()&&district!=null)stateId=district.state;Location state=states.get(stateId);boolean validState=state!=null&&state.bank.equals(bankId);boolean validDistrict=validState&&district!=null&&district.bank.equals(bankId)&&district.state.equals(stateId);
    String stateName=validState?state.name:UNKNOWN,districtName=validDistrict?district.name:UNKNOWN;if(!validState||!validDistrict)stats[4]++;
    String micr=clean(b,"micr");if(!micr.matches("[0-9]{9}"))micr="";b.put("ifsc",code);b.put("bank_name",clean(bank,"bank_name"));b.put("bank_image",clean(bank,"bank_image"));b.put("branch_name",name);b.put("state",validState?state.name:"");b.put("district",validDistrict?district.name:"");b.put("contact",first(b,"contact","phone"));b.put("bank_contact",first(bank,"contact_number","contact","phone"));b.put("bank_email",first(bank,"email"));b.put("bank_website",first(bank,"website","bank_url","url"));b.put("bank_type",clean(bank,"bank_type"));b.put("branch_status",first(b,"branch_status","is_active"));for(String service:new String[]{"neft","rtgs","imps","upi"})b.put(service,first(b,service,service.toUpperCase(Locale.ROOT)).isEmpty()?first(bank,service,service.toUpperCase(Locale.ROOT)):first(b,service,service.toUpperCase(Locale.ROOT)));b.put("micr",micr);b.put("branch_code",code.substring(5));b.put("address",address);
    String swift=first(b,"swift","swift_code","SWIFT","SWIFT_CODE","bic","BIC").toUpperCase(Locale.ROOT);if(!swift.matches("[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?"))swift="";b.put("swift",swift);
    bind(branchInsert,code,micr,swift,bankId,validState?stateId:"",validDistrict?districtId:"",name,stateName,districtName,b.toString());try{branchInsert.executeInsert();}catch(SQLiteConstraintException e){throw new IOException("Duplicate IFSC: "+code,e);}
   });}finally{branchInsert.close();}
   if(count(db,"banks")==0||count(db,"branches")==0)throw new IOException("No usable banks/branches");
   db.execSQL("CREATE INDEX branch_location ON branches (bank_id,state_name COLLATE NOCASE,district_name COLLATE NOCASE,name COLLATE NOCASE)");db.execSQL("CREATE INDEX branch_swift ON branches (swift COLLATE NOCASE)");db.execSQL("CREATE INDEX branch_micr ON branches (micr COLLATE NOCASE)");db.execSQL("CREATE INDEX branch_ifsc_prefix ON branches (ifsc COLLATE NOCASE)");db.execSQL("CREATE INDEX state_bank ON states(bank_id)");db.execSQL("CREATE INDEX district_parent ON districts(bank_id,state_id)");db.setTransactionSuccessful();
  }finally{db.endTransaction();}
  JSONObject meta=new JSONObject();meta.put("schema_version",4);meta.put("downloaded_records",stats[0]);meta.put("skipped_records",stats[1]);meta.put("skipped_states",stats[2]);meta.put("skipped_districts",stats[3]);meta.put("missing_locations",stats[4]);meta.put("bank_count",count(db,"banks"));meta.put("state_count",count(db,"states"));meta.put("district_count",count(db,"districts"));meta.put("branch_count",count(db,"branches"));return meta;
 }
 private static void bind(SQLiteStatement statement,String... values){statement.clearBindings();for(int i=0;i<values.length;i++)statement.bindString(i+1,values[i]==null?"":values[i]);}
 public static String read(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Response too large");out.write(buffer,0,n);}return out.toString("UTF-8");}
 public static String download(String url,int limit)throws Exception{HttpURLConnection c=connection(url);try{int code=c.getResponseCode();if(code!=200)throw new HttpError(code);try(InputStream in=c.getInputStream()){return read(in,limit);}}finally{c.disconnect();}}
 private static HttpURLConnection connection(String url)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(30000);c.setUseCaches(false);c.setRequestProperty("User-Agent","Bank-Pie");return c;}
 private static void downloadFile(String url,File file,long limit,Progress progress)throws Exception{HttpURLConnection c=connection(url);try{int code=c.getResponseCode();if(code!=200)throw new HttpError(code);long total=0;try(InputStream in=c.getInputStream();OutputStream out=new BufferedOutputStream(new FileOutputStream(file))){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();total+=n;if(total>limit)throw new IOException("JSON file exceeds size limit");out.write(buffer,0,n);if(total%(512*1024)<n)progress.update("Downloading "+file.getName()+" · "+(total/(1024*1024))+" MB");}}}finally{c.disconnect();}}
 public static void delete(File file){if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File child:children)delete(child);}file.delete();}
}
