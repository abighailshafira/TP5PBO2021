/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modulgame;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
/**
 *
 * @author Fauzan
 */
public class Game extends Canvas implements Runnable{
    Window window;
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    
    private int score = 0;
    
    private int time = 0;
    
    private Thread thread;
    private boolean running = false;
    
    private Handler handler;
    
    private String username = "";
    private String level;
    private String mode;
    
    private int total_waktu = 0;
    private int maks = 0;
    
    private Clip bgMusic;
    private final Random rand = new Random();
    
    public enum STATE{
        Game,
        GameOver
    };
    
    public STATE gameState = STATE.Game;
    
    public Game(String uname, String lvl, String md){
        window = new Window(WIDTH, HEIGHT, "Modul praktikum 5", this);
        
        handler = new Handler();
        dbConnection dbcon;
        this.addKeyListener(new KeyInput(handler, this));
        
        this.username = uname;
        this.level = lvl;
        this.mode = md;
        
        switch (lvl) {
            case "Easy":
                this.total_waktu = 20;
                this.time = 20;
                this.maks = 4;
                break;
            case "Normal":
                this.total_waktu = 10;
                this.time = 10;
                this.maks = 5;                
                break;
            case "Hard":
                this.total_waktu = 5;
                this.time = 5;
                this.maks = 10;
                break;
            default:
                this.total_waktu = 10;
                this.time = 10;
                this.maks = 5;
                break;
        }
        
        
        if(gameState == STATE.Game){
            //gambar player dan item pada awal permainan
            handler.addObject(new Player(200,200, ID.Player));
            handler.addObject(new Items(100,150, ID.Item));
            handler.addObject(new Items(200,350, ID.Item));
//            handler.addObject(new Enemy(350, 100, ID.Enemy));
            if(mode.equals("2")){
                handler.addObject(new Player(500,200, ID.Player2));
            }
        }
    }

    public synchronized void start(){
        thread = new Thread(this);
        thread.start();
        running = true;
    }
    
