package com.example.QuizAppApplication.service;

import com.example.QuizAppApplication.model.QuizSubmission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class GroqService {
    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    public String generateQuizPrompt(String subject, int grade, int totalQuestions, int maxScore, String difficulty) {
        return "Generate a quiz in JSON format for grade " + grade + " students. " +
                "Subject: " + subject + ". " +
                "Number of questions: " + totalQuestions + ". " +
                "Maximum score: " + maxScore + ". " +
                "Difficulty: " + difficulty + ". " +
                "Each question should have 4 options and 1 correct answer." +
                "Note: Return response only in JSON format. JSON should only contain questions, option and correct answer.";
    }

    public String generateQuiz(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");  // adjust to your Groq model

        // Force JSON output
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a quiz generator. Always return output in pure JSON. " +
                                "Do not add explanations, markdown, or extra text."),
                Map.of("role", "user", "content", prompt)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    groqApiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object choicesObj = response.getBody().get("choices");
                if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
                    Object first = choices.get(0);
                    if (first instanceof Map<?, ?> choice) {
                        Object message = choice.get("message");
                        if (message instanceof Map<?, ?> msg) {
                            Object content = msg.get("content");
                            return content != null ? content.toString().trim() : "{}";
                        }
                    }
                }
            }
            return "{}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }


    public String evaluateQuiz(String quizId, List<QuizSubmission.Response> responses) {
        RestTemplate restTemplate = new RestTemplate();

        // Build a simple prompt for Groq AI to evaluate answers
        StringBuilder prompt = new StringBuilder();
        prompt.append("Evaluate the following quiz answers in JSON format.\n");
        prompt.append("Quiz ID: ").append(quizId).append("\n");
        prompt.append("User responses:\n");
        for (QuizSubmission.Response r : responses) {
            prompt.append("Question ID: ").append(r.getQuestionId())
                    .append(", Answer: ").append(r.getUserResponse()).append("\n");
        }
        prompt.append("Return JSON with total score and optionally correct/incorrect details for each question.");

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");  // check your Groq model
        body.put("messages", List.of(Map.of("role", "user", "content", prompt.toString())));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    groqApiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object choicesObj = response.getBody().get("choices");
                if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
                    Object first = choices.get(0);
                    if (first instanceof Map<?,?> choice) {
                        Object message = choice.get("message");
                        if (message instanceof Map<?,?> msg) {
                            Object content = msg.get("content");
                            return content != null ? content.toString() : "No content returned";
                        }
                    }
                }
            }
            return "Groq API returned no evaluation";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error while evaluating quiz: " + e.getMessage();
        }
    }

}
