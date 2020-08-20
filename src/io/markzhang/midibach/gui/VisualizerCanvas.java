package io.markzhang.midibach.gui;

import io.markzhang.midibach.MidiBach;
import io.markzhang.midibach.models.Note;
import javafx.util.Pair;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class VisualizerCanvas extends Canvas{

    private MidiBach instance;
    private Color pressedBlackKey = new Color(7, 151, 85);
    private Color pressedWhiteKey = Color.GREEN;

    private BufferStrategy bs;

    public VisualizerCanvas(MidiBach instance) {
        this.instance = instance;
        setPreferredSize(new Dimension(1500,900));
        setBackground(Color.BLACK);
        setIgnoreRepaint(true);
        instance.startReading();
    }

    // c# -10% to the left of C and D
    // d# +10% to the right of D and E
    // f#-0.14, 0, +0.14
    double whiteKeyWidth;
    double blackKeyWidth;
    double whiteKeyHeight;
    double blackKeyHeight;
    double widthOffset;
    HashMap<Integer, Integer> blackKeyMap = new HashMap<Integer, Integer>(){{
        put(1, 0);
        put(3, 1);
        put(6, 2);
        put(8, 3);
        put(10, 4);
    }};
    HashMap<Integer, Integer> whiteKeyMap = new HashMap<Integer, Integer>(){{
        put(0, 0);
        put(2, 1);
        put(4, 2);
        put(5, 3);
        put(7, 4);
        put(9, 5);
        put(11, 6);
    }};
    double[] blackKeyRelLoc = new double[]{-0.1, 0.1, -0.14, 0, 0.14};
    private Pair<Double, Boolean> getNoteX(int noteVal) {
        int octave = (noteVal / 12) - 1;
        int relNoteVal = noteVal % 12;
        if (blackKeyMap.containsKey(relNoteVal)) {
            Pair<Double, Boolean> rightWhiteKeyX = getNoteX(noteVal + 1);
            return new Pair<>(rightWhiteKeyX.getKey() + whiteKeyWidth*blackKeyRelLoc[blackKeyMap.get(relNoteVal)]-blackKeyWidth/2,false);
        } else {
           return new Pair<>(octave*whiteKeyWidth*7+widthOffset+whiteKeyMap.get(relNoteVal)*whiteKeyWidth, true);
        }
    }

    public void render() {
        if (bs == null) {
            createBufferStrategy(2);
            bs = getBufferStrategy();
        }
        // Get hold of a graphics context for the accelerated
        // surface and blank it out
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.setColor(Color.black);
        g.fillRect(0,0,getWidth(),getHeight());


        //-----------------------------------------------------------------------------------------
        long bottomTime = System.nanoTime() / 1000;
        long topTime = bottomTime - instance.getFallingMicroSeconds();
        final Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        whiteKeyWidth = getWidth() / 52.0;
        blackKeyWidth = whiteKeyWidth * 0.612;
        whiteKeyHeight = whiteKeyWidth * 6.38;
        blackKeyHeight = whiteKeyHeight * 0.666;
        widthOffset = -whiteKeyWidth * 5;

        for (int key = 21; key <= 108; key++) {
            Pair<Double, Boolean> pair = getNoteX(key);
            double xLoc = pair.getKey();
            boolean white = pair.getValue();
            if (white) {
                g2d.setColor(Color.WHITE);
                g2d.fill(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,whiteKeyWidth, whiteKeyHeight));
                g2d.setColor(Color.BLACK);
                g2d.draw(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,whiteKeyWidth, whiteKeyHeight));
            }
        }
        Collection<Note> pressed = instance.getPlaybackTL().getContains(bottomTime);
        Collection<Note> curPressed = instance.getPressedNotes().values();
        pressed.addAll(curPressed);
        for (Note n : pressed) {
            Pair<Double, Boolean> pair = getNoteX(n.getNoteVal());
            double xLoc = pair.getKey();
            boolean white = pair.getValue();
            if(white){
                g2d.setColor(pressedWhiteKey);
                g2d.fill(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,whiteKeyWidth, whiteKeyHeight));
                g2d.setColor(Color.BLACK);
                g2d.draw(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,whiteKeyWidth, whiteKeyHeight));
            }
        }

        for (int key = 21; key <= 108; key++){
            Pair<Double, Boolean> pair = getNoteX(key);
            double xLoc = pair.getKey();
            boolean white = pair.getValue();
            if (!white) {
                g2d.setColor(Color.BLACK);
                g2d.fill(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,blackKeyWidth, blackKeyHeight));
                g2d.setColor(Color.BLACK);
                g2d.draw(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,blackKeyWidth, blackKeyHeight));
            }
        }

        for (Note n : pressed) {
            Pair<Double, Boolean> pair = getNoteX(n.getNoteVal());
            double xLoc = pair.getKey();
            boolean white = pair.getValue();
            if(!white){
                g2d.setColor(pressedBlackKey);
                g2d.fill(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,blackKeyWidth, blackKeyHeight));
                g2d.setColor(Color.BLACK);
                g2d.draw(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,blackKeyWidth, blackKeyHeight));
            }
        }

        List<Note> curNotes = new ArrayList<>();
        instance.getTimeline().getIntervalTree().overlappers(new Note(topTime, bottomTime+instance.getFallingMicroSeconds(),-1,-1)).forEachRemaining(curNotes::add);
        curNotes.addAll(curPressed);
        for (Note v : curNotes) {
            Pair<Double, Boolean> pair = getNoteX(v.getNoteVal());
            double width = pair.getValue() ? whiteKeyWidth : blackKeyWidth;
            double xLoc = pair.getKey() + width*0.18;
            width = width*0.7;
            Color c = pair.getValue() ? pressedWhiteKey : pressedBlackKey;
            g2d.setColor(c);
            double viewHeight = getHeight() - whiteKeyHeight;
            double yLoc = (double) (v.getStartTime() - topTime) / (bottomTime - topTime) * viewHeight;
            double yEnd = (double) (v.getEndTime() - topTime) / (bottomTime - topTime) * viewHeight;
            if (v.getEndTime() == Long.MAX_VALUE) yEnd = viewHeight;
            g2d.fillRoundRect((int)xLoc, (int)yLoc, (int)width, (int)(yEnd-yLoc),10,15);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect((int)xLoc, (int)yLoc, (int)width, (int)(yEnd-yLoc),10,15);
        }

        Iterator<Note> i = instance.getPlaybackTL().getIntervalTree().overlappers(new Note(topTime, bottomTime+instance.getFallingMicroSeconds(),-1,-1));
        while (i.hasNext()) {
            Note v = i.next();
            Pair<Double, Boolean> pair = getNoteX(v.getNoteVal());
            double width = pair.getValue() ? whiteKeyWidth : blackKeyWidth;
            double xLoc = pair.getKey() + width*0.18;
            width = width*0.7;
            Color c = pair.getValue() ? pressedWhiteKey : pressedBlackKey;
            g2d.setColor(c);
            double viewHeight = getHeight() - whiteKeyHeight;
            double yTop = (double) (topTime - (v.getEndTime() - 2*instance.getFallingMicroSeconds())) / (bottomTime - topTime) * viewHeight;
            double yBottom = (double) (topTime - (v.getStartTime() - 2*instance.getFallingMicroSeconds())) / (bottomTime - topTime) * viewHeight;
            yBottom = Math.min(yBottom, viewHeight);
            g2d.fillRoundRect((int)xLoc, (int)yTop, (int)width, (int)(yBottom-yTop),10,15);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect((int)xLoc, (int)yTop, (int)width, (int)(yBottom-yTop),10,15);
        }

        //-----------------------------------------------------------------------------------------

        g.dispose();
        bs.show();
    }
    /**
     * FPS counter integer
     */
    private int fpsCount = 0;

    public void run() {

        //Maximum 60fps
        double delta = 1.0/70.0;
        // convert the time to seconds
        double nextTime = (double)System.nanoTime() / 1000000000.0;

        /*
         * This variable is used to ensure that the rendering and
         * the game logic doesn't go desync 0.5 second with each other
         */
        double maxTimeDiff = 0.5;

        //this variable keep track of how many frames are skipped
        int skippedFrames = 1;

        //this variable determines the maximum frames that can be skipped
        int maxSkippedFrames = 5;

        //the current fps
        int fps = 0;

        //stores the current system nano time
        long preTime = System.nanoTime();

        //while the game runs
        while(true){

            //get the current time in seconds
            double currTime = (double) System.nanoTime() / 1000000000.0;

            //If the loop is fallen too much behind, render ASAP to prevent non-updating screen
            if ((currTime - nextTime) > maxTimeDiff) nextTime = currTime;

            //if we are a
            if (currTime >= nextTime) {//if the loop is behind or at the time to render

                // assign the time for the next update
                nextTime += delta;

                /*
                 * Render if the program got the game logic done early
                 * OR
                 * If the game logic is too slow that the loop is already
                 * 5 frames behind
                 */
                if ((currTime < nextTime) || (skippedFrames > maxSkippedFrames)) {
                    render();
                    fps++;
                    skippedFrames = 1;
                } else {
                    skippedFrames++;
                }
            } else {// if the loop is generating too fast make it sleep for the program to catch up
                // calculate the time to sleep
                int sleepTime = (int) (1000.0 * (nextTime - currTime));
                // sanity check
                if (sleepTime > 0) {
                    // sleep until the next update
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (((double)(System.nanoTime()-preTime))/1000000000.0>=1){
                preTime = System.nanoTime();
                fpsCount = fps;
                fps=0;
            }
        }
    }

}
