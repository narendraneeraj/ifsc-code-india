import com.bhuvancom.bank_pie.Search;
import java.util.*;
public class SearchTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  List<String> a=new ArrayList<>();for(int i=0;i<12;i++)a.add("SBIN000"+String.format("%04d",i));
  check(Search.find(a,"sbin",false,x->x).size()==8);
  check(Search.find(a," SBIN0000010 ",false,x->x).equals(Arrays.asList("SBIN0000010")));
  check(Search.find(a,"HDFC",false,x->x).isEmpty());
  check(Search.find(Arrays.asList("001234567","001111111","400002001"),"001",true,x->x).size()==2);
  check(!Search.validQuery("12",true));check(!Search.validQuery("123ABC",true));
  check(Search.find(Arrays.asList("SBIN001","SBIN"),"SBIN",false,x->x).get(0).equals("SBIN"));
  System.out.println("7 search checks passed");
 }
}
