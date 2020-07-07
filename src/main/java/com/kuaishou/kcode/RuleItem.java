package com.kuaishou.kcode;

public interface RuleItem {
    int getMinuteTime();

    void setMinuteTime(int minuteTime);

    String getDate();

    void setDate(String date);

    double getDoubleWarning();

    int getIntWarning();

    void setDoubleWarning(double warning);

    void setIntWarning(int warning);

    int getStartMinuteTime();

    boolean getSuccess();

    void setSuccess(boolean success);
}
