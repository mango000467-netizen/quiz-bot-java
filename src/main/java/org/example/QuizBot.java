package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class QuizBot extends TelegramLongPollingBot {

    private final QuizService quizService = new QuizService();
    // Сесії зберігаються в пам'яті: ключ — userId
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    // Налаштуй тут свій username/token
    @Override
    public String getBotUsername() {
        return "https://t.me/QuizPlayGameBot"; // <-- заміни на ім'я бота
    }

    @Override
    public String getBotToken() {
        return "8303767364:AAGCLKejTn8e_w8GFEA_ASWUcxkD9ncRr1s"; // <-- заміни на токен з BotFather
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Message message) throws Exception {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String text = message.getText().trim();

        switch (text) {
            case "/start":
                sendText(chatId, "Привіт! Я бот-вікторина.\nКоманди:\n/quiz — почати вікторину (10 питань)\n/score — показати поточний рахунок\n/help — допомога");
                break;

            case "/help":
                sendText(chatId, "Цей бот проводить вікторину з 10 питань.\n" +
                        "Команди:\n/quiz — почати нову вікторину\n/score — показати поточний прогрес\n/help — ця підказка\n\nПісля кожного питання натисни кнопку з відповіддю.");
                break;

            case "/score":
                UserSession session = sessions.get(userId);
                if (session == null) {
                    sendText(chatId, "У тебе немає активної вікторини. Напиши /quiz щоб почати.");
                } else {
                    sendText(chatId, String.format("Поточний рахунок: %d/%d\nПитання: %d/%d",
                            session.getScore(), session.getTotalQuestions(),
                            Math.min(session.getCurrentNumber(), session.getTotalQuestions()), session.getTotalQuestions()));
                }
                break;

            case "/quiz":
                startQuiz(userId, chatId);
                break;

            default:
                sendText(chatId, "Не розумію. Напиши /quiz щоб почати вікторину або /help для допомоги.");
                break;
        }
    }

    private void startQuiz(Long userId, Long chatId) throws Exception {
        // Отримаємо 10 випадкових питань (або менше, якщо пул менший)
        List<Question> qList = quizService.getRandomQuestions(10);
        UserSession session = new UserSession(qList);
        // позначимо, що чекаємо на відповідь першого питання
        session.setAwaitingAnswer(true);
        sessions.put(userId, session);
        sendQuestion(chatId, userId, session);
    }

    private void sendQuestion(Long chatId, Long userId, UserSession session) throws Exception {
        Question q = session.getCurrentQuestion();
        if (q == null) {
            sendText(chatId, "Немає питань.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Питання %d/%d:\n", session.getCurrentNumber(), session.getTotalQuestions()));
        sb.append(q.getText()).append("\n\n");

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(sb.toString());

        // Inline keyboard: варіанти відповіді
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String[] opts = q.getOptions();
        for (int i = 0; i < opts.length; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText((i + 1) + ") " + opts[i]);
            // callback data: answer:{userId}:{index}
            // важливо: callbackData має бути коротким (<=64 байт), тому передаємо userId не обов'язково,
            // але для безпеки ми перевіримо сесію по userId з callback.getFrom()
            button.setCallbackData("answer:" + i);
            rows.add(Collections.singletonList(button));
        }

        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);

        execute(sm);
    }

    private void handleCallback(CallbackQuery callback) throws Exception {
        String data = callback.getData(); // наприклад "answer:2" або "next"
        User from = callback.getFrom();
        Long userId = from.getId();
        Long chatId = callback.getMessage().getChatId();
        Integer messageId = callback.getMessage().getMessageId();

        if (data == null) return;

        if (data.startsWith("answer:")) {
            String[] parts = data.split(":");
            if (parts.length < 2) return;
            int selectedIndex = Integer.parseInt(parts[1]);

            UserSession session = sessions.get(userId);
            if (session == null) {
                // немає сесії — відповідаємо, що треба почати
                answerCallback(callback.getId(), "У тебе немає активної вікторини. Напиши /quiz щоб почати.");
                return;
            }

            // Якщо вже відповіли на це питання — просто ігноруємо
            if (!session.isAwaitingAnswer()) {
                answerCallback(callback.getId(), "Ти вже відповів на це питання. Натисни 'Наступне'.");
                return;
            }

            Question current = session.getCurrentQuestion();
            boolean correct = (selectedIndex == current.getCorrectIndex());
            session.markAnswered(correct);

            // Відповідаємо callback (коротке повідомлення)
            if (correct) {
                answerCallback(callback.getId(), "✅ Правильно!");
            } else {
                String correctText = current.getOptions()[current.getCorrectIndex()];
                answerCallback(callback.getId(), "❌ Неправильно. Правильна: " + correctText);
            }

            // Оновимо повідомлення з питання — покажемо правильну відповідь і кнопку "Наступне"
            String resultText = String.format("Питання %d/%d:\n%s\n\nТвоя відповідь: %d\n%s\n\nРахунок: %d/%d",
                    session.getCurrentNumber(),
                    session.getTotalQuestions(),
                    current.getText(),
                    selectedIndex + 1,
                    correct ? "✅ Правильно" : "❌ Неправильно (правильна: " + current.getOptions()[current.getCorrectIndex()] + ")",
                    session.getScore(),
                    session.getTotalQuestions()
            );

            // Зробимо EditMessageText (щоб показати результат у тому ж повідомленні)
            EditMessageText edit = new EditMessageText();
            edit.setChatId(chatId.toString());
            edit.setMessageId(messageId);
            edit.setText(resultText);

            // Кнопка "Наступне" (callback: next)
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton nextBtn = new InlineKeyboardButton();
            nextBtn.setText("Наступне ▶️");
            nextBtn.setCallbackData("next");
            markup.setKeyboard(Collections.singletonList(Collections.singletonList(nextBtn)));
            edit.setReplyMarkup(markup);

            execute(edit);
            // тепер чекатимемо натискання "next" для переходу до наступного
            return;
        }

        if (data.equals("next")) {
            UserSession session = sessions.get(userId);
            if (session == null) {
                answerCallback(callback.getId(), "У тебе немає активної вікторини. Напиши /quiz щоб почати.");
                return;
            }

            // Перейти до наступного питання
            session.nextQuestion();
            if (session.isFinished()) {
                // закінчили — покажемо фінальний рахунок і видалимо сесію
                String finalMsg = String.format("Вікторина завершена!\nТвій результат: %d/%d",
                        session.getScore(), session.getTotalQuestions());
                // відредагуємо повідомлення (щоб користувач бачив завершення)
                EditMessageText edit = new EditMessageText();
                edit.setChatId(callback.getMessage().getChatId().toString());
                edit.setMessageId(callback.getMessage().getMessageId());
                edit.setText(finalMsg);
                execute(edit);

                sessions.remove(userId);
                return;
            } else {
                // Показати наступне питання — відправимо нове повідомлення
                session.setAwaitingAnswer(true);
                sendQuestion(chatId, userId, session);
                // опціонально — видалити старе повідомлення або відредагувати його — ми вже показали Next в старому, а нове питання прийде як нове повідомлення
                return;
            }
        }

        // Інші callback-дані — ігноруємо
        answerCallback(callback.getId(), "Невідома дія");
    }

    // Відповісти на callback, щоб показати підказку у клієнті
    private void answerCallback(String callbackId, String text) throws Exception {
        AnswerCallbackQuery ack = new AnswerCallbackQuery();
        ack.setCallbackQueryId(callbackId);
        ack.setText(text);
        execute(ack);
    }

    private void sendText(Long chatId, String text) throws Exception {
        SendMessage sm = new SendMessage();
        sm.setChatId(chatId);
        sm.setText(text);
        execute(sm);
    }
}
