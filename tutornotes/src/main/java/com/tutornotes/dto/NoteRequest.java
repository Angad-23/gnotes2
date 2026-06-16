package com.tutornotes.dto;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
 
import java.time.LocalDate;
 
@Data
public class NoteRequest {
 
    @NotBlank(message = "Note type is required")
    private String noteType;            // "STUDENT" or "GROUP"
 
    @NotBlank(message = "Student name is required")
    private String studentNames;
 
    @NotNull(message = "Session date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate sessionDate;
 
    private String state;               // e.g. "Florida", "Common Core", "Maryland"
    private String grade;               // e.g. "K", "1", "Algebra 1"
    private String code;                // e.g. "MA.4.NSO.1.1"  ← replaces topic
    private String standardDescription; // full description from CSV ← new field
 
    @NotBlank(message = "Engagement observation is required")
    private String engagement;
 
    @NotBlank(message = "Skills observation is required")
    private String skills;
 
    private String activities;
}