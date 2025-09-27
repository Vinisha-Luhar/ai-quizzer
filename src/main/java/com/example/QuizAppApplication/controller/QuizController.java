package com.example.QuizAppApplication.controller;

import com.example.QuizAppApplication.model.Quiz;
import com.example.QuizAppApplication.model.QuizRequest;
import com.example.QuizAppApplication.model.QuizSubmission;
import com.example.QuizAppApplication.service.GroqService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    @Autowired
    private GroqService groqService;

    private static final Map<String, Quiz> quizStore = new HashMap<>();
    private static final List<Quiz> historyStore = new ArrayList<>();

    private String extractValidJson(String response) {
        // Try direct parse first
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readTree(response);
            return response;
        } catch (Exception e) {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start != -1 && end != -1 && start < end) {
                String possibleJson = response.substring(start, end + 1);
                try {
                    mapper.readTree(possibleJson);
                    return possibleJson;
                } catch (Exception ex) {
                    return null;
                }
            }
            return null;
        }
    }


    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String prompt = String.format(
                "Generate a quiz for Grade %s, Subject %s, Difficulty %s with %s questions. " +
                        "Return valid JSON only with this structure: " +
                        "{ \"quizId\": \"string\", " +
                        "\"grade\": number, " +
                        "\"subject\": \"string\", " +
                        "\"totalQuestions\": number, " +
                        "\"maxScore\": number, " +
                        "\"difficulty\": \"EASY|MEDIUM|HARD\", " +
                        "\"questions\": [ { \"questionId\": \"string\", " +
                        "\"questionText\": \"string\", " +
                        "\"options\": [\"A\",\"B\",\"C\",\"D\"], " +
                        "\"correctAnswer\": \"string\" } ] }",
                payload.get("grade"),
                payload.get("subject"),
                payload.get("difficulty"),
                payload.get("totalQuestions")
        );

        String quizJson = null;
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String rawResponse = groqService.generateQuiz(prompt);

            // Validate or extract JSON
            quizJson = extractValidJson(rawResponse);

            if (quizJson != null) {
                break; // got valid JSON
            } else if (attempt < maxRetries) {
                System.out.println("Retrying quiz generation... attempt " + attempt);
            }
        }

        if (quizJson == null) {
            return ResponseEntity.status(500).body("Failed to generate valid JSON after 3 attempts");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Quiz quiz = mapper.readValue(quizJson, Quiz.class);


            quizStore.put(quiz.getQuizId(), quiz);

            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error parsing quiz JSON: " + e.getMessage());
        }
    }

