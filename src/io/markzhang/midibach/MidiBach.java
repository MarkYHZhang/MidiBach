package io.markzhang.midibach;

import io.markzhang.midibach.gui.VisualizerFrame;
import io.markzhang.midibach.models.Note;
import io.markzhang.midibach.utils.Timeline;

import javax.sound.midi.*;
import java.util.concurrent.ConcurrentHashMap;

public class MidiBach {

    private int fallingMicroSeconds = 5 * 1000000;

    private Timeline timeline = new Timeline();
    private Timeline playbackTL = new Timeline();
    private ConcurrentHashMap<Integer, Note> pressedNotes = new ConcurrentHashMap<>();

    public void startReading() {
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);
                System.out.println(info);
                for (Transmitter t : device.getTransmitters()) {
                    t.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString(),timeline));
                }
                device.getTransmitter().setReceiver(getNewReceiver(device.getDeviceInfo().toString(),timeline));
                device.open();
                System.out.println(device.getDeviceInfo() + " Was Opened");
            } catch (MidiUnavailableException e) {}
        }
    }

    public MidiInputReceiver getNewReceiver(String name, Timeline tl){
        return new MidiInputReceiver(name,tl);
    }

    public class MidiInputReceiver implements Receiver {
        public String name;
        private Timeline tl;
        public MidiInputReceiver(String name, Timeline tl) {
            this.tl = tl;
            this.name = name;
        }
        public void send(MidiMessage msg, long timeStamp) {
            timeStamp = System.nanoTime() / 1000;
            processMidiMsg(timeStamp, msg, tl, pressedNotes);
        }
        public void close() {}
    }

    public static boolean processMidiMsg(long timeStamp, MidiMessage msg, Timeline tll, ConcurrentHashMap<Integer, Note> pressedNotes) {
        byte[] rawData = msg.getMessage();
        int status = msg.getStatus();
        if (status == 176 && rawData[1] == 64) { // indicates pedal
            // TODO: this stuff later
            int intensity = rawData[2] & 0xFF;
        } else if (128 <= status && status <= 159) {
            boolean down = status >= 144;
            int noteVal = rawData[1] & 0xFF;
            int intensity = rawData[2] & 0xFF;
            if (down && !pressedNotes.containsKey(noteVal)) {
                Note note = new Note(timeStamp, Long.MAX_VALUE, noteVal, intensity);
                pressedNotes.put(noteVal, note);
                tll.add(note);
            } else {
                Note note = pressedNotes.get(noteVal);
                if (note == null) return false;
                note.setEndTime(timeStamp);
                pressedNotes.remove(noteVal);
                tll.getIntervalTree().insert(note);
            }
        } else {

        }

//        System.out.println();
        return false;
    }

    public static void main(String[] args) {
        MidiBach main = new MidiBach();
        new VisualizerFrame(main);
    }

    public Timeline getTimeline() {
        return timeline;
    }

    public Timeline getPlaybackTL() {
        return playbackTL;
    }

    public int getFallingMicroSeconds() {
        return fallingMicroSeconds;
    }

    public void setFallingMicroSeconds(int fallingMicroSeconds) {
        this.fallingMicroSeconds = fallingMicroSeconds;
    }

    public ConcurrentHashMap<Integer, Note> getPressedNotes() {
        return pressedNotes;
    }
}