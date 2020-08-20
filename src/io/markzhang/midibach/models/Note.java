package io.markzhang.midibach.models;

import io.markzhang.midibach.utils.intervaltree.Interval;
import javafx.util.Pair;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

public class Note implements Interval {
    private long startTime, endTime;
    private int noteVal;
    private int intensity;

    public Note(long startTime, long endTime, int noteVal, int intensity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.noteVal = noteVal;
        this.intensity = intensity;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getNoteVal() {
        return noteVal;
    }

    public void setNoteVal(int noteVal) {
        this.noteVal = noteVal;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public Pair<MidiEvent, MidiEvent> toMidiEvents(int channel, long timeOffset) {
        MidiEvent startEvent = null, endEvent = null;
        try {
            ShortMessage startMsg = new ShortMessage();
            startMsg.setMessage(144, channel, noteVal, intensity);
            startEvent = new MidiEvent(startMsg, (int)(((startTime)+timeOffset)/1000));

            ShortMessage endMsg = new ShortMessage();
            endMsg.setMessage(128, channel, noteVal, 64);
            endEvent = new MidiEvent(endMsg, (int)(((endTime)+timeOffset)/1000));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Pair<>(startEvent, endEvent);
    }

    @Override
    public String toString() {
        return "Note{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", noteVal=" + noteVal +
                ", intensity=" + intensity +
                '}';
    }

    @Override
    public long start() {
        return startTime;
    }

    @Override
    public long end() {
        return endTime;
    }
}
