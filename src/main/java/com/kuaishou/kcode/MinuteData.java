package com.kuaishou.kcode;

import java.util.HashMap;
import java.util.Map;

public class MinuteData {
    private int minuteTime;
    private String date;
    private Map<String, CallerItem> callerMap;

    MinuteData(int minuteTime){
        this.minuteTime = minuteTime;
        this.callerMap = new HashMap<>();
    }

    MinuteData(){
        this.minuteTime = Integer.MAX_VALUE;
        this.callerMap = new HashMap<>();
    }

    int getMinuteTime() {
        return minuteTime;
    }

    void setMinuteTime(int minuteTime) {
        this.minuteTime = minuteTime;
    }

    void add(String aggregation, int cost, boolean result){
        CallerItem callerItem = callerMap.computeIfAbsent(aggregation,
                k -> new CallerItem());
        callerItem.add(cost, result);
    }

    Map<String, CallerItem> getCallerMap() {
        return callerMap;
    }

    void setCallerMap(Map<String, CallerItem> callerMap) {
        this.callerMap = callerMap;
    }

    void setDate(String date){
        this.date = date;
    }

    String getDate(){
        return date;
    }
}
