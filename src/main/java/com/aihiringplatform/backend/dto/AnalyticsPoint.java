package com.aihiringplatform.backend.dto;

public class AnalyticsPoint {

    private String label;
    private Integer value;
    private Integer secondaryValue;
    private String meta;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getSecondaryValue() {
        return secondaryValue;
    }

    public void setSecondaryValue(Integer secondaryValue) {
        this.secondaryValue = secondaryValue;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }
}
