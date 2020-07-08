package com.kuaishou.kcode;

import java.util.Set;

public interface RuleType {
    boolean compare(CallerItem callerItem, String ipAggregation, int minuteTime, String date,
                    Set<String> resSet, String caller, String responder, Set<WarningItem> warningPoint);
}
