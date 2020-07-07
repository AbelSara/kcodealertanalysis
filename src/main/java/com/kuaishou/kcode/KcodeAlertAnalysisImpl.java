package com.kuaishou.kcode;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author KCODE
 * Created on 2020-07-04
 */
public class KcodeAlertAnalysisImpl implements KcodeAlertAnalysis {
    private static String ALL = "ALL";

    private SimpleDateFormat sdf;
    Map<String, List<RuleDict>> ruleMap;

    public KcodeAlertAnalysisImpl(){
        String format = "yyyy-MM-dd HH:mm";
        sdf = new SimpleDateFormat(format);
        ruleMap = new HashMap<>();
    }

    void calculateCollection(MinuteData collection, Set<String> resSet){
        int minuteTime = collection.getMinuteTime();
        String date = collection.getDate();
        Map<String, CallerItem> callerMap = collection.getCallerMap();
        for(Map.Entry<String, CallerItem> entry : callerMap.entrySet()){
            String []str = entry.getKey().split(",");
            String caller = str[0];
            String responder = str[1];
            String ipAggregation = str[2];
            CallerItem callerItem = entry.getValue();
            callerItem.calculate();
            //匹配规则
            List<RuleDict> ruleDictList;
            String key = caller + ',' + responder;
            if((ruleDictList = ruleMap.get(key)) != null){
                for(RuleDict ruleDict : ruleDictList){
                    ruleDict.compare(callerItem, ipAggregation, minuteTime, date, resSet, caller, responder);
                }
            }
            key = ALL + "," + responder;
            if((ruleDictList = ruleMap.get(key)) != null){
                for(RuleDict ruleDict : ruleDictList){
                    ruleDict.compare(callerItem, ipAggregation,
                            minuteTime, date, resSet, caller, responder);
                }
            }
            key = caller + "," + ALL;
            if((ruleDictList = ruleMap.get(key)) != null){
                for(RuleDict ruleDict : ruleDictList){
                    ruleDict.compare(callerItem, ipAggregation,
                            minuteTime, date, resSet, caller, responder);
                }
            }
        }
    }
/*
    @Override
    public Collection<String> alarmMonitor(String path, Collection<String> alertRules)
            throws IOException, ParseException {
        //解析规则
        for(String rule : alertRules){
            String[] parts = rule.split(",");
            int id = Integer.parseInt(parts[0]);
            String caller = parts[1];
            String responder = parts[2];
            String type = parts[3];
            String detail = parts[4];
            int timeThresh = 0;
            for(int i = 0; detail.charAt(i) != '<' && detail.charAt(i) != '>'; ++i){
                timeThresh = timeThresh * 10 + detail.charAt(i) - '0';
            }
            char compare = detail.charAt(detail.length() - 1);
            String thresh = parts[5];
            List<RuleDict> list = ruleMap.computeIfAbsent(caller + "," + responder, k -> new ArrayList<>());
            list.add(new RuleDict(id,compare,type,timeThresh,thresh));
        }

        //解析数据
        Set<String> resSet = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String str;
        MinuteData smallCollection = new MinuteData();
        MinuteData largeCollection = new MinuteData();
        str = reader.readLine();
        String[] s = str.split(",");
        String caller = s[0];
        String callerIp = s[1];
        String responder = s[2];
        String responderIp = s[3];
        boolean result = Boolean.parseBoolean(s[4]);
        int cost = Integer.parseInt(s[5]);
        long timestamp = Long.parseLong(s[6]);
        int minuteTime = (int)(timestamp / 60000);
        smallCollection.setMinuteTime(minuteTime);
        smallCollection.add(caller + "," + responder + ',' +
                callerIp + "|" + responderIp, cost, result);
        smallCollection.setDate(sdf.format(new Date(timestamp)));
        while((str = reader.readLine()) != null){
            s = str.split(",");
            caller = s[0];
            callerIp = s[1];
            responder = s[2];
            responderIp = s[3];
            result = Boolean.parseBoolean(s[4]);
            cost = Integer.parseInt(s[5]);
            timestamp = Long.parseLong(s[6]);
            minuteTime = (int)(timestamp / 60000);
            if(minuteTime > smallCollection.getMinuteTime()
                    && minuteTime > largeCollection.getMinuteTime()){
                System.out.print(smallCollection.getMinuteTime() + " start -> ");
                calculateCollection(smallCollection, resSet);
                System.out.println("end!");
                smallCollection = largeCollection;
                largeCollection = new MinuteData(minuteTime);
                largeCollection.setDate(sdf.format(new Date(timestamp)));
            }
            if(smallCollection.getMinuteTime() == minuteTime){
                smallCollection.add(caller + "," + responder + ',' +
                        callerIp + "|" + responderIp, cost, result);
            }else{
                if(largeCollection.getMinuteTime() == Integer.MAX_VALUE){
                    largeCollection.setMinuteTime(minuteTime);
                    largeCollection.setDate(sdf.format(new Date(timestamp)));
                }
                largeCollection.add(caller + ","+responder + ',' +
                        callerIp + "|" + responderIp,
                        cost, result);
            }
        }
        System.out.print(smallCollection.getMinuteTime() + " start -> ");
        calculateCollection(smallCollection, resSet);
        System.out.println("end!");
        System.out.print(largeCollection.getMinuteTime() + " start -> ");
        calculateCollection(largeCollection, resSet);
        System.out.println("end!");
        return resSet;
    }
*/

