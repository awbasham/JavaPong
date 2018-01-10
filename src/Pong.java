import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class Pong extends JPanel implements KeyListener {
    private int WIDTH, HEIGHT;
    private JFrame frame;
    private Bar userBar, aiBar;
    private Ball gameBall;
    private boolean isMovingUp, isMovingDown;
    private long lastBallVelocitySwitchTime = 0;
    private long lastAudioClipPlayed = 0;
    private long userHitTime = 0;
    private long aiReactionTime = 110;
    private float MAX_BOUNCE_ANGLE = (float)(85 * Math.PI / 180);
    private BufferedImage backgroundImage = null;
    private int userScoreInt, aiScoreInt;
    private AudioInputStream pong1, pong2, pong3;
    private Clip clip1, clip2, clip3;

    public Pong(int WIDTH, int HEIGHT) {
        this.WIDTH = WIDTH;
        this.HEIGHT = HEIGHT;

        userScoreInt = 0;
        aiScoreInt = 0;

        try {
            URL url = this.getClass().getClassLoader().getResource("pong1.wav");
            pong1 = AudioSystem.getAudioInputStream(url);
            url = this.getClass().getClassLoader().getResource("pong2.wav");
            pong2 = AudioSystem.getAudioInputStream(url);
            url = this.getClass().getClassLoader().getResource("pong3.wav");
            pong3 = AudioSystem.getAudioInputStream(url);

            clip1 = AudioSystem.getClip();
            clip2 = AudioSystem.getClip();
            clip3 = AudioSystem.getClip();
            clip1.open(pong1);
            clip2.open(pong2);
            clip3.open(pong3);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        frame = new JFrame("Pong in Java");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        //Adjust frame to compensate for title bar
        frame.getContentPane().setPreferredSize(new Dimension(WIDTH, HEIGHT));
        frame.pack();

        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.addKeyListener(this);

        frame.add(this);
        frame.setVisible(true);

        isMovingDown = false;
        isMovingUp = false;

        gameBall = new Ball(WIDTH / 2, HEIGHT / 2, WIDTH / 50, WIDTH / 50);
        //gameBall.x -= gameBall.width;
        //gameBall.y -= gameBall.height;

        userBar = new Bar(WIDTH / 50f, HEIGHT * .1f);
        userBar.x = WIDTH / 8f;
        userBar.y = HEIGHT / 2f - userBar.height / 2;

        aiBar = new Bar(WIDTH / 50f, HEIGHT * .1f);
        aiBar.x = WIDTH / 8f * 7;
        aiBar.y = HEIGHT / 2f - aiBar.height / 2;
        aiBar.speed = HEIGHT * 0.38f / 1000;
    }

    private class Bar {
        private float x, y, speed, width, height, halfHeight;

        public Bar(float width, float height) {
            this.width = width;
            this.height = height;
            this.halfHeight = height / 2;

            //45% of screen movement per second(in milliseconds)?
            this.speed = HEIGHT * 0.45f / 1000;
        }
    }

    private class Ball {
        private float x, y, xVel, yVel, speed, maxVel;
        private int width, height;

        public Ball(int x, int y, int width, int height) {
            this.width = width;
            this.height = height;
            this.x = x - width / 2;
            this.y = y - height / 2;

            //30% of screen width per second (in milliseconds)
            this.xVel = WIDTH * .4f / 1000;
            this.speed = HEIGHT * 0.4f / 1000;
            this.maxVel = WIDTH * .65f / 1000;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);

        g.setFont(new Font("TimesRoman", Font.PLAIN, HEIGHT / 15));
        g.drawString(String.valueOf(userScoreInt), WIDTH / 2 - WIDTH / 4, HEIGHT / 10);
        g.drawString(String.valueOf(aiScoreInt), WIDTH / 2 + WIDTH / 4, HEIGHT / 10);

        if(backgroundImage == null) {
            backgroundImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics backgroundGraphics = backgroundImage.getGraphics();

            int centerBarCount = 20;
            int centerBarWidth = 2;
            int centerBarHeight = HEIGHT / 40;
            int centerBarGap = (HEIGHT - centerBarHeight*centerBarCount) / (centerBarCount);
            for(int i = 0; i < centerBarCount; i++) {
                backgroundGraphics.fillRect(WIDTH / 2, i*(centerBarHeight + centerBarGap), centerBarWidth, centerBarHeight);
            }
            backgroundGraphics.dispose();
        } else {
            g.drawImage(backgroundImage, 0, 0, frame);
        }

        Graphics2D gg = (Graphics2D) g;
        gg.setColor(Color.WHITE);
        Rectangle2D ball = new Rectangle2D.Float(gameBall.x, gameBall.y, gameBall.width, gameBall.height);
        gg.fill(ball);

        Rectangle2D barUser = new Rectangle2D.Float(userBar.x, userBar.y, userBar.width, userBar.height);
        gg.fill(barUser);

        Rectangle2D barAI = new Rectangle2D.Float(aiBar.x, aiBar.y, aiBar.width, aiBar.height);
        gg.fill(barAI);
    }

    private void resetBall() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
        }
        gameBall = new Ball(WIDTH / 2, HEIGHT / 2, WIDTH / 50, WIDTH / 50);
        userBar.y = HEIGHT / 2f - userBar.height / 2;
        aiBar.y = HEIGHT / 2f - aiBar.height / 2;
    }

    //Ball angle calculations from https://gamedev.stackexchange.com/questions/4253/in-pong-how-do-you-calculate-the-balls-direction-when-it-bounces-off-the-paddl
    private synchronized void updateBallPosition(long deltaTime) {
        if(gameBall.x <= 0) {
            if(System.currentTimeMillis() - lastAudioClipPlayed >= 100) {
                clip3.setFramePosition(0);
                clip3.loop(0);
                lastAudioClipPlayed = System.currentTimeMillis();
            }
            aiScoreInt++;
            resetBall();
        } else if(gameBall.x >= WIDTH) {
            if(System.currentTimeMillis() - lastAudioClipPlayed >= 100) {
                clip3.setFramePosition(0);
                clip3.loop(0);
                lastAudioClipPlayed = System.currentTimeMillis();
            }
            userScoreInt++;
            resetBall();
        }
        if(gameBall.y <= 0 || gameBall.y + gameBall.height >= HEIGHT) {
            if(gameBall.y < 0) {
                gameBall.y = 0;
            } else if(gameBall.y + gameBall.height > HEIGHT) {
                gameBall.y = HEIGHT - gameBall.height;
            }
            gameBall.yVel *= -1;
            if(System.currentTimeMillis() - lastAudioClipPlayed >= 100) {
                clip2.setFramePosition(0);
                clip2.loop(0);
                lastAudioClipPlayed = System.currentTimeMillis();
            }
        }

        if(((gameBall.x >= userBar.x && gameBall.x <= userBar.x + userBar.width) && (gameBall.y + gameBall.height >= userBar.y && gameBall.y <= userBar.y + userBar.height))
                && System.currentTimeMillis() - lastBallVelocitySwitchTime >= 100) {

            userHitTime = System.currentTimeMillis();

            clip1.setFramePosition(0);
            clip1.loop(0);

            //Reverse ball X vel
            gameBall.xVel *= -1;
            //Slightly increase xVel by 2 percent of screen width for each hit
            if(gameBall.xVel < gameBall.maxVel) {
                gameBall.xVel += WIDTH * .02f / 1000;
            } else {
                gameBall.xVel = gameBall.maxVel;
            }
            System.out.println("Ball xVel: " + gameBall.xVel);
            //Distance from ballY to center of user bar Y
            float relativeYIntersect = (userBar.y + (userBar.halfHeight)) - (gameBall.y + gameBall.height / 2);
            float normalizedYIntersect = (relativeYIntersect/(userBar.halfHeight));
            float bounceAngle = normalizedYIntersect * MAX_BOUNCE_ANGLE;
            //Update Ball Y vel based on bounceAngle
            gameBall.yVel = gameBall.speed * (float)-Math.sin(bounceAngle);

            lastBallVelocitySwitchTime = System.currentTimeMillis();
        } else if(((gameBall.x >= aiBar.x && gameBall.x <= aiBar.x + userBar.width) && (gameBall.y + gameBall.height >= aiBar.y && gameBall.y <= aiBar.y + aiBar.height))
                && System.currentTimeMillis() - lastBallVelocitySwitchTime >= 100) {

            clip1.setFramePosition(0);
            clip1.loop(0);

            gameBall.xVel *= -1;
            //Slightly increase xVel by 2 percent of screen width for each hit
            if(gameBall.xVel * -1 < gameBall.maxVel) {
                gameBall.xVel -= WIDTH * .02f / 1000;
            } else {
                gameBall.xVel = gameBall.maxVel * -1;
            }
            //Distance from ballY to center of user bar Y
            float relativeYIntersect = (aiBar.y + (aiBar.halfHeight)) - (gameBall.y + gameBall.height / 2);
            float normalizedYIntersect = (relativeYIntersect/(aiBar.halfHeight));
            float bounceAngle = normalizedYIntersect * MAX_BOUNCE_ANGLE;
            //Update Ball Y vel based on bounceAngle
            gameBall.yVel = gameBall.speed * (float)-Math.sin(bounceAngle);

            lastBallVelocitySwitchTime = System.currentTimeMillis();
        }

        gameBall.x += deltaTime * gameBall.xVel;
        gameBall.y += deltaTime * gameBall.yVel;
    }

    private void updateUserBarPosition(long deltaTime) {
        if(isMovingUp) {
            if(userBar.y > 0) {
                userBar.y -= userBar.speed * deltaTime;
            }
        } else if(isMovingDown) {
            if(userBar.y + userBar.height < HEIGHT) {
                userBar.y += userBar.speed * deltaTime;
            }
        }
    }

    private void updateAiBarPosition(long deltaTime) {
        if(System.currentTimeMillis() - userHitTime >= aiReactionTime) {
            if (aiBar.y + (aiBar.halfHeight) < gameBall.y + gameBall.height / 2 && (aiBar.y + aiBar.height < HEIGHT)) {
                aiBar.y += aiBar.speed * deltaTime;
            } else if (aiBar.y + (aiBar.halfHeight) > gameBall.y + gameBall.height / 2 && aiBar.y > 0) {
                aiBar.y -= aiBar.speed * deltaTime;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //No need
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            isMovingUp = true;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            isMovingDown = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            isMovingUp = false;
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            isMovingDown = false;
        }
    }

    private void updateGame(long deltaTime) {
        updateBallPosition(deltaTime);
        updateAiBarPosition(deltaTime);
        updateUserBarPosition(deltaTime);
        frame.repaint();
    }

    public static void main(String[] args) {
        long currentTime = System.currentTimeMillis();
        long deltaT = 0;
        Pong pong = new Pong(1200, 1000);

        while (true) {
            deltaT = System.currentTimeMillis() - currentTime;
            pong.updateGame(deltaT);
            currentTime += deltaT;
        }

    }
}
