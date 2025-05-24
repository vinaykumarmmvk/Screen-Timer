package com.example.alertuser;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.Random;

public class OverlayService extends Service {

    private static WindowManager windowManager;
    private static View overlayView;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showPopup();
        return START_NOT_STICKY;
    }

    private void showPopup() {

        // Remove existing overlay before creating a new one
        removePopupSafely();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.popup_overlay, null);

        //overlayView = LayoutInflater.from(this).inflate(R.layout.popup_overlay, null);

        TextView message = overlayView.findViewById(R.id.messageText);
        TextView quote = overlayView.findViewById(R.id.quoteText);
        Button closeBtn = overlayView.findViewById(R.id.closeButton);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int count = prefs.getInt("count", 0) + 1;
        String currDate = String.valueOf(LocalDate.now());
        String prevDate = prefs.getString("prevDate", currDate) ;

        if (!prevDate.equals(currDate))
            prefs.edit().putInt("count", 0).apply();
        else
            prefs.edit().putInt("count", count).apply();

        prefs.edit().putString("prevDate", currDate).apply();

        quote.setText(getRandomQuote());
        message.setText("Limit exceeded " + count + " times");

        closeBtn.setOnClickListener(v -> {
            try {
                if (overlayView != null && windowManager != null) {
                    if (overlayView.getWindowToken() != null && overlayView.isAttachedToWindow()) {
                        windowManager.removeView(overlayView);
                    }
                    overlayView = null; // Clean up
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace(); // View not attached
            } catch (Exception e) {
                e.printStackTrace(); // Any other unexpected error
            }

            stopSelf();
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.CENTER;
        windowManager.addView(overlayView, params);
    }

    private void removePopupSafely() {
        try {
            if (overlayView != null && windowManager != null) {
                if (overlayView.isAttachedToWindow()) {
                    windowManager.removeView(overlayView);
                }
                overlayView = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRandomQuote() {
        String[] quotes = {
                "Keep pushing forward.",
                "Discipline is the bridge between goals and accomplishment.",
                "Stay focused and never give up.",
                "Success is the sum of small efforts repeated.",
                "Dream big. Work hard. Stay humble.",
                "Believe you can and you're halfway there.",
                "Don't watch the clock; do what it does. Keep going.",
                "The future depends on what you do today.",
                "Push yourself, because no one else is going to do it for you.",
                "Great things never come from comfort zones.",
                "Success doesn’t just find you. You have to go out and get it.",
                "Work hard in silence, let success make the noise.",
                "The harder you work for something, the greater you’ll feel when you achieve it.",
                "Dream it. Wish it. Do it.",
                "Success is what comes after you stop making excuses.",
                "Do something today that your future self will thank you for.",
                "Little things make big days.",
                "It’s going to be hard, but hard does not mean impossible.",
                "Don’t wait for opportunity. Create it.",
                "Sometimes we’re tested not to show our weaknesses, but to discover our strengths.",
                "Believe in yourself and all that you are.",
                "Stay positive, work hard, make it happen.",
                "Your only limit is your mind.",
                "You are capable of amazing things.",
                "Push through the pain, and rise above.",
                "Wake up with determination. Go to bed with satisfaction.",
                "The key to success is to focus on goals, not obstacles.",
                "The way to get started is to quit talking and begin doing.",
                "Don’t limit your challenges. Challenge your limits.",
                "It always seems impossible until it's done.",
                "Nothing worth having comes easy.",
                "You don’t have to be great to start, but you have to start to be great.",
                "Hard work beats talent when talent doesn't work hard.",
                "Perseverance is not a long race; it is many short races one after the other.",
                "Success isn’t overnight. It’s when every day you get a little better.",
                "Don’t stop when you’re tired. Stop when you’re done.",
                "Be so good they can’t ignore you.",
                "You’re only one decision away from a totally different life.",
                "Success is not final, failure is not fatal: it is the courage to continue that counts.",
                "Failure is simply the opportunity to begin again, this time more intelligently.",
                "Start where you are. Use what you have. Do what you can.",
                "Do what you can with all you have, wherever you are.",
                "Quality is not an act, it is a habit.",
                "Success is walking from failure to failure with no loss of enthusiasm.",
                "Set your goals high, and don’t stop till you get there.",
                "Don’t be pushed around by the fears in your mind. Be led by the dreams in your heart.",
                "If you want something you’ve never had, you must be willing to do something you’ve never done.",
                "It’s never too late to be what you might have been.",
                "Act as if what you do makes a difference. It does.",
                "The man who moves a mountain begins by carrying away small stones.",
                "Opportunities don't happen, you create them.",
                "Make your life a masterpiece; imagine no limitations on what you can be, have or do.",
                "The best view comes after the hardest climb.",
                "You don’t need to see the whole staircase, just take the first step.",
                "Believe in your infinite potential.",
                "If you get tired, learn to rest not to quit.",
                "A river cuts through rock, not because of its power, but because of its persistence.",
                "Don’t downgrade your dream just to fit your reality.",
                "Success doesn’t come from what you do occasionally. It comes from what you do consistently.",
                "You are braver than you believe, stronger than you seem, and smarter than you think.",
                "Everything you’ve ever wanted is on the other side of fear.",
                "Chase your dreams, not people.",
                "Be fearless in the pursuit of what sets your soul on fire.",
                "Go the extra mile. It’s never crowded.",
                "Don’t wish for it. Work for it.",
                "Your dreams don’t work unless you do.",
                "Failure will never overtake me if my determination to succeed is strong enough.",
                "Be stronger than your excuses.",
                "Your passion is waiting for your courage to catch up.",
                "Focus on your goals, not your fear.",
                "It does not matter how slowly you go as long as you do not stop.",
                "Live as if you were to die tomorrow. Learn as if you were to live forever.",
                "Work until your idols become your rivals.",
                "Learn from yesterday, live for today, hope for tomorrow.",
                "Energy and persistence conquer all things.",
                "If you believe it will work out, you’ll see opportunities.",
                "Never give up on a dream just because of the time it will take to accomplish it.",
                "Success is a journey, not a destination.",
                "You miss 100% of the shots you don’t take.",
                "What you get by achieving your goals is not as important as what you become by achieving your goals.",
                "Keep your eyes on the stars, and your feet on the ground.",
                "The only place where success comes before work is in the dictionary.",
                "You’ve got to get up every morning with determination.",
                "The difference between ordinary and extraordinary is that little extra.",
                "If you’re going through hell, keep going.",
                "Nothing will work unless you do.",
                "Small progress is still progress.",
                "Progress over perfection.",
                "Success usually comes to those who are too busy to be looking for it.",
                "One day or day one. You decide.",
                "Do not stop until you are proud.",
                "Stay hungry. Stay foolish.",
                "You are never too old to set another goal or to dream a new dream.",
                "It takes what it takes.",
                "Success starts with self-discipline.",
                "Every accomplishment starts with the decision to try.",
                "Excuses don’t get results.",
                "Consistency is what transforms average into excellence.",
                "Fear kills more dreams than failure ever will.",
                "Action is the foundational key to all success.",
                "Never stop learning, because life never stops teaching.",
                "Be the energy you want to attract.",
                "Mindset is what separates the best from the rest."
        };

        return quotes[new Random().nextInt(quotes.length)];
    }

    @Override
    public void onDestroy() {
        removePopupSafely();
        super.onDestroy();
        /*try {
            if (windowManager != null && overlayView != null && overlayView.isAttachedToWindow()) {
                windowManager.removeView(overlayView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}
