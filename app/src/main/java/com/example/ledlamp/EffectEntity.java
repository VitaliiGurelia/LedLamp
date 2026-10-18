package com.example.ledlamp;

import androidx.annotation.NonNull;
import java.util.Locale;

public class EffectEntity {
    public int id;
    public String nameUA, nameEN, nameDK;

    // Заводські значення (незмінні)
    public final int defBright, defSpeed, defScale;

    // ПОТОЧНІ ЗНАЧЕННЯ (змінюються користувачем та синхронізуються з лампою)
    public int bright, speed, scale;

    public int speedMin, speedMax;
    public int scaleMin, scaleMax;
    public int scaleType;

    public boolean isVisible;
    public boolean useInCycle;

    public EffectEntity(int id, String nameUA, String nameEN, String nameDK,
                        int defBright, int defSpeed, int defScale,
                        int speedMin, int speedMax,
                        int scaleMin, int scaleMax,
                        int scaleType) {
        this.id = id;
        this.nameUA = nameUA;
        this.nameEN = nameEN;
        this.nameDK = nameDK;
        this.defBright = defBright;
        this.defSpeed = defSpeed;
        this.defScale = defScale;
        
        // При ініціалізації поточні значення дорівнюють заводським
        this.bright = defBright;
        this.speed = defSpeed;
        this.scale = defScale;

        this.speedMin = speedMin;
        this.speedMax = speedMax;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.scaleType = scaleType;

        this.isVisible = true;
        this.useInCycle = true;
    }

    public String getLocalizedName() {
        String lang = Locale.getDefault().getLanguage();
        if (lang.equals("uk")) return nameUA;
        else if (lang.equals("da")) return nameDK;
        else return nameEN;
    }

    @NonNull
    @Override
    public String toString() {
        return getLocalizedName();
    }
}
