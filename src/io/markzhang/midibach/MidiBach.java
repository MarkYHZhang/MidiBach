package io.markzhang.midibach;

import io.markzhang.midibach.gui.VisualizerFrame;
import io.markzhang.midibach.models.Note;
import io.markzhang.midibach.utils.Timeline;

import javax.sound.midi.*;
import java.util.concurrent.ConcurrentHashMap;

public class MidiBach {

    private ConcurrentHashMap<Integer, Note> pressedNotes = new ConcurrentHashMap<>(); // noteVal, Note
    private Timeline timeline = new Timeline();
    private long computerPianoTimeDeltaMicro;

    public void startReading() {
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);
                System.out.println(info);
                for (Transmitter t : device.getTransmitters()) {
                    t.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));
                }
                device.getTransmitter().setReceiver(getNewReceiver(device.getDeviceInfo().toString()));
                device.open();
                System.out.println(device.getDeviceInfo() + " Was Opened");
            } catch (MidiUnavailableException e) {}
        }
        computerPianoTimeDeltaMicro = System.nanoTime() / 1000;
    }

    public MidiInputReceiver getNewReceiver(String name){
        return new MidiInputReceiver(name);
    }

    public class MidiInputReceiver implements Receiver {
        public String name;
        public MidiInputReceiver(String name) {
            this.name = name;
        }
        public void send(MidiMessage msg, long timeStamp) {
            timeStamp = System.nanoTime() / 1000;
            setDelta(0);
            byte[] rawData = msg.getMessage();
            int status = msg.getStatus();
            if (status == 176 && rawData[1] == 64) { // indicates pedal
                // TODO: this stuff later
                int intensity = rawData[2] & 0xFF;
            } else if (128 <= status && status <= 159) {
                boolean down = status >= 144;
                int noteVal = rawData[1] & 0xFF;
                int intensity = rawData[2] & 0xFF;
                if (down) {
                    Note note = new Note(timeStamp, Long.MAX_VALUE, noteVal, intensity);
                    if (pressedNotes.containsKey(noteVal)){
                        pressedNotes.get(noteVal).setEndTime(timeStamp);
                    }
                    pressedNotes.put(noteVal, note);
                    timeline.add(note);
                } else {
                    Note note = pressedNotes.get(noteVal);
                    if (note == null) return;
                    note.setEndTime(timeStamp);
                    pressedNotes.remove(noteVal);
                }
            }
        }
        public void close() {}
    }

    public static void main(String[] args) {
        MidiBach main = new MidiBach();
        new VisualizerFrame(main);
    }

    public ConcurrentHashMap<Integer, Note> getPressedNotes() {
        return pressedNotes;
    }

    public Timeline getTimeline() {
        return timeline;
    }

    public void setDelta(long l) {
        computerPianoTimeDeltaMicro = l;
    }

    public long getComputerPianoTimeDeltaMicro() {
        return computerPianoTimeDeltaMicro;
    }
}