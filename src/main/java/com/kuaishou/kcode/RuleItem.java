package com.kuaishou.kcode;

public class RuleItem {
    private int minuteTime;
    private int startMinuteTime;

    public RuleItem(int minuteTime) {
        this.minuteTime = minuteTime;
        startMinuteTime = minuteTime;
    }

    int getMinuteTime(){
        return minuteTime;
    }

    void setMinuteTime(int minuteTime){
        this.minuteTime = minuteTime;
    }

    int getStartMinuteTime(){
        return startMinuteTime;
    }

}
