package com.kuaishou.kcode;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Set;

class RuleDict {
    private String caller;
    private String responder;
    //详细的规则
    private RuleType ruleType;

    RuleDict(String caller, String responder,
             int id, char compare, String type, int timeThresh, String thresh) throws ParseException {
        this.caller = caller;
        this.responder = responder;
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

    String getCaller() {
        return caller;
    }

    String getResponder() {
        return responder;
    }
}
