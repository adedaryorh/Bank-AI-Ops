package com.bankops.ai;
import java.util.*;
import java.util.regex.Pattern;
public final class DeterministicExtraction {
    private DeterministicExtraction() {}
    public static Map<String,String> extract(String text) {
        Map<String,String> fields=new LinkedHashMap<>();
        for(String key:List.of("reference","applicant","amount","currency")) {
            var matcher=Pattern.compile("(?im)^"+key+"[ \t]*:[ \t]*([^\r\n]{1,180})$").matcher(text);
            if(matcher.find()) fields.put(key,matcher.group(1).trim());
        }
        if(fields.containsKey("reference") && !fields.get("reference").matches("SYN-[A-Za-z0-9-]+")) fields.remove("reference");
        if(fields.containsKey("amount") && !fields.get("amount").matches("[0-9]{1,12}(\\.[0-9]{1,2})?")) fields.remove("amount");
        if(fields.containsKey("currency") && !fields.get("currency").matches("EUR|USD|GBP|NGN")) fields.remove("currency");
        return Map.copyOf(fields);
    }
    public static List<String> chunks(String text) {
        List<String> result=new ArrayList<>();
        for(int start=0;start<text.length();start+=1000) result.add(text.substring(start,Math.min(text.length(),start+1000)));
        return List.copyOf(result);
    }
}
