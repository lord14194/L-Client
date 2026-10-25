package com.lclient.module;

public enum Category {
    BASEHUNT("Basehunt"), MOVEMENT("Movement"), PLAYER("Player"), VISUAL("Visual"),
    /** Rows that stand for modules inside the bundled mods. Never saved in lclient.json. */
    EXTERNAL("External");
    public final String label;
    Category(String label) { this.label = label; }
}
