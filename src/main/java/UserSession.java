package org.example;

import java.util.List;

public class UserSession {
    private final List<Question> questions; // список питань для цієї сесії
    private int currentIndex = 0;           // індекс поточного питання (0..n-1)
    private int score = 0;                  // кількість правильних відповідей
    private boolean awaitingAnswer = false; // чи очікуємо відповідь на поточне питання

    public UserSession(List<Question> questions) {
        this.questions = questions;
    }

    public Question getCurrentQuestion() {
        if (currentIndex < questions.size()) return questions.get(currentIndex);
        return null;
    }

    public void markAnswered(boolean correct) {
        if (correct) score++;
        awaitingAnswer = false;
    }

    public void nextQuestion() {
        currentIndex++;
        awaitingAnswer = currentIndex < questions.size();
    }

    public boolean isFinished() {
        return currentIndex >= questions.size();
    }

    public int getScore() {
        return score;
    }

    public int getCurrentNumber() {
        return currentIndex + 1; // людиниочний номер питання
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public boolean isAwaitingAnswer() {
        return awaitingAnswer;
    }

    public void setAwaitingAnswer(boolean awaiting) {
        this.awaitingAnswer = awaiting;
    }
}

