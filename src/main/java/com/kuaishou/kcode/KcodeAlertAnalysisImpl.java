package com.kuaishou.kcode;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author KCODE
 *         Created on 2020-07-04
 */
public class KcodeAlertAnalysisImpl implements KcodeAlertAnalysis {
    private static NumberFormat nt = NumberFormat.getPercentInstance();

    static {
        nt.setMinimumFractionDigits(2);
    }

    private SimpleDateFormat sdf;
    //规则集合
    private List<RuleDict> ruleList;

    //搜索结构
    private Map<String, Set<String>> neighbor;
    private Map<String, Set<String>> reverseNeighbor;
    private Map<String, AggregationItem> longestPathMap;

    //调用链
    private Map<String, List<List<String>>> forwardMap;
    private Map<String, List<List<String>>> reverseMap;
    private MinuteData[] minuteDataArr;
    private int minuteDataSize = 16;

    //报警点
    private Set<WarningItem> warningPoint;
    private Map<String, Set<String>> resMap;

    public KcodeAlertAnalysisImpl() {
        String format = "yyyy-MM-dd HH:mm";
        sdf = new SimpleDateFormat(format);
        ruleList = new ArrayList<>();
        neighbor = new HashMap<>();
        reverseNeighbor = new HashMap<>();
        longestPathMap = new HashMap<>();
        forwardMap = new HashMap<>();
        reverseMap = new HashMap<>();
        minuteDataArr = new MinuteData[minuteDataSize];
        warningPoint = new HashSet<>();
        resMap = new HashMap<>();
    }

    private void collectionMinuteData(MinuteData minuteData){
        String date = minuteData.getDate();
        Map<DataItem, CallerItem> dataMap = minuteData.getMinuteMap();
        Map<String, CallerItem> tmpMap = new HashMap<>();
        for(Map.Entry<DataItem, CallerItem> dataEntry : dataMap.entrySet()){
            DataItem dataItem = dataEntry.getKey();
            CallerItem callerItem = dataEntry.getValue();
            CallerItem tmpItem =
                    tmpMap.computeIfAbsent(dataItem.getCaller() + dataItem.getResponder() + date, k -> new CallerItem());
            tmpItem.add(callerItem);
            Set<String> tmp = neighbor.computeIfAbsent(dataItem.getCaller(), k -> new HashSet<>());
            tmp.add(dataItem.getResponder());
            tmp = reverseNeighbor.computeIfAbsent(dataItem.getResponder(), k -> new HashSet<>());
            tmp.add(dataItem.getCaller());
        }
        tmpMap.forEach((k, v) -> {
            v.calculate();
            longestPathMap.put(k, new AggregationItem(nt.format(v.getRate()), v.getP99()));
        });
        tmpMap.clear();
    }

    private void calculateMinuteData(Set<String> resSet, MinuteData minuteData){
        int minute = minuteData.getMinute();
        String date = minuteData.getDate();
        Map<DataItem, CallerItem> dataMap = minuteData.getMinuteMap();
        for(Map.Entry<DataItem, CallerItem> dataEntry : dataMap.entrySet()){
            DataItem dataItem = dataEntry.getKey();
            CallerItem callerItem = dataEntry.getValue();
            callerItem.calculate();
            for(RuleDict rule : ruleList){
                String ruleCaller = rule.getCaller();
                String ruleResponder = rule.getResponder();
                if(ruleCaller.equals("ALL") && ruleResponder.equals(dataItem.getResponder())){
                    rule.compare(callerItem, decodeIp(dataItem.getIpAggregation()),
                            minute, date, resSet, dataItem.getCaller(), ruleResponder, warningPoint);
                }else if(ruleCaller.equals(dataItem.getCaller()) && ruleResponder.equals("ALL")){
                    rule.compare(callerItem, decodeIp(dataItem.getIpAggregation()),
                            minute, date, resSet, ruleCaller, dataItem.getResponder(), warningPoint);
                }else if(ruleCaller.equals(dataItem.getCaller()) && ruleResponder.equals(dataItem.getResponder())){
                    rule.compare(callerItem, decodeIp(dataItem.getIpAggregation()),
                            minute, date, resSet, ruleCaller, ruleResponder, warningPoint);
                }
            }
        }
        collectionMinuteData(minuteData);
    }

    private void calculateMinuteData(Set<String> resSet){
        for(MinuteData minuteData : minuteDataArr){
            if(minuteData == null)
                continue;
            calculateMinuteData(resSet, minuteData);
        }
    }

