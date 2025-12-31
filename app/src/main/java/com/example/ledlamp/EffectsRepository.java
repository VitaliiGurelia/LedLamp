package com.example.ledlamp;

import java.util.ArrayList;

public class EffectsRepository {
    // Головний список ефектів (перенесено з MainActivity)
    public static final ArrayList<EffectEntity> EFFECTS_DB = new ArrayList<>();

    static {
        // --- БАЗОВІ ---
        EFFECTS_DB.add(new EffectEntity(0, "Біле світло", "White Light", "Hvidt lys", 9, 207, 26, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(1, "Колір", "Color", "Farve", 9, 180, 99, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(2, "Зміна кольору", "Color Change", "Farveskift", 10, 252, 32, 1, 255, 1, 255, 0));

        // --- ГРУПА ВОГНІ ---
        EFFECTS_DB.add(new EffectEntity(18, "Вогонь 1", "Fire 1", "Ild 1", 22, 53, 3, 1, 255, 0, 255, 1));
        EFFECTS_DB.add(new EffectEntity(19, "Вогонь 2", "Fire 2", "Ild 2", 9, 51, 11, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(39, "Вогонь 3", "Fire 3", "Ild 3", 9, 225, 59, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(40, "Вогонь 4", "Fire 4", "Ild 4", 57, 225, 15, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(41, "Вогонь 5", "Fire 5", "Ild 5", 9, 220, 20, 120, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(42, "Вогонь 6", "Fire 6", "Ild 6", 22, 225, 1, 99, 252, 1, 100, 1));

        // --- ІНШІ ЕФЕКТИ ---
        EFFECTS_DB.add(new EffectEntity(43, "Вихори полум'я", "Fire Whirls", "Ildhvirvler", 9, 240, 1, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(45, "Магма", "Magma", "Magma", 9, 198, 20, 150, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(46, "Кипіння", "Boiling", "Kogning", 7, 240, 18, 170, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(3, "Безумство", "Madness", "Galskab", 11, 33, 58, 1, 150, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(4, "Хмари", "Clouds", "Skyer", 8, 4, 34, 1, 15, 1, 50, 0));
        EFFECTS_DB.add(new EffectEntity(5, "Лава", "Lava", "Lava", 8, 9, 24, 5, 60, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(6, "Плазма", "Plasma", "Plasma", 11, 19, 59, 1, 30, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(7, "Веселка 3D", "Rainbow 3D", "Regnbue 3D", 11, 13, 60, 1, 70, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(8, "Павич", "Peacock", "Påfugl", 11, 5, 12, 1, 15, 1, 30, 0));
        EFFECTS_DB.add(new EffectEntity(9, "Зебра", "Zebra", "Zebra", 7, 8, 21, 1, 30, 7, 40, 0));
        EFFECTS_DB.add(new EffectEntity(10, "Ліс", "Forest", "Skov", 7, 8, 95, 2, 30, 70, 100, 0));
        EFFECTS_DB.add(new EffectEntity(11, "Океан", "Ocean", "Ocean", 7, 6, 12, 2, 15, 4, 30, 0));
        EFFECTS_DB.add(new EffectEntity(12, "М'ячики", "Balls", "Bolde", 24, 255, 26, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(13, "М'ячики без кордонів", "Bounce", "Bolde uden grænser", 18, 11, 70, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(14, "Попкорн", "Popcorn", "Popcorn", 19, 32, 16, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(15, "Спіралі", "Spirals", "Spiraler", 9, 46, 3, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(16, "Призмата", "Prismata", "Prismata", 17, 100, 2, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(17, "Димові шашки", "Smokeballs", "Røgkugler", 12, 44, 17, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(20, "Тихий океан", "Pacific", "Stillehavet", 55, 127, 100, 1, 255, 100, 100, 2));
        EFFECTS_DB.add(new EffectEntity(21, "Тіні", "Shadows", "Skygger", 39, 77, 1, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(22, "ДНК", "DNA", "DNA", 15, 77, 95, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(23, "Зграя", "Flock", "Flok", 15, 136, 4, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(24, "Зграя і хижак", "Flock & Predator", "Flok og rovdyr", 15, 128, 80, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(25, "Метелики", "Butterflies", "Sommerfugle", 11, 53, 87, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(26, "Лампа з метеликами", "Lamp w/ Butterflies", "Lampe med sommerfugle", 7, 61, 100, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(27, "Змійки", "Snakes", "Slanger", 9, 96, 31, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(28, "Nexus", "Nexus", "Nexus", 19, 60, 20, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(29, "Кулі", "Spheres", "Kugler", 9, 85, 85, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(30, "Синусоїд", "Sinusoid", "Sinusoide", 7, 89, 83, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(31, "Метаболз", "Metaballs", "Metabolde", 7, 85, 3, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(32, "Північне сяйво", "Aurora", "Nordlys", 12, 73, 38, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(33, "Плазмова лампа", "Plasma Lamp", "Plasmaler", 8, 59, 18, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(34, "Лавова лампа", "Lava Lamp", "Lavalampe", 23, 203, 1, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(35, "Рідка лампа", "Liquid Lamp", "Flydende lampe", 11, 63, 1, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(36, "Рідка лампа (авто)", "Liquid Auto", "Flydende auto", 11, 124, 39, 1, 255, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(37, "Краплі на склі", "Drops", "Dråber", 23, 71, 59, 1, 255, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(38, "Матриця", "Matrix", "Matrix", 27, 186, 23, 99, 240, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(44, "Різнокольорові вихори", "Multi Whirls", "Farvede hvirvler", 9, 240, 86, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(47, "Водоспад", "Waterfall", "Vandfald", 5, 212, 54, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(48, "Водоспад 4в1", "Waterfall 4in1", "Vandfald 4i1", 7, 197, 22, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(49, "Басейн", "Pool", "Pool", 8, 222, 63, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(50, "Пульс", "Pulse", "Puls", 12, 185, 6, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(51, "Райдужний пульс", "Rainbow Pulse", "Regnbuepuls", 11, 185, 31, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(52, "Білий пульс", "White Pulse", "Hvid puls", 9, 179, 11, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(53, "Осцилятор", "Oscillator", "Oscillator", 8, 208, 100, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(54, "Джерело", "Fountain", "Kilde", 15, 233, 77, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(55, "Фея", "Fairy", "Fe", 19, 212, 44, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(56, "Комета", "Comet", "Komet", 16, 220, 28, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(57, "Одноколірна комета", "Color Comet", "Farvet komet", 14, 212, 69, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(58, "Дві комети", "Two Comets", "To kometer", 27, 186, 19, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(59, "Три комети", "Three Comets", "Tre kometer", 24, 186, 9, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(60, "Тяжіння", "Attract", "Tiltrækning", 21, 203, 65, 160, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(61, "Ширяючий вогонь", "Firefly", "Svævende ild", 26, 206, 15, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(62, "Верховий вогонь", "Firefly Top", "Top ild", 26, 190, 15, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(63, "Райдужний змій", "Snake", "Regnbueslange", 12, 178, 1, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(64, "Конфетті", "Sparkles", "Konfetti", 16, 142, 63, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(65, "Мерехтіння", "Twinkles", "Flimren", 25, 236, 4, 60, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(66, "Дим", "Smoke", "Røg", 9, 157, 100, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(67, "Різнокольоровий дим", "Color Smoke", "Farvet røg", 9, 157, 30, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(68, "Пікассо", "Picasso", "Picasso", 9, 189, 43, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(69, "Хвилі", "Waves", "Bølger", 9, 236, 80, 220, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(70, "Кольорові драже", "Sand", "Farvede piller", 9, 195, 80, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(71, "Кодовий замок", "Rings", "Kodelås", 10, 222, 92, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(72, "Кубик Рубіка", "Cube 2D", "Rubiks terning", 10, 231, 89, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(73, "Хмарка в банці", "Simple Rain", "Sky i krukke", 30, 233, 2, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(74, "Гроза в банці", "Stormy Rain", "Tordenvejr", 20, 236, 25, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(75, "Опади", "Color Rain", "Nedbør", 15, 198, 99, 99, 252, 0, 255, 1));
        EFFECTS_DB.add(new EffectEntity(76, "Різнокольоровий дощ", "Rain", "Farvet regn", 15, 225, 1, 99, 252, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(77, "Снігопад", "Snow", "Snefald", 9, 180, 90, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(78, "Зорепад / Заметіль", "Starfall", "Stjerneskud", 20, 199, 54, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(79, "Стрибуни", "Leapers", "Springere", 24, 203, 5, 150, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(80, "Світлячки", "Lighters", "Ildfluer", 15, 157, 23, 50, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(81, "Світлячки зі шлейфом", "Lighter Traces", "Ildfluespor", 21, 198, 93, 99, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(82, "Люмен'єр", "Lumenjer", "Lumenjer", 14, 223, 40, 1, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(83, "Пейнтбол", "Paintball", "Paintball", 11, 236, 7, 215, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(84, "Веселка", "Rainbow Ver", "Regnbue", 8, 196, 56, 50, 252, 1, 100, 0));
        EFFECTS_DB.add(new EffectEntity(85, "Годинник", "Clock", "Ur", 4, 5, 100, 1, 245, 1, 100, 1));
        EFFECTS_DB.add(new EffectEntity(86, "Прапор України", "Flag UA", "Ukraines flag", 120, 150, 50, 1, 255, 1, 100, 2));
        EFFECTS_DB.add(new EffectEntity(87, "Прапор Данії", "Flag DK", "Danmarks flag", 120, 150, 20, 1, 255, 1, 100, 2));
        EFFECTS_DB.add(new EffectEntity(88, "Малюнок", "Drawing", "Tegning", 10, 5, 1, 1, 255, 0, 255, 2)); // Type 2 (без масштабу)
        EFFECTS_DB.add(new EffectEntity(89, "Бігучий рядок", "Running Text", "Løbende tekst", 10, 99, 38, 1, 252, 1, 100, 1));
    }

}