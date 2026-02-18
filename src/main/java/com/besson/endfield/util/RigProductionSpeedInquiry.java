package com.besson.endfield.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RigProductionSpeedInquiry {
    private static Map<String ,Integer> process_inquiry_table;
    static {
        process_inquiry_table = new ConcurrentHashMap<String ,Integer>();
        process_inquiry_table.put("item.minecraft.coal",40);
        process_inquiry_table.put("item.minecraft.raw_copper",40);
        process_inquiry_table.put("item.minecraft.raw_gold",80);
        process_inquiry_table.put("item.minecraft.raw_iron",80);
        process_inquiry_table.put("item.minecraft.redstone",40);
        process_inquiry_table.put("item.minecraft.emerald",160);
        process_inquiry_table.put("item.minecraft.diamond",160);
        process_inquiry_table.put("item.minecraft.lapis_lazuli",40);
    }
    public static void  registerNewItem(String id ,Integer maxprocess){
        process_inquiry_table.put(id ,maxprocess);
    }
    public static int inquiryMaxProcess_normal(String id) {
        Optional<Integer> result = Optional.ofNullable(process_inquiry_table.get(id));
        if(result.isPresent()){
            return result.get();
        }else {
            return 40;
        }
    }
    public static int inquiryMaxProcess_highspeed(String id) {
        return Math.max(inquiryMaxProcess_normal(id) - 20 ,0);
    }
}
