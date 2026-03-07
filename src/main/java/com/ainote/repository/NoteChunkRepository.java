package com.ainote.repository;

import com.ainote.entity.NoteChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NoteChunkRepository extends JpaRepository<NoteChunk, String> {
    List<NoteChunk> findByNoteIdOrderByChunkIndexAsc(String noteId);

    @Query("SELECT nc FROM NoteChunk nc JOIN FETCH nc.note WHERE nc.id IN :ids")
    List<NoteChunk> findByIdInWithNote(@Param("ids") List<String> ids);

    @Transactional
    @Modifying
    void deleteByNoteId(String noteId);
}