    public synchronized void stop(){
        try{
            thread.join();
            running = false;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;
        bgMusic = playSound("/kids-song.wav");
        
        while(running){
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            
            while(delta >= 1){
                try {
                    tick();
                    delta--;
                } catch (InterruptedException ex) {
                    Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(running){
                render();
                frames++;
            }
            
            if(System.currentTimeMillis() - timer > 1000){
                timer += 1000;
                //System.out.println("FPS: " + frames);
                frames = 0;
                if(gameState == STATE.Game){
                    if(time>0){
                        time--;
                        
                    }else{
                        gameState = STATE.GameOver;
                        end();
                    }
                }
            }
        }
        stop();
    }
    
    private void tick() throws InterruptedException{
        handler.tick();
        if(gameState == STATE.Game){
            GameObject playerObject = null;
            GameObject playerObject2 = null;
            
            for(int i=0;i< handler.object.size(); i++){
                if(handler.object.get(i).getId() == ID.Player){
                   playerObject = handler.object.get(i);
                }
            }
            
            for(int i=0;i< handler.object.size(); i++){
                if(handler.object.get(i).getId() == ID.Player2){
                   playerObject2 = handler.object.get(i);
                }
            }
            
            //MODE 2 PLAYER
            if(playerObject != null){
                for(int i=0;i< handler.object.size(); i++){
                    if(handler.object.get(i).getId() == ID.Item){
                        if(checkCollision(playerObject, handler.object.get(i))){
                            playSound("/Eat.wav");
                            handler.removeObject(handler.object.get(i));
                            int add = rand.nextInt(10);
                            score = score + add;
                            time = time + add;
                            total_waktu = total_waktu + add;
                            break;
                        }
                    }
                }
            }
            
            //MODE 2 PLAYER
            if(playerObject2 != null){
                for(int i=0;i< handler.object.size(); i++){
                    if(handler.object.get(i).getId() == ID.Item){
                        if(checkCollision(playerObject2, handler.object.get(i))){
                            playSound("/Eat.wav");
                            handler.removeObject(handler.object.get(i));
                            int add = rand.nextInt(10);
                            score = score + add;
                            time = time + add;
                            total_waktu = total_waktu + add;
                            break;
                        }
                    }                    
                }
            }
        }
    }
    
    public static boolean checkCollision(GameObject player, GameObject item){
        boolean result = false;
        
        int sizePlayer = 50;
        int sizeItem = 20;
        
        int playerLeft = player.x;
        int playerRight = player.x + sizePlayer;
        int playerTop = player.y;
        int playerBottom = player.y + sizePlayer;
        
        int itemLeft = item.x;
        int itemRight = item.x + sizeItem;
        int itemTop = item.y;
        int itemBottom = item.y + sizeItem;
        
        if((playerRight > itemLeft ) &&
        (playerLeft < itemRight) &&
        (itemBottom > playerTop) &&
        (itemTop < playerBottom)
        ){
            result = true;
        }
        
        return result;
    }
    
    private void render(){
        BufferStrategy bs = this.getBufferStrategy();
        if(bs == null){
            this.createBufferStrategy(3);
            return;
        }
        
        Graphics g = bs.getDrawGraphics();
        
        g.setColor(Color.decode("#F1f3f3"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        if(gameState ==  STATE.Game){
            if(mode.equals("1")){
                if(handler.EmptyIs(handler.object)){
                    handler.addObject(new Items(rand.nextInt(WIDTH-60),rand.nextInt(HEIGHT-80),ID.Item));           
                    handler.addObject(new Items(rand.nextInt(WIDTH-60),rand.nextInt(HEIGHT-80),ID.Item));
                    handler.addObject(new Items(rand.nextInt(WIDTH-60),rand.nextInt(HEIGHT-80),ID.Item));
                }
            }
            else{
                if(handler.EmptyIs2(handler.object)){
                    handler.addObject(new Items(rand.nextInt(WIDTH-60),rand.nextInt(HEIGHT-80),ID.Item));           
                    handler.addObject(new Items(rand.nextInt(WIDTH-60),rand.nextInt(HEIGHT-80),ID.Item));
                    handler.addObject(new Items(rand.nextInt(WIDTH-60),rand.nextInt(HEIGHT-80),ID.Item));
                }
            }
            
            handler.render(g);
            
            Font currentFont = g.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() * 1.4F);
            g.setFont(newFont);

            g.setColor(Color.BLACK);
            g.drawString("Score: " +Integer.toString(score), 20, 20);

            g.setColor(Color.BLACK);
            g.drawString("Time: " +Integer.toString(time), WIDTH-120, 20);
        }else{
            Font currentFont = g.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() * 3F);
            g.setFont(newFont);

            g.setColor(Color.BLACK);
            g.drawString("GAME OVER", WIDTH/2 - 120, HEIGHT/2 - 30);

            currentFont = g.getFont();
            
            Font newScoreFont = currentFont.deriveFont(currentFont.getSize() * 0.5F);
            g.setFont(newScoreFont);

            g.setColor(Color.BLACK);
            g.drawString("Score: " +Integer.toString(score), WIDTH/2 - 50, HEIGHT/2 - 10);
            
            g.setColor(Color.BLACK);
            g.drawString("Press Space to Continue", WIDTH/2 - 100, HEIGHT/2 + 30);
        }
                
        g.dispose();
        bs.show();
    }
    
    public static int clamp(int var, int min, int max){
        if(var >= max){
            return var = max;
        }else if(var <= min){
            return var = min;
        }else{
            return var;
        }
    }
    
    public void close(){
        running = false;
        if (bgMusic != null) {
            bgMusic.stop();
        }
        window.CloseWindow();
    }
    
    public Clip playSound(String filename) {
        try {
            // Open an audio input stream.
            URL url = this.getClass().getResource(filename);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            // Get a sound clip resource.
            Clip clip = AudioSystem.getClip();
            // Open audio clip and load samples from the audio input stream.
            clip.open(audioIn);
            clip.start();
            return clip;
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void end(){
        dbConnection dbcon = new dbConnection();
        ResultSet result = dbcon.cekData(username);
        try {
            if(result.next()){
                if(result.getString("Username").equals(username)){
                    if(result.getInt("Score") < score){
                        dbcon.updateData(username, score,total_waktu);
                    }
                }
            } else {
                int addData = dbcon.addData(username, score,total_waktu);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
}