    private String decodeIp(long ipAggregation) {
        return (ipAggregation >>> 56) + "." + ((ipAggregation & 0xFFFFFFFFFFFFFFL) >>> 48) + "."
                + ((ipAggregation & 0xFFFFFFFFFFFFL) >>> 40) + "." + ((ipAggregation & 0xFFFFFFFFFFL) >>> 32) + "," +
                ((ipAggregation & 0xFFFFFFFFL) >>> 24) + "." + ((ipAggregation & 0xFFFFFF) >>> 16) + "."
                + ((ipAggregation & 0xFFFF) >>> 8) + "." + (ipAggregation & 0xFF);
    }

    @Override
    public Collection<String> alarmMonitor(String path, Collection<String> alertRules) throws IOException, ParseException {
        //解析规则
        for (String rule : alertRules) {
            String[] parts = rule.split(",");
            int id = Integer.parseInt(parts[0]);
            String caller = parts[1];
            String responder = parts[2];
            String type = parts[3];
            String detail = parts[4];
            int timeThresh = 0;
            for (int i = 0; detail.charAt(i) != '<' && detail.charAt(i) != '>'; ++i) {
                timeThresh = timeThresh * 10 + detail.charAt(i) - '0';
            }
            char compare = detail.charAt(detail.length() - 1);
            String thresh = parts[5];
            ruleList.add(new RuleDict(caller, responder, id, compare, type, timeThresh, thresh));
        }
        Set<String> resSet = new HashSet<>();
        //读取数据
        InputStream inputStream = new FileInputStream(path);

        int cache = 1024 * 256;
        byte[] byteArr = new byte[cache];
        byte[] preByte = new byte[200];
        byte[] s = new byte[cache + 200];
        int preLen = 0, len, pos;
        int startMinute = 0;
        while ((len = inputStream.read(byteArr)) != -1) {
            System.arraycopy(preByte, 0, s, 0, preLen);
            pos = len - 1;
            while (byteArr[pos] != 0x0A)
                --pos;
            System.arraycopy(byteArr, 0, s, preLen, ++pos);
            int end = pos + preLen;
            preLen = len - pos;
            System.arraycopy(byteArr, pos, preByte, 0, preLen);
            for (int i = 0; i < end; ) {
                int j = i;
                //caller
                while (s[j] != ',')
                    ++j;
                String caller = new String(s, i, j - i);
                //caller ip
                long callerIp = 0;
                for (j += 1; s[j] != ','; ++j) {
                    int frame = 0;
                    for (; s[j] != '.' && s[j] != ','; ++j)
                        frame = frame * 10 + s[j] - '0';
                    callerIp = frame | (callerIp << 8L);
                    if (s[j] == ',')
                        break;
                }
                //responder
                for (j += 1, i = j; s[j] != ',';)
                    ++j;
                String responder = new String(s, i , j - i);
                //responder ip
                long responderIp = 0;
                for (j += 1; s[j] != ','; ++j) {
                    int frame = 0;
                    for (; s[j] != '.' && s[j] != ','; ++j)
                        frame = frame * 10 + s[j] - '0';
                    responderIp = frame | (responderIp << 8L);
                    if (s[j] == ',')
                        break;
                }
                //result
                ++j;
                boolean result = s[j] == 't';
                j += result ? 4 : 5;
                //cost
                int cost = 0;
                for (j += 1; s[j] != ','; ++j)
                    cost = cost * 10 + s[j] - '0';
                //timestamp
                long timestamp = 0;
                for (j += 1; s[j] != '\r' && s[j] != '\n'; ++j)
                    timestamp = timestamp * 10 + s[j] - '0';
                int minuteStamp = (int) (timestamp / 60000);
                i = j + 1;
                long ipAggregation = 0L;
                ipAggregation = (ipAggregation << 32) | callerIp;
                ipAggregation = (ipAggregation << 32) | responderIp;
                if(startMinute == 0){
                    startMinute = minuteStamp;
                }
                int idx = minuteStamp - startMinute;
                if(idx >= minuteDataSize){
                    while(minuteDataSize <= idx)
                        minuteDataSize *= 2;
                    minuteDataArr = Arrays.copyOf(minuteDataArr, minuteDataSize);
                }
                if(minuteDataArr[idx] == null){
                    minuteDataArr[idx] =
                            new MinuteData(minuteStamp, sdf.format(new Date(timestamp)));
                }
                MinuteData minuteData = minuteDataArr[idx];
                minuteData.addInfo(caller, responder, ipAggregation, result, cost);
            }
        }
        calculateMinuteData(resSet);
        calculatePath();
        assemble();
        return resSet;
    }

