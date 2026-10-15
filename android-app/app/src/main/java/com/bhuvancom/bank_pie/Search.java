package com.bhuvancom.bank_pie;
import java.util.*;
public final class Search {
 public static String normalize(String value) { return value.trim().toUpperCase(Locale.ROOT); }
 public static boolean validQuery(String q, boolean micr) { return micr ? q.matches("[0-9]{3,9}") : q.matches("[A-Z0-9]{3,11}"); }
 public static <T> List<T> find(List<T> rows, String query, boolean micr, java.util.function.Function<T,String> key) {
  String q=normalize(query); List<T> exact=new ArrayList<>(), prefix=new ArrayList<>();
  if(!validQuery(q,micr)) return exact;
  for(T row:rows) { String k=normalize(key.apply(row)); if(k.equals(q)) exact.add(row); else if(k.startsWith(q)) prefix.add(row); }
  exact.addAll(prefix); return new ArrayList<>(exact.subList(0,Math.min(8,exact.size())));
 }
}
