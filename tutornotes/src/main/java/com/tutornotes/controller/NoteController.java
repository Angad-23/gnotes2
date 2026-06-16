package com.tutornotes.controller;

import com.tutornotes.dto.NoteRequest;
import com.tutornotes.model.SessionNote;
import com.tutornotes.model.Standard;
import com.tutornotes.repository.StandardRepository;
import com.tutornotes.service.SessionNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class NoteController {

    private final SessionNoteService noteService;
    private final StandardRepository standardRepository;

    // ── Home / form ──────────────────────────────────────────

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("noteRequest", new NoteRequest());
        model.addAttribute("states", standardRepository.findDistinctStates());
        model.addAttribute("recentNotes", noteService.getAllNotes().stream().limit(5).toList());
        return "index";
    }

    // ── Generate note (form POST) ────────────────────────────

    @PostMapping("/generate")
    public String generate(
            @Valid @ModelAttribute("noteRequest") NoteRequest req,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("states", standardRepository.findDistinctStates());
            model.addAttribute("recentNotes", noteService.getAllNotes().stream().limit(5).toList());
            return "index";
        }

        try {
            SessionNote saved = noteService.generateAndSave(req);
            redirectAttributes.addFlashAttribute("successNote", saved.getGeneratedNote());
            redirectAttributes.addFlashAttribute("savedId", saved.getId());
            return "redirect:/";
        } catch (Exception e) {
            log.error("Note generation failed", e);
            model.addAttribute("error", "Failed to generate note: " + e.getMessage());
            model.addAttribute("states", standardRepository.findDistinctStates());
            model.addAttribute("recentNotes", noteService.getAllNotes().stream().limit(5).toList());
            return "index";
        }
    }

    // ── History page ─────────────────────────────────────────

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("notes", noteService.getAllNotes());
        return "history";
    }

    // ── View a single note ───────────────────────────────────

    @GetMapping("/notes/{id}")
    public String viewNote(@PathVariable Long id, Model model) {
        return noteService.getById(id)
                .map(note -> { model.addAttribute("note", note); return "note-detail"; })
                .orElse("redirect:/history");
    }

    // ── Delete ───────────────────────────────────────────────

    @PostMapping("/notes/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        noteService.delete(id);
        ra.addFlashAttribute("deleted", true);
        return "redirect:/history";
    }

    // ── AJAX: grades by state ────────────────────────────────

    @GetMapping("/api/grades")
    @ResponseBody
    public List<String> grades(@RequestParam String state) {
        return standardRepository.findDistinctGradesByState(state);
    }

    // ── AJAX: topics by state + grade ────────────────────────

    @GetMapping("/api/topics")
    @ResponseBody
    public List<Standard> topics(@RequestParam String state, @RequestParam String grade) {
        return standardRepository.findByStateAndGradeOrderByTopicAsc(state, grade);
    }
}
