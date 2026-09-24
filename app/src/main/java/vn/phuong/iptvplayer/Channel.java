package vn.phuong.iptvplayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

public final class Channel {
    private final String name, group, url, logo, tvgId;
    private final Map<String, String> headers;
    private final String catchupType, catchupSource;
    private final double catchupDays;
    private boolean selected = true;
    private String originalExtInf = "";
    private final List<String> options = new ArrayList<>();

    public Channel(String name, String group, String url, String logo, String tvgId, Map<String, String> headers) {
        this(name, group, url, logo, tvgId, headers, "", "", 0d);
    }
    public Channel(String name, String group, String url, String logo, String tvgId, Map<String, String> headers,
                   String catchupType, String catchupSource, double catchupDays) {
        this.name = clean(name, "Kênh không tên");
        this.group = clean(group, "Chưa phân nhóm");
        this.url = clean(url, "");
        this.logo = clean(logo, "");
        this.tvgId = clean(tvgId, "");
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.catchupType = clean(catchupType, "");
        this.catchupSource = clean(catchupSource, "");
        this.catchupDays = Math.max(0d, catchupDays);
    }
    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }
    public String name(){return name;} public String group(){return group;} public String url(){return url;}
    public String logo(){return logo;} public String tvgId(){return tvgId;} public Map<String,String> headers(){return headers;}
    public String catchupType(){return catchupType;} public String catchupSource(){return catchupSource;} public double catchupDays(){return catchupDays;}
    public boolean hasCatchup(){return !catchupType.isEmpty() && catchupDays>0d && !catchupSource.isEmpty();}
    public boolean selected(){return selected;} public void setSelected(boolean selected){this.selected=selected;}
    public String originalExtInf(){return originalExtInf;} public void setOriginalExtInf(String line){originalExtInf=line;}
    public List<String> options(){return options;}
    public String mimeHint(){
        for(String option:options){String lower=option.toLowerCase(java.util.Locale.ROOT);
            if(lower.endsWith("manifest_type=hls"))return "application/x-mpegURL";
            if(lower.endsWith("manifest_type=mpd"))return "application/dash+xml";
            if(lower.startsWith("#kodiprop:mimetype="))return option.substring(option.indexOf('=')+1).trim();}
        return "";
    }
    public boolean needsDrm(){for(String option:options)if(option.toLowerCase(java.util.Locale.ROOT).contains("inputstream.adaptive.license"))return true;return false;}
    public String identityKey(){
        Map<String,String> normalized=new TreeMap<>();
        for(Map.Entry<String,String> header:headers.entrySet())normalized.put(header.getKey().toLowerCase(java.util.Locale.ROOT),header.getValue());
        StringBuilder key=new StringBuilder(); appendKeyPart(key,url.trim());
        key.append(normalized.size()).append(':');
        for(Map.Entry<String,String> header:normalized.entrySet()){appendKeyPart(key,header.getKey());appendKeyPart(key,header.getValue());}
        key.append(options.size()).append(':'); for(String option:options)appendKeyPart(key,option);
        return key.toString();
    }
    private static void appendKeyPart(String target,String value){target.append(value.length()).append(':').append(value);}
    @Override public boolean equals(Object object){return object instanceof Channel && identityKey().equals(((Channel)object).identityKey());}
    @Override public int hashCode(){return Objects.hash(identityKey());}
}