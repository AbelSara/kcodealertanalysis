package com.kuaishou.kcode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SortType implements RuleType {
    private int thresh;
    private char compare;
    private int timeThresh;
    private int id;
    //ip聚合
    private Map<String, RuleItem> ruleItemMap;

    SortType(int thresh, char compare, int timeThresh, int id) {
        this.thresh = thresh;
        this.compare = compare;
        this.timeThresh = timeThresh;
        ruleItemMap = new HashMap<>();
        this.id = id;
    }

    @Override
    public boolean compare(CallerItem callerItem, String ipAggregation, int minuteTime, String date,
                           Set<String> resSet, String caller, String responder) {
        if (compare == '<' && callerItem.getP99() >= thresh) {
            return false;
        } else if (compare == '>' && callerItem.getP99() <= thresh) {
            return false;
        }

        RuleItem ruleItem = ruleItemMap.computeIfAbsent(ipAggregation,
                k -> new RuleItem(minuteTime));
        if (ruleItem.getMinuteTime() != minuteTime && ruleItem.getMinuteTime() + 1 != minuteTime) {
            ruleItem = new RuleItem(minuteTime);
            ruleItemMap.put(ipAggregation, ruleItem);
        }
        if (minuteTime >= ruleItem.getStartMinuteTime() + timeThresh - 1) {
            String[] ips = ipAggregation.split(",");
            String ans = id + "," + date + "," + caller + "," + ips[0] + "," +
                    responder + "," + ips[1] + "," + callerItem.getP99() + "ms";
            resSet.add(ans);
        } else if(minuteTime > ruleItem.getStartMinuteTime() + timeThresh - 1){
            ruleItem = new RuleItem(minuteTime);
            ruleItemMap.put(ipAggregation, ruleItem);
        }
        ruleItem.setMinuteTime(minuteTime);

        return true;
    }
}
