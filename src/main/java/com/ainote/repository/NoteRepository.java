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

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = "SELECT jsonb_array_elements_text(ai_metadata->'entities') as entity, count(*) as cnt FROM notes WHERE deleted = false GROUP BY entity ORDER BY cnt DESC LIMIT 20")
    List<Object[]> countTopEntities();

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = "SELECT ai_metadata->>'primaryDomain' as domain, count(*) as cnt FROM notes WHERE deleted = false GROUP BY domain ORDER BY cnt DESC LIMIT 20")
    List<Object[]> countTopDomains();

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = "SELECT ai_metadata->>'contentType' as type, count(*) as cnt FROM notes WHERE deleted = false GROUP BY type ORDER BY cnt DESC LIMIT 20")
    List<Object[]> countTopContentTypes();
}
