package com.kuaishou.kcode;

import java.util.Arrays;

public class CallerItem {
    private int callerTimes;
    private int successTimes;
    private int[] values;
    private int min, max;
    private int size, length;
    private double rate;
    private int p99;
    private boolean calculated;

    CallerItem(){
        callerTimes = 0;
        successTimes = 0;
        length = 0;
        size = 128;
        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
        values = new int[size];
        calculated = false;
    }

    private void add(int cost){
        if(length == size){
            size += (size >> 1);
            values = Arrays.copyOf(values, size);
        }
        if (max < cost) max = cost;
        if (min > cost) min = cost;
        values[length++] = cost;
    }

    void add(int cost, boolean result){
        callerTimes += 1;
        if (result)
            successTimes += 1;
        add(cost);
    }

    void add(CallerItem callerItem){
        max = Math.max(callerItem.max, max);
        min = Math.min(callerItem.min, min);
        if(length + callerItem.length >= size){
            size = length + callerItem.length + (size >> 1);
            values = Arrays.copyOf(values, size);
        }
        System.arraycopy(callerItem.values, 0, values, length, callerItem.length);
        length += callerItem.length;
        successTimes += callerItem.successTimes;
        callerTimes += callerItem.callerTimes;
    }

    private void calculateRate(){
        rate = (double)successTimes / (double)callerTimes;
    }

    private void calculateP99(){
        int key = (int) Math.ceil((double) length * 0.99) - 1;
        int tmpLen = max - min + 1;
        int[] tmp = new int[tmpLen];
        for (int i = 0; i < length; ++i)
            tmp[values[i] - min] += 1;
        int ans = 0, count = length;
        for (int i = tmpLen - 1; i >= 0; --i) {
            count -= tmp[i];
            if (count <= key) {
                ans = min + i;
                break;
            }
        }
        p99 = ans;
    }

    void calculate(){
        if(!calculated) {
            calculateRate();
            calculateP99();
            calculated = true;
        }
    }

    double getRate() {
        return rate;
    }

    int getP99() {
        return p99;
    }
}
