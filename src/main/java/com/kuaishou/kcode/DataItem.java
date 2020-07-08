package com.kuaishou.kcode;

import java.util.Objects;

public class DataItem {
    private String caller;
    private String responder;
    private Long ipAggregation;

    DataItem(String caller, String responder, Long ipAggregation) {
        this.caller = caller;
        this.responder = responder;
        this.ipAggregation = ipAggregation;
    }

    String getCaller() {
        return caller;
    }

    String getResponder() {
        return responder;
    }

    Long getIpAggregation() {
        return ipAggregation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataItem dataItem = (DataItem) o;

        if (caller != null ? !caller.equals(dataItem.caller) : dataItem.caller != null) return false;
        if (responder != null ? !responder.equals(dataItem.responder) : dataItem.responder != null) return false;
        return ipAggregation != null ? ipAggregation.equals(dataItem.ipAggregation) : dataItem.ipAggregation == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caller, responder, ipAggregation);
    }
}
