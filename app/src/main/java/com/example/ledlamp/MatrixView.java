package com.example.ledlamp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class MatrixView extends View {

    private final int ROWS = 16;
    private final int COLS = 16;
    private final int[][] grid = new int[COLS][ROWS]; // Зберігаємо колір кожного пікселя

    private Paint paint;
    private Paint borderPaint;
    private float cellSize = 0;
    private int currentColor = Color.RED; // Колір за замовчуванням

    // Інтерфейс для передачі координат у Activity
    public interface OnPixelTouchListener {
        void onPixelTouched(int x, int y, int color);
    }
    private OnPixelTouchListener listener;

    public MatrixView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.DKGRAY);
        borderPaint.setStrokeWidth(2f);
    }

    public void setOnPixelTouchListener(OnPixelTouchListener listener) {
        this.listener = listener;
    }

    public void setCurrentColor(int color) {
        this.currentColor = color;
    }

    public void clear() {
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                grid[x][y] = 0; // Чорний/Пустий
            }
        }
        invalidate(); // Перемалювати
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Робимо View квадратним на основі ширини екрану
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Розраховуємо розмір клітинки
        float width = getWidth();
        cellSize = width / COLS;

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                // Малюємо піксель
                int color = grid[x][y];
                if (color == 0) paint.setColor(Color.BLACK); // Фон
                else paint.setColor(color);

                // (Y інвертований у деяких матрицях, але тут малюємо стандартно зверху-вниз)
                // Для Gunner/SottNick матриця часто починається з лівого нижнього кута або лівого верхнього.
                // Будемо малювати як на екрані: (0,0) - лівий верхній.

                float left = x * cellSize;
                float top = y * cellSize;

                canvas.drawRect(left, top, left + cellSize, top + cellSize, paint);
                canvas.drawRect(left, top, left + cellSize, top + cellSize, borderPaint); // Сітка
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float xPos = event.getX();
            float yPos = event.getY();

            // Визначаємо, яку клітинку зачепили
            int x = (int) (xPos / cellSize);
            int y = (int) (yPos / cellSize);

            // Перевірка меж
            if (x >= 0 && x < COLS && y >= 0 && y < ROWS) {
                // Якщо колір змінився - оновлюємо і шлемо команду
                if (grid[x][y] != currentColor) {
                    grid[x][y] = currentColor;
                    invalidate(); // Перемалювати екран

                    if (listener != null) {
                        // Увага: Y на матриці може бути перевернутий.
                        // У SottNick (0,0) зазвичай лівий нижній.
                        // У Android (0,0) лівий верхній.
                        // Відправимо як є, якщо буде перевернуто - додамо `15 - y`.
                        listener.onPixelTouched(x, 15 - y, currentColor); // Перевертаємо Y для стандарту лампи
                    }
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
    // Метод для заповнення всієї матриці з байтового масиву
    public void setFullImage(byte[] data) {
        if (data.length < 768) return; // Захист

        int idx = 0;
        // Увага: прошивка віддає нам рядки знизу-вгору або зверху-вниз залежно від налаштувань.
        // Але ми записували XY(x,y).
        // Тут ми просто заповнюємо сітку.

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                // Java byte (-128..127) -> int (0..255)
                int r = data[idx++] & 0xFF;
                int g = data[idx++] & 0xFF;
                int b = data[idx++] & 0xFF;

                // Прошивка SottNick зазвичай шле RGB.
                // Але Android Color.rgb хоче (r, g, b).
                // У нас є налаштування swapColors, але при отриманні сирих даних
                // зазвичай простіше відобразити як є.

                grid[x][(ROWS - 1) - y] = Color.rgb(r, g, b);
                // (ROWS - 1) - y : Інверсія Y, бо лампа (0,0) знизу, а Android зверху.
            }
        }
        invalidate(); // Перемалювати
    }
}