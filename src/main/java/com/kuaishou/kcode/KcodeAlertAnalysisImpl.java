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
    private static String ALL = "ALL";

    static {
        nt.setMinimumFractionDigits(2);
    }

    private SimpleDateFormat sdf;
    //规则集合
    private List<RuleDict> ruleList;
    //解析后的数据 caller -> responder -> minute -> ipAggregation -> data
    private Map<Integer, Map<Integer, Map<Integer, Map<Long, CallerItem>>>> map;
    //时间记录
    private Map<Integer, String> minute2date;
    private Map<String, Integer> date2minute;
    //hash->name
    private Map<Integer, String> hash2Service;

    //搜索结构
    private Map<Integer, Set<Integer>> neighbor;
    private Map<Integer, Set<Integer>> reverseNeighbor;
    private Map<String, AggregationItem> longestPathMap;

    public KcodeAlertAnalysisImpl() {
        String format = "yyyy-MM-dd HH:mm";
        sdf = new SimpleDateFormat(format);
        ruleList = new ArrayList<>();
        minute2date = new TreeMap<>();
        date2minute = new HashMap<>();
        hash2Service = new HashMap<>();
        map = new HashMap<>();
        neighbor = new HashMap<>();
        reverseNeighbor = new HashMap<>();
        longestPathMap = new HashMap<>();
    }

    private void calculateCollection(Set<String> resSet) {
        for (Map.Entry<Integer, String> timeEntry : minute2date.entrySet()) {
            int minuteTime = timeEntry.getKey();
            String date = minute2date.get(minuteTime);
            for (RuleDict rule : ruleList) {
                int callerHash = rule.getCaller();
                int responderHash = rule.getResponder();
                int ALL_HASH = -1;
                if (callerHash == ALL_HASH) {
                    for (Map.Entry<Integer, Map<Integer, Map<Integer, Map<Long, CallerItem>>>> callerEntry : map.entrySet()) {
                        Map<Integer, Map<Integer, Map<Long, CallerItem>>> responderMap = callerEntry.getValue();
                        Map<Integer, Map<Long, CallerItem>> minuteMap;
                        Map<Long, CallerItem> ipAggregationMap;
                        String caller = hash2Service.get(callerEntry.getKey());
                        String responder = hash2Service.get(responderHash);
                        if ((minuteMap = responderMap.get(responderHash)) != null
                                && (ipAggregationMap = minuteMap.get(minuteTime)) != null) {
                            for (Map.Entry<Long, CallerItem> ipAggregationEntry : ipAggregationMap.entrySet()) {
                                String ipAggregation = decodeIp(ipAggregationEntry.getKey());
                                CallerItem callerItem = ipAggregationEntry.getValue();
                                callerItem.calculate();
                                rule.compare(callerItem, ipAggregation,
                                        minuteTime, date, resSet, caller, responder);
                            }
                        }
                    }
                } else if (responderHash == ALL_HASH) {
                    Map<Integer, Map<Integer, Map<Long, CallerItem>>> responderMap = map.get(callerHash);
                    if (responderMap != null) {
                        String caller = hash2Service.get(callerHash);
                        for (Map.Entry<Integer, Map<Integer, Map<Long, CallerItem>>> responderEntry
                                : responderMap.entrySet()) {
                            String responder = hash2Service.get(responderEntry.getKey());
                            Map<Integer, Map<Long, CallerItem>> minuteMap = responderEntry.getValue();
                            Map<Long, CallerItem> ipAggregationMap = minuteMap.get(minuteTime);
                            if (ipAggregationMap != null) {
                                for (Map.Entry<Long, CallerItem> ipAggregationEntry : ipAggregationMap.entrySet()) {
                                    String ipAggregation = decodeIp(ipAggregationEntry.getKey());
                                    CallerItem callerItem = ipAggregationEntry.getValue();
                                    callerItem.calculate();
                                    rule.compare(callerItem, ipAggregation,
                                            minuteTime, date, resSet, caller, responder);
                                }
                            }
                        }
                    }
                } else {
                    Map<Integer, Map<Integer, Map<Long, CallerItem>>> responderMap = map.get(callerHash);
                    if (responderMap != null) {
                        String caller = hash2Service.get(callerHash);
                        Map<Integer, Map<Long, CallerItem>> minuteMap = responderMap.get(responderHash);
                        if (minuteMap != null) {
                            String responder = hash2Service.get(responderHash);
                            Map<Long, CallerItem> ipAggregationMap = minuteMap.get(minuteTime);
                            if (ipAggregationMap != null) {
                                for (Map.Entry<Long, CallerItem> ipAggregationEntry : ipAggregationMap.entrySet()) {
                                    String ipAggregation = decodeIp(ipAggregationEntry.getKey());
                                    CallerItem callerItem = ipAggregationEntry.getValue();
                                    callerItem.calculate();
                                    rule.compare(callerItem, ipAggregation,
                                            minuteTime, date, resSet, caller, responder);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void initConstruct() {
        for (Map.Entry<Integer, Map<Integer, Map<Integer, Map<Long, CallerItem>>>> callerEntry : map.entrySet()) {
            String caller = hash2Service.get(callerEntry.getKey());
            for (Map.Entry<Integer, Map<Integer, Map<Long, CallerItem>>> responderEntry : callerEntry.getValue().entrySet()) {
                String responder = hash2Service.get(responderEntry.getKey());
                for (Map.Entry<Integer, Map<Long, CallerItem>> minuteEntry : responderEntry.getValue().entrySet()) {
                    CallerItem tmpItem = new CallerItem();
                    for (Map.Entry<Long, CallerItem> ipAggregationEntry : minuteEntry.getValue().entrySet()) {
                        CallerItem callerItem = ipAggregationEntry.getValue();
                        tmpItem.add(callerItem);
                    }
                    tmpItem.calculate();
                    longestPathMap.put(caller + responder + minute2date.get(minuteEntry.getKey()),
                            new AggregationItem(nt.format(tmpItem.getRate()), tmpItem.getP99()));
                }
                //构建图
                Set<Integer> tmpSet = neighbor.computeIfAbsent(callerEntry.getKey(), k -> new HashSet<>());
                tmpSet.add(responderEntry.getKey());
                tmpSet = reverseNeighbor.computeIfAbsent(responderEntry.getKey(), k -> new HashSet<>());
                tmpSet.add(callerEntry.getKey());
            }
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
        byte[] preByte = new byte[128];
        byte[] s = new byte[cache + 128];
        int preLen = 0, len, pos;
        while ((len = inputStream.read(byteArr)) != -1) {
            System.arraycopy(preByte, 0, s, 0, preLen);
            for (pos = len - 1; byteArr[pos] != 0x0A; --pos) ;
            System.arraycopy(byteArr, 0, s, preLen, ++pos);
            int end = pos + preLen;
            preLen = len - pos;
            System.arraycopy(byteArr, pos, preByte, 0, preLen);
            for (int i = 0; i < end; ) {
                int j = i;
                int caller = 0;
                //caller
                for (; s[j] != ','; ++j)
                    caller = caller * 31 + s[j];
                if (!hash2Service.containsKey(caller)) {
                    hash2Service.put(caller, new String(s, i, j - i));
                }
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
                int responder = 0;
                for (j += 1, i = j; s[j] != ','; ++j)
                    responder = responder * 31 + s[j];
                if (!hash2Service.containsKey(responder)) {
                    hash2Service.put(responder, new String(s, i, j - i));
                }
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
                for (j += 1; s[j] != 0x0A; ++j)
                    timestamp = timestamp * 10 + s[j] - '0';
                int minuteStamp = (int) (timestamp / 60000);
                i = j + 1;
                long ipAggregation = 0L;
                ipAggregation = (ipAggregation << 32) | callerIp;
                ipAggregation = (ipAggregation << 32) | responderIp;
                //放入中间数据
                Map<Integer, Map<Integer, Map<Long, CallerItem>>> responderMap =
                        map.computeIfAbsent(caller, k -> new HashMap<>());
                Map<Integer, Map<Long, CallerItem>> minuteMap =
                        responderMap.computeIfAbsent(responder, k -> new HashMap<>());
                Map<Long, CallerItem> ipAggregationMap =
                        minuteMap.computeIfAbsent(minuteStamp, k -> new HashMap<>());
                CallerItem callerItem = ipAggregationMap.computeIfAbsent(ipAggregation, k -> new CallerItem());
                callerItem.add(cost, result);
                //放入时间转换
                if (!minute2date.containsKey(minuteStamp)) {
                    String date = sdf.format(new Date(timestamp));
                    minute2date.put(minuteStamp, date);
                    date2minute.put(date, minuteStamp);
                }
            }
        }
        calculateCollection(resSet);
        initConstruct();
        return resSet;
    }

    private int findLongest(Map<Integer, Set<Integer>> neighborMap, int curIdx, int step) {
        step += 1;
        Set<Integer> neighbors = neighborMap.get(curIdx);
        if (neighbors == null) return step;
        int res = 0;
        for (int neighborIdx : neighbors) {
            res = Math.max(res, findLongest(neighborMap, neighborIdx, step));
        }
        return res;
    }

    private void collectLongest(Map<Integer, Set<Integer>> neighborMap, int curIdx, LinkedList<Integer> list,
                                List<List<Integer>> resList, int step, int target, boolean forward) {
        step += 1;
        if (forward) {
            list.addLast(curIdx);
        } else {
            list.addFirst(curIdx);
        }
        if (step == target) {
            resList.add(new ArrayList<>(list));
        }
        Set<Integer> neighbors = neighborMap.get(curIdx);
        if (neighbors != null) {
            for (int neighborIdx : neighbors) {
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
        boolean sr = false;
        if (type.equals("SR"))
            sr = true;
        int longest = findLongest(neighbor, responder.hashCode(), 0);
        List<List<Integer>> forwardList = new ArrayList<>();
        collectLongest(neighbor, responder.hashCode(), new LinkedList<>(), forwardList, 0, longest, true);
        longest = findLongest(reverseNeighbor, caller.hashCode(), 0);
        List<List<Integer>> reverseList = new ArrayList<>();
        collectLongest(reverseNeighbor, caller.hashCode(), new LinkedList<>(), reverseList, 0, longest, false);
        Set<String> resSet = new HashSet<>();
        for (List<Integer> reversePath : reverseList) {
            StringBuilder reversePathBuilder = new StringBuilder();
            StringBuilder reverseResultBuilder = new StringBuilder();
            int preIdx = -1;
            for (int idx : reversePath) {
                reversePathBuilder.append(hash2Service.get(idx)).append("->");
                if (preIdx != -1) {
                    AggregationItem aggregationItem =
                            longestPathMap.get(hash2Service.get(preIdx) + hash2Service.get(idx) + time);
                    if (aggregationItem != null) {
                        reverseResultBuilder.append(sr ?
                                aggregationItem.getRate() : (aggregationItem.getP99() + "ms")).append(',');
                    }else{
                        reverseResultBuilder.append(sr ? "-1%" : "-1ms").append(',');
                    }
                }
                preIdx = idx;
            }
            int tmp = preIdx;
            StringBuilder pathBuilder = new StringBuilder(reversePathBuilder);
            StringBuilder resultBuilder = new StringBuilder(reverseResultBuilder);
            for (List<Integer> path : forwardList) {
                preIdx = tmp;
                for (int idx : path) {
                    pathBuilder.append(hash2Service.get(idx)).append("->");
                    if (preIdx != -1) {
                        AggregationItem aggregationItem =
                                longestPathMap.get(hash2Service.get(preIdx) + hash2Service.get(idx) + time);
                        if (aggregationItem != null) {
                            resultBuilder.append(sr ?
                                    aggregationItem.getRate() : (aggregationItem.getP99() + "ms")).append(',');
                        } else {
                            resultBuilder.append(sr ? "-1%" : "-1ms").append(',');
                        }
                    }
                    preIdx = idx;
                }
            }
            pathBuilder.delete(pathBuilder.length() - 2, pathBuilder.length());
            resultBuilder.deleteCharAt(resultBuilder.length() - 1);
            pathBuilder.append('|').append(resultBuilder);
            resSet.add(pathBuilder.toString());
        }
        return resSet;
    }
}