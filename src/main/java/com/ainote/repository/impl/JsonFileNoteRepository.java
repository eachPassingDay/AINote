package com.ainote.repository.impl;

import com.ainote.entity.Note;
import com.ainote.repository.NoteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class JsonFileNoteRepository implements NoteRepository {

    private final Map<String, Note> noteCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Value("${note.db.path:notes_db.json}")
    private String dbPath;

    public JsonFileNoteRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() {
        File file = new File(dbPath);
        if (file.exists()) {
            try {
                List<Note> notes = objectMapper.readValue(file, new TypeReference<List<Note>>() {
                });
                notes.forEach(note -> noteCache.put(note.getId(), note));
            } catch (IOException e) {
                System.err.println("Failed to load notes from DB: " + e.getMessage());
            }
        }
    }

    private synchronized void persist() {
        try {
            objectMapper.writeValue(new File(dbPath), new ArrayList<>(noteCache.values()));
        } catch (IOException e) {
            System.err.println("Failed to persist notes to DB: " + e.getMessage());
        }
    }

    @Override
    public Note save(Note note) {
        if (note.getId() == null) {
            note.setId(java.util.UUID.randomUUID().toString());
            if (note.getCreatedAt() == null) {
                note.setCreatedAt(java.time.LocalDateTime.now());
            }
        }
        note.setUpdatedAt(java.time.LocalDateTime.now());
        noteCache.put(note.getId(), note);
        persist();
        return note;
    }

    @Override
    public Optional<Note> findById(String id) {
        return Optional.ofNullable(noteCache.get(id));
    }

    @Override
    public List<Note> findAll() {
        return new ArrayList<>(noteCache.values());
    }
}
