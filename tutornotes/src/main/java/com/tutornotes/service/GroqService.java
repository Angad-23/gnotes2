package com.tutornotes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tutornotes.dto.NoteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqService {

    private final WebClient groqWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.model}")
    private String model;

    @Value("${groq.api.max-tokens}")
    private int maxTokens;

    /**
     * Calls Groq API and returns the generated session note text.
     */
    public String generateNote(NoteRequest req) {
        String prompt = buildPrompt(req);
        log.debug("Calling Groq API with model: {}", model);

        try {
            // Build request body
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.5); // lower = sticks to session facts, less hallucination

            ArrayNode messages = body.putArray("messages");

            // System prompt — tone rules + style-only examples
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", buildSystemPrompt());

            // User prompt — session facts only
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", prompt);

            // Call Groq
            String responseBody = groqWebClient.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response
            JsonNode root = objectMapper.readTree(responseBody);
            String note = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("")
                    .trim();

            if (note.isEmpty()) {
                throw new RuntimeException("Empty response from Groq API");
            }

            log.debug("Generated note ({} chars)", note.length());
            return note;

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate note: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // System prompt — tone rules + style-reference examples (NOT content templates)
    // ─────────────────────────────────────────────────────────────────────────
    private String buildSystemPrompt() {
        return "You are an expert tutor note writer for a math tutoring program.\n\n"
                + "TONE & STYLE RULES:\n"
                + "- First-person, past tense (\"we worked on…\", \"the students did well…\")\n"
                + "- One flowing paragraph, 4–8 sentences. No bullet points, no headers.\n"
                + "- Be specific: name the actual activities, games, problems, and moments from the session facts\n"
                + "- Be honest about struggles — do not sugarcoat\n"
                + "- Include small human details if mentioned (early log-ons, a student's request, technical issues)\n"
                + "- End with what comes next (\"We will continue to go over X next session\" / \"I look forward to…\")\n"
                + "- Sound like a person, not a report. Contractions are fine.\n"
                + "- Do NOT start with the student's name. Start with \"Today\", \"In today's session\", "
                + "\"We had a good session\", or a similar opener.\n\n"
                + "STYLE REFERENCE EXAMPLES — these show tone and structure only.\n"
                + "Do NOT copy any content, activities, names, or topics from these examples.\n"
                + "Your note must be based ONLY on the session facts provided by the user.\n\n"
                + "Example A (tone reference only):\n"
                + "\"In today's session, we finished up multiplication and then moved on to division. "
                + "The students chose to start with a would you rather ice breaker question. After, we "
                + "worked through multiplication word problems on IXL which the students did well. As per "
                + "a student's request, I decided they were ready to move on to division. We started "
                + "division by going over how to solve a division problem with remainders. The students "
                + "needed some help with this but started to understand it more. We will continue to go "
                + "over division next session.\"\n\n"
                + "Example B (tone reference only):\n"
                + "\"Today we did a Level C puzzle, played Penguin Run, practiced estimating 2 by 2 digit "
                + "multiplication, and played a Blooket. The students are still struggling with their "
                + "multiplication facts, but I will encourage them to practice at home. Their levels of "
                + "understanding were mixed — one student seemed comfortable and the other struggled. "
                + "Afterwards, we played a Blooket.\"\n\n"
                + "Example C (tone reference only):\n"
                + "\"We had a good session today going over Decimal Place Value Concepts and Converting "
                + "Fractions to Decimals. All of the students logged on a few minutes early, so we were "
                + "able to start the session early! We started with an ice breaker question, then played "
                + "a warm-up game of math tic tac toe. Every student was able to answer one conversion "
                + "question, and for the last few minutes we finished with a Blooket. I look forward to "
                + "continuing to work with the students on decimal places.\"\n\n"
                + "CRITICAL: The examples above are for tone and structure only.\n"
                + "Every word in the generated note must come from the session facts the user provides.\n"
                + "Never borrow activities, names, topics, or phrases from the examples above.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User prompt — session-specific facts only
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPrompt(NoteRequest req) {
        boolean isGroup = "GROUP".equalsIgnoreCase(req.getNoteType());

        String dateStr = req.getSessionDate() != null
                ? req.getSessionDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                : "today";

        String standardLine = (req.getState() != null && !req.getState().isBlank()
                && req.getCode() != null && !req.getCode().isBlank())
                ? String.format("- Standard: %s · Grade %s · Code: %s\n- Standard description: %s",
                        req.getState(), req.getGrade(), req.getCode(),
                        req.getStandardDescription() != null ? req.getStandardDescription() : "")
                : "";

        String activitiesLine = (req.getActivities() != null && !req.getActivities().isBlank())
                ? "- Activities used: " + req.getActivities()
                : "";

        return "Write a session note using ONLY the facts below. "
                + "Do not use any content, activities, or phrases from the style examples.\n\n"
                + "SESSION FACTS:\n"
                + "- " + (isGroup ? "Students" : "Student") + ": " + req.getStudentNames() + "\n"
                + "- Date: " + dateStr + "\n"
                + (standardLine.isBlank() ? "" : standardLine + "\n")
                + (activitiesLine.isBlank() ? "" : activitiesLine + "\n")
                + "\nWHAT THE TUTOR OBSERVED:\n"
                + "Engagement & Behaviour: " + req.getEngagement() + "\n"
                + "Skills & Specific Moments: " + req.getSkills() + "\n\n"
                + "Generate a note grounded entirely in the above facts, "
                + "written in the tone and style of the reference examples.";
    }
}