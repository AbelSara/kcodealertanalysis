package com.kuaishou.kcode;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SrType implements RuleType {
    private static NumberFormat nt = NumberFormat.getPercentInstance();
    static {
        nt.setMinimumFractionDigits(2);
    }
    private double thresh;
    private char compare;
    private int timeThresh;
    private int id;
    //ip聚合
    private Map<String, RuleItem> ruleItemMap;

    SrType(double thresh, char compare, int timeThresh, int id) {
        this.thresh = thresh;
        this.compare = compare;
        this.timeThresh = timeThresh;
        ruleItemMap = new HashMap<>();
        this.id = id;
    }

    @Override
    public boolean compare(CallerItem callerItem, String ipAggregation, int minuteTime, String date,
                           Set<String> resSet, String caller, String responder) {
        if (compare == '<' && callerItem.getRate() >= thresh) {
            return false;
        } else if (compare == '>' && callerItem.getRate() <= thresh) {
            return false;
        }
        RuleItem ruleItem = ruleItemMap.computeIfAbsent(ipAggregation,
                k -> new SrItem(minuteTime, date, callerItem.getRate()));
        //判断时间是否连续
        if(ruleItem.getMinuteTime() != minuteTime && ruleItem.getMinuteTime() + 1 != minuteTime){
            ruleItem = new SrItem(minuteTime, date, callerItem.getRate());
            ruleItemMap.put(ipAggregation, ruleItem);
        }
        //判断时间是否再阈值内
        if(minuteTime >= ruleItem.getStartMinuteTime() + timeThresh - 1) {
            ruleItem.setSuccess(true);
            String[] ips = ipAggregation.split("\\|");
            String ans = id + "," + date + "," + caller + "," + ips[0] + "," +
                    responder + "," + ips[1] + "," + nt.format(callerItem.getRate());
            resSet.add(ans);
        }else{
            ruleItem = new SrItem(minuteTime, date, callerItem.getRate());
            ruleItemMap.put(ipAggregation, ruleItem);
        }
        ruleItem.setDoubleWarning(callerItem.getRate());
        ruleItem.setDate(date);
        ruleItem.setMinuteTime(minuteTime);

        return true;
    }
}
