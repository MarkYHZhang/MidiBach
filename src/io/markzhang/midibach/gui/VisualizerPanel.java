package io.markzhang.midibach.gui;

import io.markzhang.midibach.MidiBach;
import io.markzhang.midibach.models.Note;
import javafx.util.Pair;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizerPanel extends JPanel{

    private MidiBach instance;

    public VisualizerPanel(MidiBach instance) {
        this.instance = instance;
        setPreferredSize(new Dimension(1500,900));
        setBackground(Color.BLACK);
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        long bottomTime = System.nanoTime() / 1000;
        long topTime = bottomTime - 5 * 1000000;
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
        pressed.addAll(instance.getTimeline().getContains(bottomTime));
        for (Note n : pressed) {
            Pair<Double, Boolean> pair = getNoteX(n.getNoteVal());
            double xLoc = pair.getKey();
            boolean white = pair.getValue();
            if(white){
                g2d.setColor(Color.GREEN);
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
                g2d.setColor(Color.GREEN);
                g2d.fill(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,blackKeyWidth, blackKeyHeight));
                g2d.setColor(Color.BLACK);
                g2d.draw(new Rectangle2D.Double(xLoc,getHeight()-whiteKeyHeight,blackKeyWidth, blackKeyHeight));
            }
        }

        instance.getTimeline().getNotes().forEach(v -> {
            Pair<Double, Boolean> pair = getNoteX(v.getNoteVal());
            double width = pair.getValue() ? whiteKeyWidth : blackKeyWidth;
            double xLoc = pair.getKey() + width*0.18;
            width = width*0.7;
            g2d.setColor(Color.GREEN);
            double viewHeight = getHeight() - whiteKeyHeight;
            double yLoc = (double) (v.getStartTime() - topTime) / (bottomTime - topTime) * viewHeight;
            double yEnd = (double) (v.getEndTime() - topTime) / (bottomTime - topTime) * viewHeight;
            if (v.getEndTime() == Long.MAX_VALUE) yEnd = viewHeight;
            g2d.fillRoundRect((int)xLoc, (int)yLoc, (int)width, (int)(yEnd-yLoc),10,15);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect((int)xLoc, (int)yLoc, (int)width, (int)(yEnd-yLoc),10,15);
        });

        instance.getPlaybackTL().getNotes().forEach(v -> {
            Pair<Double, Boolean> pair = getNoteX(v.getNoteVal());
            double width = pair.getValue() ? whiteKeyWidth : blackKeyWidth;
            double xLoc = pair.getKey() + width*0.18;
            width = width*0.7;
            g2d.setColor(Color.GREEN);
            double viewHeight = getHeight() - whiteKeyHeight;
            double yTop = (double) (topTime - v.getEndTime() + 10*1000000) / (bottomTime - topTime) * viewHeight;
            double yBottom = (double) (topTime - v.getStartTime() + 10*1000000) / (bottomTime - topTime) * viewHeight;
            yBottom = Math.min(yBottom, viewHeight);
            g2d.fillRoundRect((int)xLoc, (int)yTop, (int)width, (int)(yBottom-yTop),10,15);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect((int)xLoc, (int)yTop, (int)width, (int)(yBottom-yTop),10,15);
        });

    }
}
