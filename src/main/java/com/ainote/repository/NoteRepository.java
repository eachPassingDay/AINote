package com.ainote.repository;

import com.ainote.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {

    // Find only non-deleted notes
    List<Note> findByDeletedFalse();

    // Override findAll to return only non-deleted notes if needed, or use a
    // specific method

    // Method to soft delete a note
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Note n SET n.deleted = true WHERE n.id = :id")
    void softDelete(String id);
}
