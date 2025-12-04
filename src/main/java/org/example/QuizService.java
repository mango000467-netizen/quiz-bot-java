package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizService {

    private final List<Question> pool = new ArrayList<>();

    public QuizService() {
        // Додавай сюди свої питання (приклад — 12 питань, бот вибере 10 випадкових)
        pool.add(new Question("Який тип даних використовується для цілих чисел у Java?",
                new String[]{"int", "String", "boolean", "float"}, 0));

        pool.add(new Question("Що означає JVM?",
                new String[]{"Java Variable Machine", "Java Virtual Machine", "Just Very Magic", "Java Version Manager"}, 1));

        pool.add(new Question("Яким ключовим словом створюється новий об’єкт?",
                new String[]{"create", "object", "new", "make"}, 2));

        pool.add(new Question("Який цикл в Java повторюється поки умова істинна?",
                new String[]{"for", "while", "switch", "try"}, 1));

        pool.add(new Question("Який модифікатор доступу дозволяє доступ всередині пакета?",
                new String[]{"private", "public", "protected", "package-private (без модифікатора)"}, 3));

        pool.add(new Question("Що таке 'null' в Java?",
                new String[]{"Порожній рядок", "Відсутність посилання", "0", "Тип даних"}, 1));

        pool.add(new Question("Який інтерфейс використовується для потоків вводу/виводу в Java?",
                new String[]{"List", "Runnable", "Serializable", "Closeable"}, 3));

        pool.add(new Question("Який оператор порівняння для рівності примітивних типів?",
                new String[]{"equals()", "==", "compareTo()", "==="}, 1));

        pool.add(new Question("Який пакет містить основні класи Java (String, Math, System)?",
                new String[]{"java.lang", "java.util", "java.io", "javax"}, 0));

        pool.add(new Question("Що повертає метод hashCode()?",
                new String[]{"Унікальний ідентифікатор об'єкта", "Хеш-код (int)", "Адресу в пам'яті", "Ім'я класу"}, 1));

        pool.add(new Question("Який цикл зручно використовувати для масивів?",
                new String[]{"for-each", "do-while", "while", "switch"}, 0));

        pool.add(new Question("Який тип дозволяє дробові числа з подвійною точністю?",
                new String[]{"float", "double", "BigDecimal", "int"}, 1));
    }

    /**
     * Повертає список з count випадкових питань (без повторів).
     * Якщо count більше, ніж pool size — поверне всі питання у випадковому порядку.
     */
    public List<Question> getRandomQuestions(int count) {
        List<Question> copy = new ArrayList<>(pool);
        Collections.shuffle(copy);
        if (count >= copy.size()) return copy;
        return copy.subList(0, count);
    }
}
