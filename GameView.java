package com.example.farminggame;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.view.MotionEventCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//Create the GameView class: fuctioning as the main class the game is ran from
public class GameView extends SurfaceView implements Runnable {

    private Thread thread;
    private boolean isPlaying = true;
    private boolean isGameOver = false;
    private int screenX, screenY, score = 0;
    public static float screenRatioX, screenRatioY;
    private Paint paint;
    private SharedPreferences preferences;
    private Background background1, background2;
    private Flight flight;
    private List<Bullet> bullets;
    private Bird[] birds;
    private Random random;
    private GameActivity context;

    //GameView Function: Used to render the game on screen
    public GameView(GameActivity context, int screenX, int screenY) {
        super(context);

        this.context = context;
        preferences = context.getSharedPreferences("game", Context.MODE_PRIVATE);

        this.screenX = screenX;     //Get screen sizes both X & Y
        this.screenY = screenY;
        screenRatioX = 1920f /screenX;      //Set the size to the correct ratio
        screenRatioY = 1080f /screenY;

        background1 = new Background(screenX,screenY, getResources());              //Save the two background resoursces for later use
        background2 = new Background(screenX, screenY, getResources());
        background2.x = screenX;

        flight = new Flight(this, screenY, getResources());     //Create new flight object: Used for the plane
        bullets = new ArrayList<>();            //Create  a new bullet object: Used for all bullets
        birds = new Bird[4];                    //Create the array of birds
        random = new Random();                  //creat a object of type random
        paint = new Paint();
        paint.setTextSize(128);
        paint.setColor(Color.WHITE);

        for(int i = 0; i <4; i++) {                     //Fill the array of birds with objects type bird
            Bird bird = new Bird(getResources());
            birds[i] = bird;
        }
    }

    //Overide and use the run fuction
    @Override
    public void run(){
        while(isPlaying) {
            update();           //Update the game
            draw();             //render the update
            sleep();            //wait
        }
    }

    //function to update all the game info
    private void update() {

        background1.x -= 10 * screenRatioX;         //Make background scroll by moving images x cooredinate
        background2.x -=10 * screenRatioX;
        if (background1.x + background1.background.getWidth() < 0) {        //move the backgrounds back to the start
            background1.x = screenX;
        }
        if (background2.x + background2.background.getWidth() < 0) {
            background2.x = screenX;
        }

        if(flight.isGoingUp) {                  //Move the plane up or down based on the boolean "isGoing..."
            flight.y -= 10 * screenRatioY;
        }
        if(flight.isGoingDown){
            flight.y += 10 * screenRatioY;
        }
        if(flight.y < 0) {                  //Stop the plane from moving outside of the screen
            flight.y = 0;
        }
        if(flight.y > screenY - flight.height) {
            flight.y = screenY - flight.height;
        }

        List<Bullet> trash = new ArrayList<>();

        for (Bullet bullet : bullets) {
            if(bullet.x > screenX) {
                trash.add(bullet);
            }
            bullet.x += 30 * screenRatioX;
            for(Bird bird : birds){
                if (Rect.intersects(bird.getCollisionShape(), bullet.getCollisionShape())) {
                    bird.x = -1000;
                    bullet.x = screenX + 1000;
                    bird.wasShot = true;
                    score++;
                }
            }
        }

        for (Bullet bullet : trash) {
            bullets.remove(bullet);
        }

        for(Bird bird : birds) {
            bird.x -= bird.speed;

            if (bird.x + bird.width < 0) {

                if(!bird.wasShot) {
                    isGameOver = true;
                    return;
                }
                int bound = (int) ( 15 * screenRatioX);
                bird.speed = random.nextInt(bound);
                if (bird.speed < 5 * screenRatioX)
                    bird.speed = (int) (5 * screenRatioX);

                bird.x = screenX;
                bird.y = random.nextInt(screenY - bird.height);

                bird.wasShot = true;
            }

            if(Rect.intersects(bird.getCollisionShape(), flight.getCollisionShape())){
                isGameOver = true;
                return;
            }
        }
    }

    private void draw() {

        if(getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();
            canvas.drawBitmap(background1.background, background1.x, background1.y, null);
            canvas.drawBitmap(background2.background, background2.x, background2.y, null);

            for(Bird bird : birds)
                canvas.drawBitmap(bird.getBird(), bird.x, bird.y ,null);

            canvas.drawText(score + "", screenX / 2f, 164, paint);

            if(isGameOver) {
                isPlaying = false;
                canvas.drawBitmap(flight.getDead(), flight.x, flight.y, null);
                SaveHighScore();
                getHolder().unlockCanvasAndPost(canvas);
                try {
                   Thread.sleep(3000);
                   context.startActivity(new Intent(context, MainActivity.class));
                   context.finish();
                } catch (Exception e) {
                   e.printStackTrace();
                }
                return;
            }

            canvas.drawBitmap(flight.getFlight(), flight.x, flight.y, null);

            for(Bird bird : birds)
                canvas.drawBitmap(bird.getBird(), bird.x, bird.y ,null);

            for (Bullet bullet : bullets)
                canvas.drawBitmap(bullet.bullet, bullet.x, bullet.y, null);

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void SaveHighScore() {

        if (preferences.getInt("highscore", 0) < score) {
               SharedPreferences.Editor editor = preferences.edit();
               editor.putInt("highscore", score);
               editor.apply();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        try{
            isPlaying = false;
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                 if (event.getX() < screenX / 2) {
                    if(event.getY() < screenY / 2)
                        flight.isGoingUp = true;
                    if(event.getY() > screenY / 2)
                        flight.isGoingDown = true;
                 }
                 if (event.getX() > screenX /2) {
                    flight.toShoot++;
                 }

            break;

            case MotionEvent.ACTION_MOVE:
                flight.isGoingUp = false;
                flight.isGoingDown = false;
                if (event.getX() < screenX / 2) {
                    if(event.getY() < screenY / 2)
                        flight.isGoingUp = true;
                    if(event.getY() > screenY / 2)
                        flight.isGoingDown = true;
                }
                if (event.getX() > screenX /2) {
                    flight.toShoot++;
                }

            break;

            case MotionEvent.ACTION_UP:

                flight.isGoingUp = false;
                flight.isGoingDown = false;

            break;
        }
        return true;
    }

    public void newBullet() {
        Bullet bullet = new Bullet(getResources());
        bullet.x = flight.x + flight.width;
        bullet.y = flight.y + (flight.height / 2);
        bullets.add(bullet);
    }
}

