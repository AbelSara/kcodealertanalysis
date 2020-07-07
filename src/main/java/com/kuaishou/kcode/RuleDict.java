package com.kuaishou.kcode;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Set;

class RuleDict {
    private int caller;
    private int responder;
    //详细的规则
    private RuleType ruleType;

    private static String ALL = "ALL";

    RuleDict(String caller, String responder,
             int id, char compare, String type, int timeThresh, String thresh) throws ParseException {
        this.caller = caller.equals(ALL) ? -1 : caller.hashCode();
        this.responder = responder.equals(ALL) ? -1 : responder.hashCode();
        if(type.equals("SR")){
            this.ruleType = new SrType(
                    NumberFormat.getInstance().parse(thresh).doubleValue() / 100, compare, timeThresh, id);
        }else{
            int val = 0;
            for(int i = 0; thresh.charAt(i) != 'm'; ++i){
                val = val * 10 + thresh.charAt(i) - '0';
            }
            this.ruleType = new SortType(val, compare, timeThresh, id);
        }
    }

    void compare(CallerItem callerItem, String ipAggregation, int minuteTime, String date,
                 Set<String> resSet, String caller, String responder){
        ruleType.compare(callerItem, ipAggregation, minuteTime, date, resSet, caller, responder);
    }

    public int getCaller() {
        return caller;
    }

    public int getResponder() {
        return responder;
    }
}
