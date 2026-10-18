package com.example.ledlamp;

import androidx.annotation.NonNull;

public class Lamp {
    public String name;
    public String ip;

    public Lamp(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    // Цей метод відповідає за те, що буде написано у випадаючому списку
    @NonNull
    @Override
    public String toString() {
        return name;
    }
}