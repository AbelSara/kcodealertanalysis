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
//        if (id == 1 && date.equals("2020-07-02 19:25") && caller.equals("rd_6089216845914989634")
//                && responder.equals("rd_9563486881659901967") && ipAggregation.equals("10.168.13.100|10.190.81.50")) {
//            System.out.println(id + " " + callerItem.getP99());
//        }
        if (compare == '<' && callerItem.getP99() >= thresh) {
            return false;
        } else if (compare == '>' && callerItem.getP99() <= thresh) {
            return false;
        }

        RuleItem ruleItem = ruleItemMap.computeIfAbsent(ipAggregation,
                k -> new SrItem(minuteTime, date, callerItem.getP99()));
        if (ruleItem.getMinuteTime() != minuteTime && ruleItem.getMinuteTime() + 1 != minuteTime) {
            return false;
        }
        if (minuteTime >= ruleItem.getStartMinuteTime() + timeThresh - 1) {
            ruleItem.setSuccess(true);
            String[] ips = ipAggregation.split("\\|");
            String ans = id + "," + date + "," + caller + "," + ips[0] + "," +
                    responder + "," + ips[1] + "," + callerItem.getP99() + "ms";
//            if (id == 1 && date.equals("2020-07-02 19:25") && caller.equals("rd_6089216845914989634")
//                    && responder.equals("rd_9563486881659901967") && ipAggregation.equals("10.168.13.100|10.190.81.50")) {
//                System.out.println(ans);
//            }
            resSet.add(ans);
        } else {
            ruleItem = new SrItem(minuteTime, date, callerItem.getP99());
            ruleItemMap.put(ipAggregation, ruleItem);
        }
        ruleItem.setDoubleWarning(callerItem.getP99());
        ruleItem.setDate(date);
        ruleItem.setMinuteTime(minuteTime);

        return true;
    }
}
