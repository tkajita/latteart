package org.kajip.latteart;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class OutputData {

    public static final String SAMPLING_TIME_KEY = "time";

    // @caution tomcatの接続プール情報のMBean に host という値があり、上書きされる可能性があるので hostname にした。
    public static final String HOSTNAME_KEY = "hostname";

    public static final String  DOMAIN_KEY = "domain";

    public static final String  OBJECT_NAME_KEY = "objectName";

//    static final DateTimeFormatter dateTimeformatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    static final DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSSxxx");

    private final Map<String,Object>  outputData;


    public OutputData(ZonedDateTime samplingTime, String hostname, SamplingData samplingData) {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put(SAMPLING_TIME_KEY, samplingTime.format(dateTimeformatter));
        map.put(HOSTNAME_KEY, hostname);
        map.putAll(parseSamplingData(samplingData));

        this.outputData = Collections.unmodifiableMap(map);
    }

    private Map<String,Object> parseSamplingData(SamplingData samplingData) {

        Map<String,Object> map = parseObjectName(samplingData.objectName);
        map.putAll(parseAttributes(samplingData.attributes));

        return map;
    }

    /**
     * ObjectName を解析
     * @param objectName
     * @return
     */
    private Map<String,Object> parseObjectName(ObjectName objectName) {

        Map<String,Object> map = new LinkedHashMap<>();
        map.put(OBJECT_NAME_KEY, objectName.getCanonicalName());
        map.put(DOMAIN_KEY, objectName.getDomain());
        map.putAll(objectName.getKeyPropertyList());

        return map;
    }

    /**
     * AttributeList を解析
     * @param attributes
     * @return
     */
    private Map<String,Object> parseAttributes(AttributeList attributes) {
        Map<String,Object> map = new LinkedHashMap<>();

        for(Attribute attribute : attributes.asList()) {
            if(attribute.getValue() instanceof CompositeDataSupport) {
                map.putAll(parseCompositeDataSupport((CompositeDataSupport) attribute.getValue()));
            } else {
                map.put(attribute.getName(), attribute.getValue());
            }
        }

        return map;
    }

    /**
     * CompositeDataSupport を解析
     * @param compositeDataSupport
     * @return
     */
    private Map<String,Object> parseCompositeDataSupport(CompositeDataSupport compositeDataSupport) {

        CompositeType compositeType = compositeDataSupport.getCompositeType();

        Map<String,Object> map = new LinkedHashMap<>();
        for (String key: compositeType.keySet()) {
            Object value = compositeDataSupport.get(key);

            if (!(value instanceof CompositeDataSupport)) {
                map.put(key, value);
            } else {
                map.putAll(parseCompositeDataSupport((CompositeDataSupport) value));
            }
        }

        return map;
    }

    public String printLTSV() {
        return outputData.entrySet().stream()
                .map(entry -> String.format("%s:%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\t"));
    }
}
