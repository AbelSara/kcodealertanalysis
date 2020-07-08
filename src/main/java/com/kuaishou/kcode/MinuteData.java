package com.kuaishou.kcode;

import java.util.HashMap;
import java.util.Map;

class MinuteData {
    private int minute;
    private String date;
    private Map<DataItem, CallerItem> minuteMap;

    MinuteData(int minute, String date){
        this.minute = minute;
        this.date = date;
        minuteMap = new HashMap<>();
    }

    int getMinute() {
        return minute;
    }

    String getDate() {
        return date;
    }

    Map<DataItem, CallerItem> getMinuteMap() {
        return minuteMap;
    }

    void addInfo(String caller, String responder, Long ipAggregation, boolean result, int cost) {
        CallerItem callerItem =
                minuteMap.computeIfAbsent(new DataItem(caller, responder, ipAggregation), k -> new CallerItem());
        callerItem.add(cost, result);
    }

    void clear() {
        minuteMap.clear();
    }
}
