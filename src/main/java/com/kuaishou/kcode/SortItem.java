package com.kuaishou.kcode;

public class SortItem implements RuleItem{
    private int minuteTime;
    private String date;
    private int warning;
    private boolean success;
    private int startMinuteTime;

    public SortItem(int minuteTime, String date, int warning) {
        this.minuteTime = minuteTime;
        this.date = date;
        this.warning = warning;
        success = false;
        startMinuteTime = minuteTime;
    }

    public int getMinuteTime() {
        return minuteTime;
    }

    public void setMinuteTime(int minuteTime) {
        this.minuteTime = minuteTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public double getDoubleWarning() {
        return 0;
    }

    @Override
    public int getIntWarning() {
        return warning;
    }

    @Override
    public void setDoubleWarning(double warning) {

    }

    @Override
    public void setIntWarning(int warning) {
        this.warning = warning;
    }

    @Override
    public int getStartMinuteTime() {
        return startMinuteTime;
    }

    @Override
    public boolean getSuccess() {
        return success;
    }

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

}
