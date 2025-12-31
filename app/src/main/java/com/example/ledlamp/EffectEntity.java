package com.example.ledlamp;

import androidx.annotation.NonNull;
import java.util.Locale;

public class EffectEntity {
    // Технічний номер у прошивці
    public int id;

    // Назви трьома мовами
    public String nameUA;
    public String nameEN;
    public String nameDK;

    // Значення за замовчуванням
    public int defBright;
    public int defSpeed;
    public int defScale;

    // Діапазони налаштувань
    public int speedMin, speedMax;
    public int scaleMin, scaleMax;

    // Тип регулятора: 0=Масштаб, 1=Колір, 2=Приховати
    public int scaleType;

    // --- ЛОГІЧНІ ЗМІННІ (Для налаштувань додатку) ---

    // 1. Чи показувати в головному списку (для EffectsSettingsActivity)
    public boolean isVisible;

    // 2. Чи використовувати в режимі циклу (для CycleActivity) -> ОСЬ ЦЬОГО НЕ ВИСТАЧАЛО
    public boolean useInCycle;

    // Конструктор
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
        this.speedMin = speedMin;
        this.speedMax = speedMax;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.scaleType = scaleType;

        // Встановлюємо значення за замовчуванням
        this.isVisible = true;
        this.useInCycle = true; // <--- ТЕПЕР ПОМИЛКА ЗНИКНЕ
    }

    // --- ЛОГІКА МОВИ ---
    public String getLocalizedName() {
        String lang = Locale.getDefault().getLanguage();

        if (lang.equals("uk")) {
            return nameUA;
        } else if (lang.equals("da")) {
            return nameDK;
        } else {
            return nameEN;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getLocalizedName();
    }
}