    private Set<String> getRes(String caller, String responder, String time, String type){
        boolean sr = false;
        if (type.equals("SR"))
            sr = true;
        List<List<String>> forwardList = forwardMap.get(responder);
        List<List<String>> reverseList = reverseMap.get(caller);
        Set<String> resSet = new HashSet<>();
        for (List<String> reversePath : reverseList) {
            StringBuilder reversePathBuilder = new StringBuilder();
            StringBuilder reverseResultBuilder = new StringBuilder();
            String preIdx = " ";
            for (String idx : reversePath) {
                reversePathBuilder.append(idx).append("->");
                if (!preIdx.equals(" ")) {
                    AggregationItem aggregationItem =
                            longestPathMap.get(preIdx + idx + time);
                    if (aggregationItem != null) {
                        reverseResultBuilder.append(sr ?
                                aggregationItem.getRate() : (aggregationItem.getP99() + "ms")).append(',');
                    }else{
                        reverseResultBuilder.append(sr ? "-1%" : "-1ms").append(',');
                    }
                }
                preIdx = idx;
            }
            String tmp = preIdx;
            for (List<String> path : forwardList) {
                preIdx = tmp;
                StringBuilder pathBuilder = new StringBuilder(reversePathBuilder);
                StringBuilder resultBuilder = new StringBuilder(reverseResultBuilder);
                for (String idx : path) {
                    pathBuilder.append(idx).append("->");
                    if (!preIdx.equals(" ")) {
                        AggregationItem aggregationItem =
                                longestPathMap.get(preIdx + idx + time);
                        if (aggregationItem != null) {
                            resultBuilder.append(sr ?
                                    aggregationItem.getRate() : (aggregationItem.getP99() + "ms")).append(',');
                        } else {
                            resultBuilder.append(sr ? "-1%" : "-1ms").append(',');
                        }
                    }
                    preIdx = idx;
                }
                pathBuilder.delete(pathBuilder.length() - 2, pathBuilder.length());
                resultBuilder.deleteCharAt(resultBuilder.length() - 1);
                pathBuilder.append('|').append(resultBuilder);
                resSet.add(pathBuilder.toString());
            }
        }
        return resSet;
    }

    private void assemble(){
        for(WarningItem warningItem : warningPoint){
            resMap.put(warningItem.toString(), getRes(warningItem.getCaller(),
                    warningItem.getResponder(), warningItem.getTime(), warningItem.getType()));
        }
    }

    private void calculatePath(){
        for(Map.Entry<String, Set<String>> neighborEntry : neighbor.entrySet()){
            String caller = neighborEntry.getKey();
            List<List<String>> reverseList = reverseMap.get(caller);
            if(reverseList == null) {
                int longest = findLongest(reverseNeighbor, caller, 0);
                reverseList = new ArrayList<>();
                collectLongest(reverseNeighbor, caller, new LinkedList<>(), reverseList, 0, longest, false);
                reverseMap.put(caller, reverseList);
            }
            for(String responder : neighborEntry.getValue()){
                List<List<String>> forwardList = forwardMap.get(responder);
                if(forwardList == null) {
                    int longest = findLongest(neighbor, responder, 0);
                    forwardList = new ArrayList<>();
                    collectLongest(neighbor, responder, new LinkedList<>(), forwardList, 0, longest, true);
                    forwardMap.put(responder, forwardList);
                }
            }
        }
    }

    private int findLongest(Map<String, Set<String>> neighborMap, String curIdx, int step) {
        step += 1;
        Set<String> neighbors = neighborMap.get(curIdx);
        if (neighbors == null) return step;
        int res = 0;
        for (String neighborIdx : neighbors) {
            res = Math.max(res, findLongest(neighborMap, neighborIdx, step));
        }
        return res;
    }

    private void collectLongest(Map<String, Set<String>> neighborMap, String curIdx, LinkedList<String> list,
                                List<List<String>> resList, int step, int target, boolean forward) {
        step += 1;
        if (forward) {
            list.addLast(curIdx);
        } else {
            list.addFirst(curIdx);
        }
        if (step == target) {
            resList.add(new ArrayList<>(list));
        }
        Set<String> neighbors = neighborMap.get(curIdx);
        if (neighbors != null) {
            for (String neighborIdx : neighbors) {
                collectLongest(neighborMap, neighborIdx, list, resList, step, target, forward);
            }
        }
        if (forward) {
            list.removeLast();
        } else {
            list.removeFirst();
        }
    }

    @Override
    public Collection<String> getLongestPath(String caller, String responder, String time, String type) {
        Set<String> resSet = resMap.get(caller + responder + time + type);
        return resSet == null ? getRes(caller, responder, time, type) : resSet;
    }
}