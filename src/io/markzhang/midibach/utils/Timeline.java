package io.markzhang.midibach.utils;

import io.markzhang.midibach.models.Note;
import io.markzhang.midibach.utils.intervaltree.IntervalTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

public class Timeline {

    private ConcurrentSkipListMap<Long, Note> notesTimeline = new ConcurrentSkipListMap<>(); // startTime, Note
    private IntervalTree<Note> intervalTree = new IntervalTree<>();

    public Timeline () {

    }

    public Note ceilNote(long startTime) {
        return notesTimeline.ceilingEntry(startTime).getValue();
    }

    public Collection<Note> tail(long startTime) {
        return notesTimeline.tailMap(startTime).values();
    }

    public void add(Note note) {
        long start = note.getStartTime();
        long end = note.getEndTime();
        while (notesTimeline.containsKey(start)) {
            start += 1;
        }
        notesTimeline.put(start, note);
    }

    public IntervalTree<Note> getIntervalTree() {
        return intervalTree;
    }

    public boolean contains(long startTime) {
        return notesTimeline.containsKey(startTime);
    }

    public Note get(long startTime) {
        return notesTimeline.get(startTime);
    }

    public Collection<Note> getNotes() {
        return notesTimeline.values();
    }

    public void clear(){
        notesTimeline.clear();
        intervalTree = new IntervalTree<>();
    }

    public Collection<Note> getContains(long ts) {
        List<Note> notes = new ArrayList<>();
        intervalTree.overlappers(new Note(ts-1,ts+1,-1,-1)).forEachRemaining(notes::add);
        return notes;
    }

}
