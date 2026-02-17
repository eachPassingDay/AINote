package com.ainote.repository;

import com.ainote.entity.Note;
import java.util.List;
import java.util.Optional;

public interface NoteRepository {
    Note save(Note note);

    Optional<Note> findById(String id);

    List<Note> findAll();
}
