package io.markzhang.midibach.utils;

import io.markzhang.midibach.models.Note;

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

    public Collection<Note> getNotes() {
        return notesTimeline.values();
    }

    public void clear(){
        notesTimeline.clear();
    }

}
