package io.markzhang.midibach.gui;

import io.markzhang.midibach.MidiBach;
import io.markzhang.midibach.models.Note;
import io.markzhang.midibach.utils.Timeline;
import javafx.util.Pair;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizerFrame extends JFrame {

    private boolean recording = false;
    private long recordStart = Long.MAX_VALUE;

    public VisualizerFrame (MidiBach main) {
        setTitle("MidiBach: Visualizer");
        setLayout(new BorderLayout());
        JPanel visualPanel = new VisualizerPanel(main);
        add(visualPanel, BorderLayout.CENTER);
        JButton record = new JButton("Start Recording");
        record.addActionListener(e -> {
            if (recording) {
                record.setText("Start Recording");
                // Creating a sequence.
                Sequence sequence = null;
                try {
                    sequence = new Sequence(Sequence.PPQ, 125);
                } catch (InvalidMidiDataException invalidMidiDataException) {
                    invalidMidiDataException.printStackTrace();
                }
                if (sequence != null) {
                    Track track = sequence.createTrack();
                    long offset = main.getTimeline().ceilNote(recordStart).getStartTime();

                    MetaMessage mt = new MetaMessage();
                    try {
                        mt.setMessage(0x51,
                                Arrays.copyOfRange(
                                        ByteBuffer.allocate(4)
                                                  .order(ByteOrder.BIG_ENDIAN)
                                                  .putInt(125000).array(),
                                        1,4),
                                3);
                        track.add(new MidiEvent(mt, 0L));
                    } catch (InvalidMidiDataException invalidMidiDataException) {
                        invalidMidiDataException.printStackTrace();
                    }

                    for (Note note : main.getTimeline().tail(recordStart)) {
                        Pair<MidiEvent, MidiEvent> pair = note.toMidiEvents(1,-offset);
                        track.add(pair.getKey());
                        track.add(pair.getValue());
                        System.out.println(pair.getKey().getTick() + " " + pair.getValue().getTick());
                    }
                    try {
                        MidiSystem.write(sequence, 0, new File(System.currentTimeMillis()+".mid"));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                recordStart = Long.MAX_VALUE;
            } else {
                record.setText("Stop Recording");
                recordStart = System.nanoTime() / 1000;
            }
            recording = !recording;
        });
        JPanel buttonPane = new JPanel(new FlowLayout());
        buttonPane.add(record);
        JButton select = new JButton("Select File");
        select.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                main.getPlaybackTL().clear();
                Sequence sequence = null;
                try {
                    sequence = MidiSystem.getSequence(selectedFile);

                    double microsecPerTick = sequence.getMicrosecondLength() * 1.0 / sequence.getTickLength();
                    long startTime = System.nanoTime()/1000 + 5*1000000;
                    ConcurrentHashMap<Integer, Note> pressedNotes = new ConcurrentHashMap<>();
                    for (Track track : sequence.getTracks()) {
                        long tick = 0;
                        for (int i = 0; i < track.size(); i++) {
                            MidiEvent midiEvent = track.get(i);
                            MidiMessage msg = midiEvent.getMessage();
                            long timeStamp = startTime+(long)microsecPerTick*midiEvent.getTick();

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
                                    main.getPlaybackTL().add(note);
                                } else {
                                    Note note = pressedNotes.get(noteVal);
                                    if (note == null) return;
                                    note.setEndTime(timeStamp);
                                    pressedNotes.remove(noteVal);
                                }
                            }
                        }
                    }
                    Sequencer sequencer = MidiSystem.getSequencer();
                    sequencer.setSequence(sequence);
                    sequencer.open();
                    long timeDelta = System.nanoTime()/1000 - (startTime - 5*1000000);
                    timeDelta /= 1000;
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    sequencer.start();
                                }
                            },
                            5000-timeDelta
                    );

                } catch (InvalidMidiDataException | IOException | MidiUnavailableException invalidMidiDataException) {
                    invalidMidiDataException.printStackTrace();
                }
            }
        });
        buttonPane.add(select);
        add(buttonPane, BorderLayout.PAGE_END);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        Timer timer = new Timer(30, e -> repaint());
        timer.start();
    }
}
