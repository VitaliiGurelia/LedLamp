package com.example.ledlamp;

public class Lamp {
    public String name;
    public String ip;

    public Lamp(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    // Цей метод відповідає за те, що буде написано у випадаючому списку
    @Override
    public String toString() {
        return name;
    }
}