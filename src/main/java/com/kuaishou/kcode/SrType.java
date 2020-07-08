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
                           Set<String> resSet, String caller, String responder, Set<WarningItem> warningPoint) {
        if (compare == '<' && callerItem.getRate() >= thresh || compare == '>' && callerItem.getRate() <= thresh) {
            return false;
        }
        RuleItem ruleItem = ruleItemMap.computeIfAbsent(ipAggregation,
                k -> new RuleItem(minuteTime));
        //判断时间是否连续
        if(ruleItem.getMinuteTime() != minuteTime && ruleItem.getMinuteTime() + 1 != minuteTime){
            ruleItem = new RuleItem(minuteTime);
            ruleItemMap.put(ipAggregation, ruleItem);
        }
        //判断时间是否再阈值内
        if(minuteTime >= ruleItem.getStartMinuteTime() + timeThresh - 1) {
            String[] ips = ipAggregation.split(",");
            String ans = id + "," + date + "," + caller + "," + ips[0] + "," +
                    responder + "," + ips[1] + "," + nt.format(callerItem.getRate());
            resSet.add(ans);
            warningPoint.add(new WarningItem(caller, responder, date, "SR"));
        }
        ruleItem.setMinuteTime(minuteTime);

        return true;
    }
}
