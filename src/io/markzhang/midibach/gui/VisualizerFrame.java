package io.markzhang.midibach.gui;

import io.markzhang.midibach.MidiBach;
import io.markzhang.midibach.models.Note;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizerFrame extends JFrame {

    private boolean recording = false;
    private long lastClear = 0;
    private long recordStart = Long.MAX_VALUE;
    private Sequencer sequencer;

    public VisualizerFrame (MidiBach main) {
        setTitle("MidiBach: Visualizer");
        setLayout(new BorderLayout());
        JButton record = new JButton("Start Recording");
        record.addActionListener(e -> {
            if (recording) {
                record.setText("Start Recording");
                Sequence sequence = null;
                try {
                    sequence = new Sequence(Sequence.PPQ, 125);
                } catch (InvalidMidiDataException invalidMidiDataException) {
                    invalidMidiDataException.printStackTrace();
                }
                if (sequence != null && !main.getTimeline().tail(recordStart).isEmpty()) {
                    Track track = sequence.createTrack();
                    double offset = main.getTimeline().ceilNote(recordStart).getStartTime();

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
                    }
                    try {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

                        //set it to be a save dialog
                        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                        fileChooser.setSelectedFile(new File("mypiece.mid"));
                        fileChooser.setFileFilter(new FileNameExtensionFilter("MIDI file","mid"));

                        int result = fileChooser.showOpenDialog(this);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            File selectedFile = fileChooser.getSelectedFile();
                            String filename = selectedFile.toString();
                            if (!filename .endsWith(".mid"))
                                filename += ".mid";
                            MidiSystem.write(sequence, 0, new File(filename));
                        }
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
                try {
                    Sequence sequence = MidiSystem.getSequence(selectedFile);
                    sequencer = MidiSystem.getSequencer();
                    sequencer.setSequence(sequence);
                    double microsecPerTick = 0;
                    long lastTick = 0;
                    ConcurrentHashMap<Integer, Note> pressedNotes = new ConcurrentHashMap<>();
                    ArrayList<MidiEvent> midiEvents = new ArrayList<>();
                    for (Track track : sequence.getTracks()) {
                        for (int i = 0; i < track.size(); i++) {
                            MidiEvent midiEvent = track.get(i);
                            midiEvents.add(midiEvent);
                        }
                    }
                    // sort based on ticks, because midi msgs might not be sorted based on increasing ticks order
                    midiEvents.sort((o1, o2) -> {
                        if (o1.getTick() < o2.getTick()) {
                            return -1;
                        } else if (o1.getTick() > o2.getTick()) {
                            return 1;
                        }
                        return 0;
                    });
                    long startTime = System.nanoTime()/1000 + main.getFallingMicroSeconds();
                    long timeStamp = startTime;
                    for (MidiEvent midiEvent : midiEvents) {
                        MidiMessage msg = midiEvent.getMessage();
//                        System.out.print(midiEvent.getTick() + " ");
//                        for (byte b : msg.getMessage()) {
//                            System.out.print((b&0xFF) +" ");
//                        }System.out.println();
                        timeStamp += Math.round(microsecPerTick * (midiEvent.getTick() - lastTick));
                        lastTick = midiEvent.getTick();
//                        System.out.println(microsecPerTick * (midiEvent.getTick() - lastTick));
                        MidiBach.processMidiMsg(timeStamp, msg, main.getPlaybackTL(), pressedNotes);
                        if (msg.getStatus() == 255) { //Meta Message
                            byte[] rawData = msg.getMessage();
                            if ((rawData[1] & 0xFF) == 81) { // tempo change
                                double microsecondsPerQuarterNote = ((rawData[3] & 0xFF) << 16)
                                        | ((rawData[4] & 0xFF) << 8)
                                        | (rawData[5] & 0xFF);
                                microsecPerTick = getMicrosecondsPerTick(microsecondsPerQuarterNote);
                            }
                        }
                    }
                    sequencer.open();
                    long timeDelta = System.nanoTime()/1000 - (startTime - main.getFallingMicroSeconds());
                    timeDelta /= 1000;
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    if (Math.abs(lastClear - System.nanoTime()) <= (long)5*1000000000) return;
                                    sequencer.start();
                                }
                            },
                            main.getFallingMicroSeconds()/1000-timeDelta
                    );

                } catch (InvalidMidiDataException | IOException | MidiUnavailableException invalidMidiDataException) {
                    invalidMidiDataException.printStackTrace();
                }
            }
        });
        buttonPane.add(select);
        JButton fallingTime = new JButton("Adjust Falling Speed");
        fallingTime.addActionListener(e -> {
            try {
                String raw = JOptionPane.showInputDialog("Input seconds to fall");
                if (raw !=null &&!raw.trim().isEmpty()) {
                    int seconds_to_fall = Integer.parseInt(raw);
                    main.setFallingMicroSeconds(seconds_to_fall * 1000000);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please input integral values only!");
            }
        });
        buttonPane.add(fallingTime);
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> {
            main.getTimeline().clear();
            main.getPlaybackTL().clear();
            main.getPressedNotes().clear();
            lastClear = System.nanoTime();
            if (sequencer!=null && sequencer.isRunning()) {
                sequencer.stop();
            }
        });
        buttonPane.add(clear);
        add(buttonPane, BorderLayout.PAGE_END);
        VisualizerCanvas visualPanel = new VisualizerCanvas(main);
        add(visualPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        visualPanel.run();
    }

    private double getMicrosecondsPerTick(double microsecondsPerQuarterNote) {
        double microsecondsPerTick;
        float divisionType = sequencer.getSequence().getDivisionType();
        if (divisionType == Sequence.PPQ) {
            // (us/qn) / (ticks/qn) = us/ticks
            microsecondsPerTick = (microsecondsPerQuarterNote * 1.0 / sequencer.getSequence().getResolution());
        } else { // SMPTE
            double framesPerSecond;
            if (divisionType == Sequence.SMPTE_24) {
                framesPerSecond = 24;
            } else if (divisionType == Sequence.SMPTE_25) {
                framesPerSecond = 25;
            } else if (divisionType == Sequence.SMPTE_30) {
                framesPerSecond = 30;
            } else if (divisionType == Sequence.SMPTE_30DROP) {
                framesPerSecond = 29.97;
            } else {
                framesPerSecond = -1;
            }
            double ticksPerSecond = sequencer.getSequence().getResolution() * framesPerSecond;
            microsecondsPerTick = ((1.0 / ticksPerSecond) * 1000000);
        }
        return microsecondsPerTick;
    }
}
