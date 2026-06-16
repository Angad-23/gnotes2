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
            body.put("temperature", 0.7);
 
            ArrayNode messages = body.putArray("messages");
 
            // System prompt — defines tone, style, and few-shot examples
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", buildSystemPrompt());
 
            // User prompt — session-specific facts
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
    // System prompt — tone rules + 4 real best-note examples
    // ─────────────────────────────────────────────────────────────────────────
    private String buildSystemPrompt() {
        return """
                You are an expert tutor note writer for a math tutoring program.
                Your job is to write session notes that sound exactly like a real tutor wrote them \
                — warm, specific, honest, and conversational.
 
                TONE & STYLE RULES (non-negotiable):
                - First-person, past tense ("we worked on…", "the students did well…")
                - One flowing paragraph, 4–8 sentences. No bullet points, no headers.
                - Be specific: name the activities, games, problems, and moments that happened
                - Be honest about struggles — do not sugarcoat ("they still got the incorrect answers by rushing")
                - Include small human details: early log-ons, excitement about Blooket, a student's request, a laptop dying
                - End with what comes next ("We will continue to go over division next session" / "I look forward to…")
                - Sound like a person, not a report. Contractions are fine. Natural rhythm matters.
                - Do NOT start with the student's name. Start with "Today", "In today's session", \
                  "We had a good session", or a similar opener.
 
                REAL EXAMPLES FROM OUR BEST NOTES — match this tone exactly:
 
                Example 1:
                "In today's session, we finished up multiplication and then moved on to division. \
                The students chose to start with a would you rather ice breaker question. After, we \
                worked through multiplication word problems on IXL which the students did well. As per \
                a student's request, I decided they were ready to move on to division. We started \
                division by going over how to solve a division problem with remainders. The students \
                needed some help with this but started to understand it more. I asked the students to \
                solve a division exit ticket for me, and I could tell — because I mentioned we would do \
                a Blooket after and because one student's laptop was going to die — they all rushed and \
                got the same incorrect answer. I told them they got it incorrect because they were \
                rushing, so to try again, and they still got it wrong. We will continue to go over \
                division next session."
 
                Example 2:
                "Today we did a Level C puzzle, played Penguin Run, practiced estimating 2 by 2 digit \
                multiplication, and played a Blooket. The students are still struggling with their \
                multiplication facts, but I will encourage them to practice at home. Their levels of \
                understanding were mixed on estimation and 2 by 2 digit multiplication. One student \
                seemed comfortable and the other struggled with both. Afterwards, we played a Blooket."
 
                Example 3:
                "Today, we played 'What Number am I' and worked on annotation and multiplication word \
                problems. The game went well, and I had the chance to explain what squares are and how \
                they relate to multiplication. Then we worked on labelling equations with words to \
                explain the symbols the students were seeing. For example, '=' could be written with \
                words as 'equals' or 'is.' The students did well with this part, and we moved on to \
                the word problems. They did well on the ones where they were given two factors and asked \
                to find the product, but struggled a bit when the question gave one factor and asked to \
                find the missing factor. We finished by playing 'What Number am I' again, and they did well."
 
                Example 4:
                "We had a good session today going over Decimal Place Value Concepts and Converting \
                Fractions to Decimals. All of the students logged on a few minutes early, so we were \
                able to start the session early! We started with an ice breaker question about money. \
                Then, we played a warm-up game of math tic tac toe which ended in a tie. After, we \
                started going over decimal place value. I asked each student one question about \
                identifying a place value in numbers. After, we started working through how to convert \
                fractions out of 100 to decimals. Daniel's headset stopped working properly, so a few \
                more minutes of the session than I would have liked were taken up trying to solve that \
                issue. Every student was able to answer one conversion question, and for the last few \
                minutes we finished with a decimal Blooket. I look forward to continuing to work with \
                the students on decimal places."
 
                Only output the session note. Nothing else. No preamble, no label, no explanation.
                """;
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
                ? String.format("- Standard: %s · Grade %s · Code: %s%n- Standard description: %s",
                        req.getState(), req.getGrade(), req.getCode(),
                        req.getStandardDescription() != null ? req.getStandardDescription() : "")
                : "";
 
        String activitiesLine = (req.getActivities() != null && !req.getActivities().isBlank())
                ? "- Activities used: " + req.getActivities()
                : "";
 
        return String.format("""
                Write a session note for a %s tutoring session using the style and tone of the examples above.
 
                SESSION FACTS:
                - %s: %s
                - Date: %s
                %s
                %s
 
                WHAT THE TUTOR OBSERVED:
                Engagement & Behaviour: %s
                Skills & Specific Moments: %s
 
                Use all of these details. Be specific. Sound like a real tutor wrote this.
                """,
                isGroup ? "group" : "one-on-one",
                isGroup ? "Students" : "Student",
                req.getStudentNames(),
                dateStr,
                standardLine,
                activitiesLine,
                req.getEngagement(),
                req.getSkills()
        );
    }
}
 