//    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
//        String username = (String) request.getAttribute("username");
//        if (username == null) {
//            return ResponseEntity.status(401).body("Unauthorized");
//        }
//
//        // Prompt Groq to return JSON structured quiz with correct answers
//        String prompt = String.format(
//                "Generate a quiz for Grade %s, Subject %s, Difficulty %s with %s questions. " +
//                        "Return valid JSON only with this structure: " +
//                        "{ \"quizId\": \"string\", " +
//                        "\"grade\": number, " +
//                        "\"subject\": \"string\", " +
//                        "\"totalQuestions\": number, " +
//                        "\"maxScore\": number, " +
//                        "\"difficulty\": \"EASY|MEDIUM|HARD\", " +
//                        "\"questions\": [ { \"questionId\": \"string\", " +
//                        "\"questionText\": \"string\", " +
//                        "\"options\": [\"A\",\"B\",\"C\",\"D\"], " +
//                        "\"correctAnswer\": \"string\" } ] }",
//                payload.get("grade"),
//                payload.get("subject"),
//                payload.get("difficulty"),
//                payload.get("totalQuestions")
//        );
//
//        String quizJson = groqService.generateQuiz(prompt);
//
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            Quiz quiz = mapper.readValue(quizJson, Quiz.class);
//
//            // Store quiz in memory
//            quizStore.put(quiz.getQuizId(), quiz);
//
//            return ResponseEntity.ok(quiz);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Error parsing quiz JSON: " + e.getMessage());
//        }
//    }


    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody QuizSubmission submission, HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Quiz quiz = quizStore.get(submission.getQuizId());
        if (quiz == null) {
            return ResponseEntity.status(404).body("Quiz not found");
        }

        // ✅ Count past attempts by this user for this quiz
        long pastAttempts = historyStore.stream()
                .filter(q -> q.getQuizId().equals(submission.getQuizId()) && q.getUsername().equals(username))
                .count();
        int attemptNumber = (int) pastAttempts + 1;

        // Build Groq evaluation prompt (same as before)
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a strict JSON API. ");
        prompt.append("Return ONLY valid JSON without any comments, text, or markdown. ");
        prompt.append("Do not include ```json or ``` markers. ");
        prompt.append("Here is the quiz:\n").append(quiz).append("\n");
        prompt.append("Here are the user responses:\n");
        for (QuizSubmission.Response r : submission.getResponses()) {
            prompt.append("{ \"questionId\": \"").append(r.getQuestionId())
                    .append("\", \"userAnswer\": \"").append(r.getUserResponse()).append("\" },\n");
        }
        prompt.append("Now evaluate answers against correctAnswer. ");
        prompt.append("Return strictly this format: { \"quizId\": \"string\", \"totalScore\": number, \"details\": [ {\"questionId\":\"...\",\"userAnswer\":\"...\",\"correctAnswer\":\"...\",\"correct\":true|false} ] }");


        String evaluationResult = groqService.generateQuiz(prompt.toString());

        // ✅ Save attempt to history
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(evaluationResult);

            Quiz result = new Quiz();
            result.setQuizId(submission.getQuizId());
            result.setUsername(username);
            result.setGrade(quiz.getGrade());
            result.setSubject(quiz.getSubject());
            result.setMaxScore(quiz.getMaxScore());
            result.setScore(root.get("totalScore").asInt());
            result.setCompletedDate(LocalDate.now());
            result.setAttemptNumber(attemptNumber);

            historyStore.add(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(evaluationResult);
    }


    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(required = false) Integer grade,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Integer minMarks,
            @RequestParam(required = false) Integer maxMarks,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest request) {

        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        LocalDate fromDate = (from != null) ? LocalDate.parse(from, DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
        LocalDate toDate = (to != null) ? LocalDate.parse(to, DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;

        List<Quiz> filtered = historyStore.stream()
                .filter(q -> q.getUsername().equals(username)) // only user’s history
                .filter(q -> grade == null || q.getGrade() == grade)
                .filter(q -> subject == null || q.getSubject().equalsIgnoreCase(subject))
                .filter(q -> minMarks == null || q.getScore() >= minMarks)
                .filter(q -> maxMarks == null || q.getScore() <= maxMarks)
                .filter(q -> fromDate == null || !q.getCompletedDate().isBefore(fromDate))
                .filter(q -> toDate == null || !q.getCompletedDate().isAfter(toDate))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/history/{quizId}")
    public ResponseEntity<?> getQuizAttempts(@PathVariable String quizId, HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        List<Quiz> attempts = historyStore.stream()
                .filter(q -> q.getQuizId().equals(quizId) && q.getUsername().equals(username))
                .sorted(Comparator.comparingInt(Quiz::getAttemptNumber))
                .toList();

        if (attempts.isEmpty()) {
            return ResponseEntity.status(404).body("No attempts found for quiz " + quizId);
        }

        return ResponseEntity.ok(attempts);
    }


    @PostMapping("/hint/{quizId}/{questionId}")
    public ResponseEntity<?> getHint(@PathVariable String quizId,
                                     @PathVariable String questionId,
                                     HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Quiz quiz = quizStore.get(quizId);
        if (quiz == null) {
            return ResponseEntity.status(404).body("Quiz not found");
        }

        // Find the question
        Quiz.Question question = quiz.getQuestions().stream()
                .filter(q -> q.getQuestionId().equals(questionId))
                .findFirst()
                .orElse(null);

        if (question == null) {
            return ResponseEntity.status(404).body("Question not found");
        }

        // Build prompt for Groq
        String prompt = "Provide a helpful hint for this question without giving the answer directly:\n"
                + question.getQuestionText() + "\nOptions: " + question.getOptions();

        String hint = groqService.generateQuiz(prompt);
        return ResponseEntity.ok(Map.of("questionId", questionId, "hint", hint));
    }




}
