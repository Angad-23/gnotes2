package com.tutornotes.service;

import com.tutornotes.dto.NoteRequest;
import com.tutornotes.model.SessionNote;
import com.tutornotes.model.Standard;
import com.tutornotes.repository.SessionNoteRepository;
import com.tutornotes.repository.StandardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionNoteService {

    private final SessionNoteRepository noteRepository;
    private final StandardRepository standardRepository;
    private final GroqService groqService;

    /**
     * Generate a note via Groq, persist it, and return the saved entity.
     */
    @Transactional
    public SessionNote generateAndSave(NoteRequest req) {
        // Call Groq
        String generatedNote = groqService.generateNote(req);

        // Build entity
        SessionNote note = new SessionNote();
        note.setNoteType(SessionNote.NoteType.valueOf(req.getNoteType().toUpperCase()));
        note.setStudentNames(req.getStudentNames());
        note.setSessionDate(req.getSessionDate());
        note.setEngagement(req.getEngagement());
        note.setSkills(req.getSkills());
        note.setActivities(req.getActivities());
        note.setGeneratedNote(generatedNote);

        // Attach standard if provided
        if (req.getState() != null && req.getGrade() != null && req.getTopic() != null) {
            note.setState(req.getState());
            note.setGrade(req.getGrade());
            note.setTopic(req.getTopic());

            Optional<Standard> std = standardRepository
                    .findByStateAndGradeAndTopic(req.getState(), req.getGrade(), req.getTopic());
            std.ifPresent(note::setStandard);
        }

        SessionNote saved = noteRepository.save(note);
        log.info("Saved session note id={} for student(s): {}", saved.getId(), saved.getStudentNames());
        return saved;
    }

    public List<SessionNote> getAllNotes() {
        return noteRepository.findAllByOrderBySessionDateDescCreatedAtDesc();
    }

    public Optional<SessionNote> getById(Long id) {
        return noteRepository.findById(id);
    }

    public void delete(Long id) {
        noteRepository.deleteById(id);
    }
}
