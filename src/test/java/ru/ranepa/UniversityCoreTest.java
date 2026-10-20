package ru.ranepa;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Общая часть практики №3: университет, студент, состояния и отказы.
 *
 * <p>Имена методов и полей свободны: помощник находит их по поведению. Проверяется то,
 * что описано в задании: билеты, порог 2.5, выпуск с четвёртого курса, отказы и сводка.
 */
@Timeout(30)
class UniversityCoreTest {

    private static Lab3Support.Domain d;

    @BeforeAll
    static void discoverUniversity() {
        d = Lab3Support.domain();
    }

    // ---------------------------------------------------------------- студент

    @Test
    @DisplayName("Студент закрыт: конструкторы private")
    void studentIsClosed() {
        Class<?> student = Lab3Support.studentClass();
        List<String> openConstructors = new ArrayList<>();
        for (Constructor<?> constructor : student.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(constructor.getModifiers())) {
                openConstructors.add(constructor.toString());
            }
        }
        assertTrue(openConstructors.isEmpty(),
                "Все конструкторы Student должны быть private: студента создаёт только университет."
                        + " Сейчас доступны: " + openConstructors);
    }

    @Test
    @DisplayName("Зачисление выдаёт билеты long по порядку и ставит на 1 курс")
    void enrollmentGivesSequentialTickets() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef first = university.enroll("Первый Студент");
        Lab3Support.StudentRef second = university.enroll("Второй Студент");
        Lab3Support.StudentRef third = university.enroll("Третий Студент");

        assertEquals(first.id + 1, second.id,
                "Номер следующего студенческого билета должен быть на 1 больше предыдущего."
                        + " Сейчас получились: " + first.id + ", " + second.id);
        assertEquals(second.id + 1, third.id,
                "Номер следующего студенческого билета должен быть на 1 больше предыдущего."
                        + " Сейчас получились: " + first.id + ", " + second.id + ", " + third.id);

        for (Lab3Support.StudentRef student : List.of(first, second, third)) {
            assertEquals(1, university.course(student),
                    "Зачисленный студент «" + student.name + "» должен быть на 1 курсе,"
                            + " а он на " + university.course(student) + ".");
            assertEquals(d.enrolledState, university.state(student),
                    "Зачисленный студент «" + student.name + "» должен учиться,"
                            + " а его состояние: " + university.state(student) + ".");
        }
    }

    // ---------------------------------------------------------------- перевод

    @Test
    @DisplayName("Средний ровно 2.5 — это уже отчисление, а не перевод")
    void averageExactlyTwoPointFiveExpels() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Ровно Два Пять");
        university.grade(student, 2);
        university.grade(student, 3); // (2 + 3) / 2 = 2.5

        university.transfer(student);

        assertEquals(d.expelledState, university.state(student),
                "При среднем балле ровно 2.5 перевод недоступен (условие «больше 2.5»),"
                        + " поэтому студент должен стать отчисленным. Сейчас состояние: "
                        + university.state(student) + ".");
    }

    @Test
    @DisplayName("Без оценок средний 0 — перевод отчисляет")
    void studentWithoutGradesIsExpelledOnTransfer() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Без Оценок");

        university.transfer(student);

        assertEquals(d.expelledState, university.state(student),
                "Без оценок средний балл равен 0, поэтому перевод должен отчислить студента."
                        + " Сейчас состояние: " + university.state(student) + "."
                        + " Деление на ноль давать Infinity/NaN не должно.");
    }

    @Test
    @DisplayName("Средний больше 2.5 переводит на следующий курс")
    void averageAboveTwoPointFiveTransfers() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Хороший Студент");
        university.grade(student, 3);
        university.grade(student, 3); // 3.0

        university.transfer(student);

        assertEquals(2, university.course(student),
                "При среднем 3.0 студент должен перейти на 2 курс.");
        assertEquals(d.enrolledState, university.state(student),
                "После успешного перевода студент должен продолжать учиться,"
                        + " а его состояние: " + university.state(student) + ".");
    }

    @Test
    @DisplayName("Средний меньше 2.5 отчисляет")
    void averageBelowTwoPointFiveExpels() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Слабый Студент");
        university.grade(student, 2);
        university.grade(student, 2); // 2.0

        university.transfer(student);

        assertEquals(d.expelledState, university.state(student),
                "При среднем 2.0 перевод должен отчислить студента."
                        + " Сейчас состояние: " + university.state(student) + ".");
    }

    @Test
    @DisplayName("Перевод с 3 курса ведёт на 4, а не выпускает")
    void thirdCourseGoesToFourth() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Третий Курс");
        university.grade(student, 5);
        university.grade(student, 5);
        university.transfer(student);
        university.transfer(student);

        assertEquals(3, university.course(student), "Перед проверкой студент должен быть на 3 курсе.");
        university.transfer(student);

        assertEquals(4, university.course(student),
                "С третьего курса перевод ведёт на четвёртый, а не выпускает.");
        assertEquals(d.enrolledState, university.state(student),
                "На 4 курсе студент ещё учится, а не выпустился.");
    }

    @Test
    @DisplayName("Перевод с 4 курса выпускает, курс не становится пятым")
    void fourthCourseGraduates() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Выпускник");
        university.grade(student, 5);
        university.grade(student, 5);
        university.transfer(student);
        university.transfer(student);
        university.transfer(student);

        assertEquals(4, university.course(student), "Перед выпуском студент должен быть на 4 курсе.");
        university.transfer(student);

        assertEquals(d.graduatedState, university.state(student),
                "Перевод с 4 курса при среднем больше 2.5 должен выпускать студента."
                        + " Сейчас состояние: " + university.state(student) + ".");
        assertEquals(4, university.course(student),
                "Курсов всего четыре: после выпуска курс не должен становиться пятым.");
    }

    @Test
    @DisplayName("С 4 курса с низким средним отчисляют, а не выпускают")
    void fourthCourseWithLowAverageIsExpelled() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Двоечник На Четвёртом");
        university.grade(student, 5);
        university.grade(student, 5);
        university.transfer(student);
        university.transfer(student);
        university.transfer(student);
        for (int i = 0; i < 10; i++) {
            university.grade(student, 2); // (10 + 20) / 12 = 2.5
        }

        university.transfer(student);

        assertEquals(d.expelledState, university.state(student),
                "На 4 курсе с средним ровно 2.5 перевод недоступен: студент должен быть"
                        + " отчислен, а не выпущен. Сейчас состояние: " + university.state(student) + ".");
    }

    // ---------------------------------------------------------------- отказы

    @Test
    @DisplayName("Отчисление меняет состояние и не трогает курс")
    void expelChangesState() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Отчисляемый");
        university.grade(student, 5);

        university.expel(student);

        assertEquals(d.expelledState, university.state(student),
                "После отчисления состояние должно быть «отчислен».");
        assertEquals(1, university.course(student), "Отчисление не должно менять курс.");
    }

    @Test
    @DisplayName("Повторное отчисление — IllegalArgumentException")
    void secondExpelIsRefused() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Уже Отчислен");
        university.expel(student);

        assertRefused("повторное отчисление отчисленного студента", () -> university.expel(student));
    }

    @Test
    @DisplayName("С отчисленным нельзя переводить, ставить оценку и отчислять")
    void operationsOnExpelledAreRefused() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Отчисленный Студент");
        university.grade(student, 5);
        university.expel(student);

        assertAll(
                () -> assertRefused("оценка отчисленному",
                        () -> university.grade(student, 5)),
                () -> assertRefused("перевод отчисленного",
                        () -> university.transfer(student)),
                () -> assertRefused("повторное отчисление",
                        () -> university.expel(student)));
    }

    @Test
    @DisplayName("С выпускником нельзя переводить, ставить оценку и отчислять")
    void operationsOnGraduatedAreRefused() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Выпустившийся Студент");
        university.grade(student, 5);
        university.grade(student, 5);
        university.transfer(student);
        university.transfer(student);
        university.transfer(student);
        university.transfer(student);

        assertEquals(d.graduatedState, university.state(student),
                "Перед проверкой студент должен быть выпущен.");
        assertAll(
                () -> assertRefused("оценка выпускнику",
                        () -> university.grade(student, 5)),
                () -> assertRefused("перевод выпускника",
                        () -> university.transfer(student)),
                () -> assertRefused("отчисление выпускника",
                        () -> university.expel(student)));
    }

    @Test
    @DisplayName("Неизвестный номер билета — IllegalArgumentException")
    void unknownTicketIsRefused() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Известный Студент");
        long unknown = Lab3Support.UNKNOWN_ID;

        assertAll(
                () -> assertRefused("оценка по неизвестному билету",
                        () -> university.grade(new Lab3Support.StudentRef(student.object, unknown,
                                "Неизвестный"), 5)),
                () -> assertRefused("перевод по неизвестному билету",
                        () -> university.transfer(new Lab3Support.StudentRef(student.object,
                                unknown, "Неизвестный"))),
                () -> assertRefused("отчисление по неизвестному билету",
                        () -> university.expel(new Lab3Support.StudentRef(student.object,
                                unknown, "Неизвестный"))));
    }

    // ---------------------------------------------------------------- оценки

    @Test
    @DisplayName("Оценки 2 и 5 допустимы, 1 и 6 — отказ")
    void gradeBoundaries() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Границы Оценок");

        assertDoesNotThrow(() -> university.grade(student, Lab3Support.MIN_GRADE),
                "Оценка 2 входит в диапазон 2–5 и должна приниматься.");
        assertDoesNotThrow(() -> university.grade(student, Lab3Support.MAX_GRADE),
                "Оценка 5 входит в диапазон 2–5 и должна приниматься.");
        assertAll(
                () -> assertRefused("оценка 1 (меньше 2)",
                        () -> university.grade(student, 1)),
                () -> assertRefused("оценка 6 (больше 5)",
                        () -> university.grade(student, 6)));
    }

    @Test
    @DisplayName("Оценка в 0 и 7 тоже вне диапазона")
    void gradesOutsideRange() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef student = university.enroll("Оценки Вне Диапазона");

        assertAll(
                () -> assertRefused("оценка 0",
                        () -> university.grade(student, 0)),
                () -> assertRefused("оценка 7",
                        () -> university.grade(student, 7)));
    }

    // ---------------------------------------------------------------- сводка

    @Test
    @DisplayName("Сводка показывает имена, оценки и все три состояния")
    void summaryShowsStudentsAndStates() {
        Lab3Support.Uni university = d.uni();
        Lab3Support.StudentRef enrolled = university.enroll("Всем Известный Студент");
        university.grade(enrolled, 4);
        university.grade(enrolled, 5);

        Lab3Support.StudentRef expelled = university.enroll("Отчисленный Студент");
        university.grade(expelled, 4);
        university.expel(expelled);

        Lab3Support.StudentRef graduated = university.enroll("Выпустившийся Студент");
        university.grade(graduated, 5);
        university.grade(graduated, 5);
        university.transfer(graduated);
        university.transfer(graduated);
        university.transfer(graduated);
        university.transfer(graduated);

        String text = university.summary();
        assertAll(
                () -> assertTrue(text.contains("Всем Известный Студент"),
                        "В сводке нет имени студента «Всем Известный Студент». Сводка:\n" + text),
                () -> assertTrue(text.contains("Отчисленный Студент"),
                        "В сводке нет имени студента «Отчисленный Студент». Сводка:\n" + text),
                () -> assertTrue(text.contains("Выпустившийся Студент"),
                        "В сводке нет имени студента «Выпустившийся Студент». Сводка:\n" + text),
                () -> assertTrue(text.contains("4") && text.contains("5"),
                        "В сводке не видны оценки 4 и 5. Сводка:\n" + text),
                () -> assertMentionsState(text, d.enrolledState,
                        "учится", "учиться", "enrolled", "studies"),
                () -> assertMentionsState(text, d.expelledState,
                        "отчислен", "expelled", "expeled"),
                () -> assertMentionsState(text, d.graduatedState,
                        "выпустился", "выпущен", "выпуск", "graduated"));
    }

    // ---------------------------------------------------------------- помощники

    private static void assertRefused(String what, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        } catch (RuntimeException e) {
            fail("Ожидался IllegalArgumentException: " + what + ". А получено "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        fail("Ожидался IllegalArgumentException: " + what + ". Сейчас операция прошла без ошибки.");
    }

    private static void assertMentionsState(String text, Object state, String... words) {
        assertNotNull(state, "Состояние студента не найдено — в задании нужно enum-поле.");
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> accepted = new ArrayList<>();
        String constant = String.valueOf(state).toLowerCase(Locale.ROOT);
        accepted.add(constant);
        for (String word : words) {
            accepted.add(word.toLowerCase(Locale.ROOT));
        }
        for (String word : accepted) {
            if (lower.contains(word)) {
                return;
            }
        }
        fail("В сводке не видно состояния «" + state + "». Можно напечатать русское название"
                + " или значение enum. Сейчас сводка:\n" + text);
    }
}
