package io.markzhang.midibach.gui;

import io.markzhang.midibach.MidiBach;
import io.markzhang.midibach.models.Note;
import javafx.util.Pair;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
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
                recordStart = System.nanoTime() / 1000 - main.getComputerPianoTimeDeltaMicro();
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
                main.getTimeline().clear();
                Sequence sequence = null;
                try {
                    sequence = MidiSystem.getSequence(selectedFile);
                    Sequencer sequencer = MidiSystem.getSequencer();
                    sequencer.setSequence(sequence);
                    sequencer.getTransmitter().setReceiver(main.getNewReceiver("file"));
                    sequencer.open();
                    sequencer.start();
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
