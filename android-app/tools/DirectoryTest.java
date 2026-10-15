import org.easycalculator.ifsc.*;
import org.json.*;
import java.nio.file.*;
public class DirectoryTest {
 static int checks=0;
 static void check(boolean value){if(!value)throw new AssertionError("Check "+checks+" failed");checks++;}
 public static void main(String[] args)throws Exception {
  BranchRepository.Dataset demo=BranchRepository.parse(Files.readString(Path.of(args[0])));DirectoryIndex index=new DirectoryIndex(demo);
  check(index.banks.size()==8);check(index.banks.get("state bank of india").locations.size()==1);
  check(index.branches("state bank of india","maharashtra","mumbai").size()==1);
  check(index.branches("state bank of india","MAHARASHTRA","PUNE").get(0).getString("ifsc").equals("SBIN0005678"));
  check(index.branches("hdfc bank","MAHARASHTRA","MUMBAI").size()==1);
  check(index.branches("missing","MAHARASHTRA","MUMBAI").isEmpty());
  check(index.banks.get("axis bank").branches.isEmpty());
  check(BranchRepository.imagePath("hdfc.png").equals("images/hdfc.png"));check(BranchRepository.imagePath("../hdfc.png").isEmpty());
  JSONArray banks=new JSONArray("[{\"bank_id\":1,\"bank_name\":\"State Bank of India\",\"bank_image\":\"sbi.png\"}]");
  JSONArray states=new JSONArray("[{\"state_id\":101,\"bank_id\":1,\"state_name\":\"STATE A\"}]");
  JSONArray districts=new JSONArray("[{\"district_id\":201,\"state_id\":101,\"district_name\":\"DISTRICT A\"}]");
  JSONArray rows=new JSONArray("[{\"ifsc\":\"SBIN0000001\",\"district_id\":201,\"branch_name\":\"One\",\"address\":\"Address\",\"micr\":\"001234567\"},{\"ifsc\":\"SBIN0000002\",\"district_id\":999,\"branch_name\":\"Two\",\"address\":\"Address\"},{\"ifsc\":\"ICICI0002222\"}]");
  BranchRepository.Dataset legacy=BranchRepository.legacy(banks,states,districts,rows);DirectoryIndex legacyIndex=new DirectoryIndex(legacy);
  check(legacy.rows.size()==2);check(legacy.rows.get(0).getString("micr").equals("001234567"));
  check(legacyIndex.branches("state bank of india","STATE A","DISTRICT A").size()==1);
  check(legacyIndex.branches("state bank of india","Not provided","Not provided").size()==1);
  check(legacy.meta.getBoolean("sample_data"));check(legacy.meta.getInt("skipped_records")==1);
  JSONObject bad=new JSONObject(demo.meta.toString());bad.getJSONArray("branches").getJSONObject(0).put("micr","123");boolean rejected=false;try{BranchRepository.parse(bad.toString());}catch(Exception e){rejected=true;}check(rejected);
  bad=new JSONObject(demo.meta.toString());bad.getJSONArray("branches").put(bad.getJSONArray("branches").getJSONObject(0));rejected=false;try{BranchRepository.parse(bad.toString());}catch(Exception e){rejected=true;}check(rejected);
  check(BranchRepository.parse(demo.meta.toString()).banks.length()==8);
  System.out.println(checks+" directory checks passed");
 }
}
