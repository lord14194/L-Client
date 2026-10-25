package com.lclient.setting;

public abstract class Setting<T> {
    public final String name;
    public T value;
    protected Setting(String name, T value) { this.name = name; this.value = value; }
}