    private String decodeIp(long ipAggregation) {
        return (ipAggregation >>> 56) + "." + ((ipAggregation & 0xFFFFFFFFFFFFFFL) >>> 48) + "."
                + ((ipAggregation & 0xFFFFFFFFFFFFL) >>> 40) + "." + ((ipAggregation & 0xFFFFFFFFFFL) >>> 32) + "," +
                ((ipAggregation & 0xFFFFFFFFL) >>> 24) + "." + ((ipAggregation & 0xFFFFFF) >>> 16) + "."
                + ((ipAggregation & 0xFFFF) >>> 8) + "." + (ipAggregation & 0xFF);
    }

    @Override
    public Collection<String> alarmMonitor(String path, Collection<String> alertRules) throws IOException, ParseException {
        //解析规则
        for(String rule : alertRules){
            String[] parts = rule.split(",");
            int id = Integer.parseInt(parts[0]);
            String caller = parts[1];
            String responder = parts[2];
            String type = parts[3];
            String detail = parts[4];
            int timeThresh = 0;
            for(int i = 0; detail.charAt(i) != '<' && detail.charAt(i) != '>'; ++i){
                timeThresh = timeThresh * 10 + detail.charAt(i) - '0';
            }
            char compare = detail.charAt(detail.length() - 1);
            String thresh = parts[5];
            List<RuleDict> list = ruleMap.computeIfAbsent(caller + "," + responder, k -> new ArrayList<>());
            list.add(new RuleDict(id,compare,type,timeThresh,thresh));
        }
        //读取数据
        InputStream inputStream = new FileInputStream(path);
        Map<Long, Map<Long, Map<Integer, CallerItem>>> map = new TreeMap<>();
        int cache = 1024 * 256;
        byte[] byteArr = new byte[cache];
        byte[] preByte = new byte[128];
        byte[] s = new byte[cache + 128];
        int preLen = 0, len, pos;
//        int smallMinute = Integer.MAX_VALUE, largeMinute = Integer.MAX_VALUE;
        while((len = inputStream.read(byteArr)) != -1){
            System.arraycopy(preByte, 0, s, 0, preLen);
            for (pos = len - 1; byteArr[pos] != 0x0A; --pos) ;
            System.arraycopy(byteArr, 0, s, preLen, ++pos);
            int end = pos + preLen;
            preLen = len - pos;
            System.arraycopy(byteArr, pos, preByte, 0, preLen);
            for(int i = 0; i < end;){
                int j = i;
                int caller = 0;
                //caller
                for(; s[j] != ','; ++j)
                    caller = caller * 31 + s[j];
                //caller ip
                long callerIp = 0;
                for (j += 1; s[j] != ','; ++j) {
                    int frame = 0;
                    for (; s[j] != '.' && s[j] != ','; ++j)
                        frame = frame * 10 + s[j] - '0';
                    callerIp = frame | (callerIp << 8L);
                    if(s[j] == ',')
                        break;
                }
                //responder
                int responder = 0;
                for(j += 1; s[j] != ','; ++j)
                    responder = responder * 31 + s[j];
                //responder ip
                long responderIp = 0;
                for (j += 1; s[j] != ','; ++j) {
                    int frame = 0;
                    for (; s[j] != '.' && s[j] != ','; ++j)
                        frame = frame * 10 + s[j] - '0';
                    responderIp = frame | (responderIp << 8L);
                    if(s[j] == ',')
                        break;
                }
                //result
                ++j;
                boolean result = s[j] == 't';

                j += result ? 4 : 5;

                int cost = 0;
                for (j += 1; s[j] != ','; ++j)
                    cost = cost * 10 + s[j] - '0';

                long timestamp = 0;
                for (j += 1; s[j] != 0x0A; ++j)
                    timestamp = timestamp * 10 + s[j] - '0';
                int minuteStamp = (int) (timestamp / 60000);
                i = j + 1;
                long serviceAggregation = 0L;
                serviceAggregation = (serviceAggregation << 32) | caller;
                serviceAggregation = (serviceAggregation << 32) | responder;
                long ipAggregation = 0L;
                ipAggregation = (ipAggregation << 32) | callerIp;
                ipAggregation = (ipAggregation << 32) | responderIp;
                Map<Long, Map<Integer, CallerItem>> serviceMap =
                        map.computeIfAbsent(serviceAggregation, k -> new HashMap<>());
                Map<Integer, CallerItem> ipAggregationMap =
                        serviceMap.computeIfAbsent(ipAggregation, k -> new HashMap<>());
                CallerItem callerItem = ipAggregationMap.computeIfAbsent(minuteStamp, k -> new CallerItem());
                callerItem.add(cost, result);
//                if(minuteStamp > smallMinute && minuteStamp > largeMinute){
//                    smallMinute = largeMinute;
//                    largeMinute = minuteStamp;
//                }
//                if(smallMinute == Integer.MAX_VALUE){
//                    smallMinute = minuteStamp;
//                }else if(largeMinute == Integer.MAX_VALUE){
//                    largeMinute = minuteStamp;
//                }
            }
        }
        return null;
    }

    @Override
    public Collection<String> getLongestPath(String caller, String responder, String time, String type) {
        return null;
    }
}