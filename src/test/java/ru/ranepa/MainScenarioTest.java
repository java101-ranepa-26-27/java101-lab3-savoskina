package ru.ranepa;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Сценарий {@code Main.main}: программа проходит все шаги и печатает три состояния.
 * Правила проверяются отдельными тестами по поведению.
 */
@Timeout(30)
class MainScenarioTest {

    @Test
    @DisplayName("Main: три состояния")
    void scenarioShowsStates() {
        Lab3Support.MainResult result = Lab3Support.mainThatRan();
        String text = result.all();
        Lab3Support.Domain d = Lab3Support.domain();

        assertFalse(text.isBlank(), "Main ничего не напечатал.");
        assertAll(
                () -> assertContainsState(text, d.enrolledState,
                        "учится", "учиться", "enrolled", "studies", "active"),
                () -> assertContainsState(text, d.expelledState,
                        "отчислен", "expelled", "expeled", "dropped"),
                () -> assertContainsState(text, d.graduatedState,
                        "выпустился", "выпуск", "graduated", "done"));
    }

    private static void assertContainsState(String text, Object state, String... words) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> accepted = new ArrayList<>();
        accepted.add(String.valueOf(state).toLowerCase(Locale.ROOT));
        for (String word : words) {
            accepted.add(word.toLowerCase(Locale.ROOT));
        }
        for (String word : accepted) {
            if (lower.contains(word)) {
                return;
            }
        }
        throw new AssertionError("В сценарии Main не видно состояния: " + accepted
                + ". Напечатай его в сценарии или в сводке. Вывод:\n" + text);
    }
}
