package com.ainote.repository;

import com.ainote.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {

        // 仅查询未软删除的笔记
        List<Note> findByDeletedFalse();

        @Query(nativeQuery = true, value = "SELECT * FROM notes WHERE deleted = true ORDER BY updated_at DESC", countQuery = "SELECT count(*) FROM notes WHERE deleted = true")
        Page<Note> findTrashNotes(Pageable pageable);

        // 如需覆盖 findAll 使其默认排除已删除笔记，可使用上方 findByDeletedFalse

        // 软删除笔记
        @Modifying
        @Query("UPDATE Note n SET n.deleted = true WHERE n.id = :id")
        void softDelete(String id);

        @Query(nativeQuery = true, value = "SELECT entity, count(*) as cnt FROM (SELECT jsonb_array_elements_text(ai_metadata->'entities') as entity FROM notes WHERE deleted = false AND ai_metadata IS NOT NULL AND ai_metadata->'entities' IS NOT NULL) sub WHERE entity IS NOT NULL GROUP BY entity ORDER BY cnt DESC LIMIT 20")
        List<Object[]> countTopEntities();

        @Query(nativeQuery = true, value = "SELECT ai_metadata->>'primaryDomain' as domain, count(*) as cnt FROM notes WHERE deleted = false AND ai_metadata IS NOT NULL AND ai_metadata->>'primaryDomain' IS NOT NULL GROUP BY domain ORDER BY cnt DESC LIMIT 20")
        List<Object[]> countTopDomains();

        @Query(nativeQuery = true, value = "SELECT ai_metadata->>'contentType' as type, count(*) as cnt FROM notes WHERE deleted = false AND ai_metadata IS NOT NULL AND ai_metadata->>'contentType' IS NOT NULL GROUP BY type ORDER BY cnt DESC LIMIT 20")
        List<Object[]> countTopContentTypes();

        @Query(nativeQuery = true, value = "SELECT id, title, ai_metadata->'entities' as entities FROM notes WHERE deleted = false AND (:domain IS NULL OR ai_metadata->>'primaryDomain' = :domain)")
        List<Object[]> findNotesForGraph(@Param("domain") String domain);

        @Query(nativeQuery = true, value = "SELECT * FROM notes WHERE deleted = false AND (:domain IS NULL OR ai_metadata->>'primaryDomain' = :domain) AND (:type IS NULL OR ai_metadata->>'contentType' = :type)", countQuery = "SELECT count(*) FROM notes WHERE deleted = false AND (:domain IS NULL OR ai_metadata->>'primaryDomain' = :domain) AND (:type IS NULL OR ai_metadata->>'contentType' = :type)")
        Page<Note> findByFilters(
                        @Param("domain") String domain,
                        @Param("type") String type,
                        Pageable pageable);

        @Query(nativeQuery = true, value = "SELECT DISTINCT jsonb_array_elements_text(ai_metadata->'entities') FROM notes WHERE deleted = false AND ai_metadata->>'primaryDomain' = :domain")
        List<String> findEntitiesByDomain(@Param("domain") String domain);

        @Query(nativeQuery = true, value = "SELECT id, title, ai_metadata->>'contentType' as contentType FROM notes WHERE deleted = false AND ai_metadata->'entities' @> jsonb_build_array(:entityName)")
        List<Object[]> findNotesByEntity(
                        @Param("entityName") String entityName);

        @Query(nativeQuery = true, value = "SELECT jsonb_array_elements_text(ai_metadata->'entities') FROM notes WHERE id = :noteId AND deleted = false")
        List<String> findEntitiesByNoteId(@Param("noteId") String noteId);

        @Query(nativeQuery = true, value = "SELECT DISTINCT ai_metadata->>'primaryDomain' FROM notes WHERE deleted = false AND ai_metadata->>'primaryDomain' ILIKE CONCAT('%', :keyword, '%') LIMIT 10")
        List<String> suggestDomains(@Param("keyword") String keyword);

        @Query(nativeQuery = true, value = "SELECT DISTINCT ai_metadata->>'contentType' FROM notes WHERE deleted = false AND ai_metadata->>'contentType' ILIKE CONCAT('%', :keyword, '%') LIMIT 10")
        List<String> suggestContentTypes(@Param("keyword") String keyword);

        @Query(nativeQuery = true, value = "SELECT DISTINCT el FROM notes n, jsonb_array_elements_text(n.ai_metadata->'entities') el WHERE n.deleted = false AND el ILIKE CONCAT('%', :keyword, '%') LIMIT 10")
        List<String> suggestEntities(@Param("keyword") String keyword);

        @Query(nativeQuery = true, value = "SELECT DISTINCT n.id, n.title, n.ai_metadata->>'contentType' as contentType, CAST(n.ai_metadata->'entities' as text) as entities FROM notes n JOIN jsonb_array_elements_text(n.ai_metadata->'entities') e ON true WHERE n.deleted = false AND n.id != :noteId AND e IN (:entities)")
        List<Object[]> findNotesSharingEntities(
                        @Param("noteId") String noteId,
                        @Param("entities") List<String> entities);

        @Modifying
        @Transactional
        @Query(nativeQuery = true, value = "DELETE FROM vector_store WHERE metadata->>'note_id' = :noteId")
        void deleteVectorsByNoteId(@Param("noteId") String noteId);

        // 第三阶段宏轨道 A 过滤器：查找实体标签中包含指定概念的所有笔记 ID
        @Query(nativeQuery = true, value = "SELECT id FROM notes WHERE deleted = false AND (ai_metadata->'entities' @> CAST(:concept AS jsonb) OR ai_metadata->>'primaryDomain' = :conceptStr OR ai_metadata->>'contentType' = :conceptStr)")
        List<String> findNoteIdsByEntityLike(
                        @Param("concept") String jsonConcept,
                        @Param("conceptStr") String conceptStr);
}
