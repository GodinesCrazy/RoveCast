package com.ivanmarty.rovecast.player;

import android.os.CountDownTimer;

public class SleepTimerManager {

    private static SleepTimerManager instance;
    private CountDownTimer timer;
    private Runnable onFinish;
    private long millisRemaining = 0;

    private SleepTimerManager() {}

    public static synchronized SleepTimerManager getInstance() {
        if (instance == null) {
            instance = new SleepTimerManager();
        }
        return instance;
    }

    public void startTimer(long durationMillis, Runnable onFinishCallback) {
        cancelTimer(); // Cancela cualquier timer anterior
        this.onFinish = onFinishCallback;
        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisRemaining = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                millisRemaining = 0;
                if (onFinish != null) {
                    onFinish.run();
                }
                timer = null;
            }
        };
        timer.start();
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            millisRemaining = 0;
        }
    }

    public boolean isTimerRunning() {
        return timer != null;
    }

    public long getMillisRemaining() {
        return millisRemaining;
    }
}
