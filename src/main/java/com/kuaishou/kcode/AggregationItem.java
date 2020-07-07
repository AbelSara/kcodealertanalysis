package com.kuaishou.kcode;

public class AggregationItem {
    private String rate;
    private int p99;

    AggregationItem(String rate, int p99) {
        this.rate = rate;
        this.p99 = p99;
    }

    String getRate() {
        return rate;
    }

    int getP99() {
        return p99;
    }
}
