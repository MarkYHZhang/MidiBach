package io.markzhang.midibach.utils;

import io.markzhang.midibach.models.Note;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListMap;

public class Timeline {

    private ConcurrentSkipListMap<Long, Note> notesTimeline = new ConcurrentSkipListMap<>(); // startTime, Note

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
        while (notesTimeline.containsKey(start)) {
            start += 1;
        }
        notesTimeline.put(start, note);
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
    }

    public Collection<Note> getContains(long ts) {
        Collection<Note> notes = new ArrayList<>();
        for (Note note : notesTimeline.values()) {
            if (note==null) continue;
            if (note.getStartTime() <= ts && ts <= note.getEndTime()) {
                notes.add(note);
            }
        }
        return notes;
    }

}
