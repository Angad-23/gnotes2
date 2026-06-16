package com.tutornotes.repository;

import com.tutornotes.model.SessionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionNoteRepository extends JpaRepository<SessionNote, Long> {

    List<SessionNote> findAllByOrderBySessionDateDescCreatedAtDesc();

    List<SessionNote> findByNoteTypeOrderBySessionDateDesc(SessionNote.NoteType noteType);
}
