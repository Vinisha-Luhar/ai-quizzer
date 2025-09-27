package com.example.QuizAppApplication.model;


public class QuizRequest {

    private int grade;
    private String subject;
    private int totalQuestions;
    private int maxScore;
    private String difficulty;

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

}
