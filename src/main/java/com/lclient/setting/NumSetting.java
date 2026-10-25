package com.lclient.setting;

public class NumSetting extends Setting<Double> {
    public final double min, max, step;
    public NumSetting(String name, double def, double min, double max, double step) {
        super(name, def); this.min = min; this.max = max; this.step = step;
    }
    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        value = Math.round(v / step) * step;
    }
    public double d() { return value; }
    public int i() { return (int) Math.round(value); }
    public String display() { return step >= 1 ? String.valueOf(i()) : String.format("%.1f", value); }
}
