package com.kuaishou.kcode;

import java.util.Objects;

class WarningItem {
    private String caller;
    private String responder;
    private String time;
    private String type;

    WarningItem(String caller, String responder, String time, String type) {
        this.caller = caller;
        this.responder = responder;
        this.time = time;
        this.type = type;
    }

    public String getCaller() {
        return caller;
    }

    public String getResponder() {
        return responder;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WarningItem that = (WarningItem) o;

        if (caller != null ? !caller.equals(that.caller) : that.caller != null) return false;
        if (responder != null ? !responder.equals(that.responder) : that.responder != null) return false;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caller, responder, time, type);
    }

    @Override
    public String toString() {
        return caller + responder + time + type;
    }
}
