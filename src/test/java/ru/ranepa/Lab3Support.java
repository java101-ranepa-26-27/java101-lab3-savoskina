package ru.ranepa;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Помощник тестов практики №3.
 *
 * <p>Тесты не требуют конкретных имён методов и полей. Университет ищется среди классов
 * пакета {@code ru.ranepa}: это класс с конструктором без аргументов, который умеет зачислить
 * студента по имени ({@code String}) и работать с номером билета ({@code long}). Операции
 * распознаются по тому, что они делают с полями студента, а не по названиям.
 *
 * <p>Из имён зафиксированы только {@code ru.ranepa.Main} (точка входа) и
 * {@code ru.ranepa.Student} (его требует задание).
 */
final class Lab3Support {

    static final String STUDENT_CLASS = "ru.ranepa.Student";
    static final int FIRST_COURSE = 1;
    static final int LAST_COURSE = 4;
    static final int MIN_GRADE = 2;
    static final int MAX_GRADE = 5;
    static final int UNKNOWN_ID = 9_999;

    private static final Pattern VARIANT_MARKER =
            Pattern.compile("\\[\\s*вариант\\s*(\\d+)\\s*\\]", Pattern.CASE_INSENSITIVE);

    private static Class<?> studentClass;
    private static Domain domain;
    private static MainResult mainResult;

    private Lab3Support() {
    }

    // ------------------------------------------------------------------ студент

    static synchronized Class<?> studentClass() {
        if (studentClass == null) {
            try {
                studentClass = Class.forName(STUDENT_CLASS);
            } catch (ClassNotFoundException e) {
                fail("Нужен класс " + STUDENT_CLASS
                        + ": в нём живёт студент из задания. Верни файл Student.java на место.");
            }
        }
        return studentClass;
    }

    static List<Field> instanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                fields.add(field);
            }
        }
        return fields;
    }

    static List<Field> fieldsOfType(Class<?> type, Class<?> fieldType) {
        return instanceFields(type).stream()
                .filter(f -> f.getType() == fieldType)
                .collect(Collectors.toList());
    }

    static List<Field> intFields(Class<?> type) {
        return fieldsOfType(type, int.class);
    }

    static List<Field> longFields(Class<?> type) {
        return fieldsOfType(type, long.class);
    }

    static List<Field> booleanFields(Class<?> type) {
        return fieldsOfType(type, boolean.class);
    }

    static List<Field> enumFields(Class<?> type) {
        return instanceFields(type).stream()
                .filter(f -> f.getType().isEnum())
                .collect(Collectors.toList());
    }

    static Object read(Field field, Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError("не удалось прочитать поле " + field.getName()
                    + " у " + target.getClass().getSimpleName(), e);
        }
    }

    static int readInt(Field field, Object target) {
        return ((Number) read(field, target)).intValue();
    }

    static long readLong(Field field, Object target) {
        return ((Number) read(field, target)).longValue();
    }

    static boolean readBoolean(Field field, Object target) {
        return (Boolean) read(field, target);
    }

    static String fieldName(Field field) {
        return field.getDeclaringClass().getSimpleName() + "." + field.getName();
    }

    // ------------------------------------------------------------------ Main

    static synchronized MainResult main() {
        if (mainResult == null) {
            mainResult = runMain();
        }
        return mainResult;
    }

    static MainResult mainThatRan() {
        MainResult result = main();
        if (result.error != null) {
            fail("Main.main упал с исключением " + result.error.getClass().getSimpleName()
                    + ": " + result.error.getMessage()
                    + ". Отказы университета нужно перехватывать через try/catch и печатать,"
                    + " а не давать программе упасть.\n\nВывод программы:\n" + result.all(), result.error);
        }
        return result;
    }

    static int variantNumber() {
        String text = mainThatRan().all();
        Matcher matcher = VARIANT_MARKER.matcher(text);
        Set<Integer> numbers = new LinkedHashSet<>();
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group(1)));
        }
        if (numbers.isEmpty()) {
            fail("В выводе Main нет маркера [вариант N]. Добавь в сценарий строки своего варианта,"
                    + " каждая строка должна начинаться с [вариант N], где N — номер варианта."
                    + "\n\nВывод программы:\n" + text);
        }
        if (numbers.size() > 1) {
            fail("В выводе Main несколько разных номеров варианта: " + numbers
                    + ". Должен быть один — твой вариант.\n\nВывод программы:\n" + text);
        }
        int number = numbers.iterator().next();
        if (number < 1 || number > 24) {
            fail("Номер варианта в Main должен быть от 1 до 24, а напечатан " + number + ".");
        }
        return number;
    }

    private static MainResult runMain() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Throwable error = null;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
            Main.main(new String[0]);
            System.out.flush();
            System.err.flush();
        } catch (Throwable t) {
            error = t;
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return new MainResult(out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8), error);
    }

    static final class MainResult {
        final String out;
        final String err;
        final Throwable error;

        MainResult(String out, String err, Throwable error) {
            this.out = out;
            this.err = err;
            this.error = error;
        }

        String all() {
            return out + (err.isEmpty() ? "" : "\n--- stderr ---\n" + err);
        }
    }

    // ------------------------------------------------------------------ домен

    static synchronized Domain domain() {
        if (domain == null) {
            domain = discoverDomain();
        }
        return domain;
    }

    private static Domain discoverDomain() {
        Domain d = new Domain(findUniversityClass(), studentClass());
        d.enrollMethod = findEnrollMethod(d);
        d.idField = findIdField(d);
        d.gradeMethod = findGradeMethod(d);
        findTransfer(d);
        findExpelledStateAndExpelMethod(d);
        findGraduatedState(d);
        findSummaryMethod(d);
        d.longMethods = singleLongMethods(d.universityClass);
        d.variantMethods = new ArrayList<>(d.longMethods);
        d.variantMethods.remove(d.transferMethod);
        d.variantMethods.remove(d.expelMethod);
        return d;
    }

    // ------------------------------------------------ поиск класса университета

    private static Class<?> findUniversityClass() {
        List<Class<?>> candidates = new ArrayList<>();
        for (Class<?> type : mainClasses()) {
            if (isUniversityCandidate(type)) {
                candidates.add(type);
            }
        }
        List<Class<?>> viable = new ArrayList<>();
        for (Class<?> type : candidates) {
            try {
                if (findEnrollMethodForClass(type) != null) {
                    viable.add(type);
                }
            } catch (Throwable ignored) {
                // не университет — пробуем следующий
            }
        }
        if (viable.size() == 1) {
            return viable.get(0);
        }
        if (viable.size() > 1) {
            List<Class<?>> named = viable.stream()
                    .filter(c -> c.getSimpleName().toLowerCase().contains("univers")
                            || c.getSimpleName().toLowerCase().contains("uni"))
                    .collect(Collectors.toList());
            if (named.size() == 1) {
                return named.get(0);
            }
            throw new AssertionError("Найдено несколько классов, похожих на университет: "
                    + names(viable) + ". Оставь один класс, который создаёт студентов.");
        }
        throw new AssertionError("Не найден класс университета. Нужен отдельный класс в пакете"
                + " ru.ranepa с конструктором без аргументов. Он должен уметь зачислить студента"
                + " по имени (метод с параметром String) и выполнять операции по номеру билета"
                + " (методы с параметром long).");
    }

    private static boolean isUniversityCandidate(Class<?> type) {
        if (type == studentClass() || type == Main.class) {
            return false;
        }
        if (type.isInterface() || type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
            return false;
        }
        if (type.isAnonymousClass() || type.isLocalClass() || type.isMemberClass()) {
            return false;
        }
        if (!"ru.ranepa".equals(type.getPackageName())) {
            return false;
        }
        if (noArgConstructor(type) == null) {
            return false;
        }
        return hasParameter(type, String.class) && hasParameter(type, long.class);
    }

    private static boolean hasParameter(Class<?> type, Class<?> parameter) {
        for (Method method : instanceMethods(type)) {
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == parameter) {
                return true;
            }
        }
        return false;
    }

    private static Constructor<?> noArgConstructor(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            if (Modifier.isPrivate(constructor.getModifiers())) {
                return null;
            }
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Object newUniversity(Class<?> type) {
        Constructor<?> constructor = noArgConstructor(type);
        try {
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Не удалось создать университет через конструктор без аргументов."
                    + " Сделай его доступным для Main.", e);
        }
    }

    private static Method findEnrollMethodForClass(Class<?> type) {
        for (Method method : instanceMethods(type)) {
            if (method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != String.class) {
                continue;
            }
            Object university;
            try {
                university = newUniversity(type);
            } catch (Throwable ignored) {
                continue;
            }
            String name = "Проверка Зачисления";
            Object result;
            try {
                result = invoke(method, university, name);
            } catch (Throwable ignored) {
                continue;
            }
            if (findStudentObject(university, name, result) != null) {
                return method;
            }
        }
        return null;
    }

    private static Method findEnrollMethod(Domain d) {
        Method method = findEnrollMethodForClass(d.universityClass);
        if (method == null) {
            throw new AssertionError("Университет должен уметь зачислять студента по имени:"
                    + " нужен метод с одним параметром String, который создаёт студента.");
        }
        return method;
    }

    // ------------------------------------------------------ поиск полей студента

    private static Field findIdField(Domain d) {
        Object university = newUniversity(d.universityClass);
        EnrollOutcome first = enrollOutcome(d, university, "Билет Первый");
        EnrollOutcome second = enrollOutcome(d, university, "Билет Второй");

        List<Field> longs = longFields(d.studentClass);
        if (longs.isEmpty()) {
            throw new AssertionError("У студента должно быть поле long — номер студенческого билета."
                    + " Сейчас полей типа long нет.");
        }
        if (longs.size() == 1) {
            return longs.get(0);
        }

        List<Field> different = new ArrayList<>();
        for (Field field : longs) {
            long a = readLong(field, first.student);
            long b = readLong(field, second.student);
            if (a != b) {
                different.add(field);
            }
        }
        List<Field> looksLikeIds = different.stream()
                .filter(f -> isIdPair(readLong(f, first.student), readLong(f, second.student)))
                .collect(Collectors.toList());
        if (looksLikeIds.size() == 1) {
            return looksLikeIds.get(0);
        }
        if (different.size() == 1) {
            return different.get(0);
        }
        throw new AssertionError("У студента должно быть одно поле long — номер билета."
                + " Найдено несколько подходящих long-полей: "
                + longs.stream().map(Lab3Support::fieldName).collect(Collectors.joining(", "))
                + ". Номер билета должен быть уникальным и расти по порядку.");
    }

    private static boolean isIdPair(long first, long second) {
        return (first == 1 && second == 2) || (first == 2 && second == 1);
    }

    private static Method findGradeMethod(Domain d) {
        for (Method method : instanceMethods(d.universityClass)) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 2 && isLong(params[0]) && isInt(params[1])) {
                return method;
            }
        }
        throw new AssertionError("Университет должен уметь ставить оценку по номеру билета:"
                + " нужен метод с параметрами (long, int).");
    }

    // ------------------------------------------------------ операции университета

    private static void findTransfer(Domain d) {
        List<Method> candidates = singleLongMethods(d.universityClass);
        for (Method method : candidates) {
            Object university = newUniversity(d.universityClass);
            StudentRef student = enroll(d, university, "Проверка Перевода");
            grade(d, university, student, 5);
            grade(d, university, student, 5);

            Map<Field, Integer> before = intValues(student.object, intFields(d.studentClass));
            Object stateBefore = enumValue(d, student.object);
            try {
                invoke(method, university, student.id);
            } catch (Throwable ignored) {
                continue;
            }
            Field course = changedInt(before, student.object, FIRST_COURSE, FIRST_COURSE + 1);
            if (course == null || !Objects.equals(stateBefore, enumValue(d, student.object))) {
                continue;
            }
            try {
                invoke(method, university, student.id);
            } catch (Throwable ignored) {
                continue;
            }
            if (readInt(course, student.object) == FIRST_COURSE + 2) {
                d.transferMethod = method;
                d.courseField = course;
                return;
            }
        }
        throw new AssertionError("Не найден метод перевода на следующий курс. Нужен метод"
                + " университета с параметром long, который переводит студента с 1 курса на 2."
                + " Найденные методы с long: " + signatures(candidates) + ".");
    }

    private static void findExpelledStateAndExpelMethod(Domain d) {
        List<Field> enums = enumFields(d.studentClass);
        if (enums.isEmpty()) {
            throw new AssertionError("У студента должно быть enum-поле состояния"
                    + " (учится / отчислен / выпустился). Сейчас enum-полей нет.");
        }

        Object university = newUniversity(d.universityClass);
        StudentRef failed = enroll(d, university, "Проверка Отчисления");
        grade(d, university, failed, 2);
        grade(d, university, failed, 3);
        Map<Field, Object> before = enumValues(failed.object, enums);
        transfer(d, university, failed);
        Field stateField = changedEnum(before, failed.object, enums);
        if (stateField == null) {
            throw new AssertionError("Перевод студента со средним баллом ровно 2.5 должен менять"
                    + " enum-состояние на «отчислен», но состояние не изменилось.");
        }
        Object expelled = read(stateField, failed.object);
        Object enrolled = before.get(stateField);
        if (Objects.equals(enrolled, expelled)) {
            throw new AssertionError("Состояния «учится» и «отчислен» не должны совпадать.");
        }

        List<Method> candidates = singleLongMethods(d.universityClass);
        candidates.remove(d.transferMethod);
        for (Method method : candidates) {
            Object u = newUniversity(d.universityClass);
            StudentRef student = enroll(d, u, "Проверка Отчисления 2");
            grade(d, u, student, 5);
            grade(d, u, student, 5);
            int courseBefore = course(d, student);
            try {
                invoke(method, u, student.id);
            } catch (Throwable ignored) {
                continue;
            }
            Object stateAfter = read(stateField, student.object);
            if (Objects.equals(stateAfter, expelled)
                    && course(d, student) == courseBefore) {
                d.stateField = stateField;
                d.enrolledState = enrolled;
                d.expelledState = expelled;
                d.expelMethod = method;
                return;
            }
        }
        throw new AssertionError("Не найден метод отчисления. Нужен метод университета"
                + " с параметром long, который переводит студента в состояние «отчислен»."
                + " Найденные методы с long: " + signatures(candidates) + ".");
    }

    private static void findGraduatedState(Domain d) {
        Object university = newUniversity(d.universityClass);
        StudentRef student = enroll(d, university, "Проверка Выпуска");
        grade(d, university, student, 5);
        grade(d, university, student, 5);
        for (int i = FIRST_COURSE; i < LAST_COURSE; i++) {
            transfer(d, university, student);
        }
        if (course(d, student) != LAST_COURSE) {
            throw new AssertionError("После трёх успешных переводов студент должен быть на 4 курсе,"
                    + " а он на " + course(d, student) + ".");
        }
        if (!Objects.equals(read(d.stateField, student.object), d.enrolledState)) {
            throw new AssertionError("На 4 курсе студент ещё должен учиться, а его состояние: "
                    + read(d.stateField, student.object) + ".");
        }
        transfer(d, university, student);
        Object graduated = read(d.stateField, student.object);
        if (Objects.equals(graduated, d.expelledState)) {
            throw new AssertionError("Перевод с 4 курса при среднем больше 2.5 должен выпускать"
                    + " студента, а не отчислять.");
        }
        if (Objects.equals(graduated, d.enrolledState)) {
            throw new AssertionError("Перевод с 4 курса должен менять состояние на «выпустился»,"
                    + " но состояние осталось «учится».");
        }
        d.graduatedState = graduated;
    }

    private static void findSummaryMethod(Domain d) {
        Object university = newUniversity(d.universityClass);
        StudentRef student = enroll(d, university, "Сводка Тестов");
        grade(d, university, student, 4);

        Method best = null;
        boolean returnsString = false;
        for (Method method : instanceMethods(d.universityClass)) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == String.class) {
                Object text;
                try {
                    text = invoke(method, university);
                } catch (Throwable ignored) {
                    continue;
                }
                if (text != null && text.toString().contains("Сводка Тестов")) {
                    best = method;
                    returnsString = true;
                }
            } else if (returnType == void.class) {
                String text;
                try {
                    text = captureStdout(() -> invoke(method, university));
                } catch (Throwable ignored) {
                    continue;
                }
                if (text.contains("Сводка Тестов")) {
                    best = method;
                    returnsString = false;
                }
            }
        }
        if (best == null) {
            throw new AssertionError("Не найден метод сводки. Нужен метод университета без"
                    + " параметров, который печатает или возвращает String со студентами,"
                    + " их состоянием и оценками.");
        }
        d.summaryMethod = best;
        d.summaryReturnsString = returnsString;
    }

    // -------------------------------------------------------------- зачисление

    static StudentRef enroll(Domain d, Object university, String name) {
        EnrollOutcome outcome = enrollOutcome(d, university, name);
        long id;
        if (outcome.returnedId != null) {
            id = outcome.returnedId;
        } else {
            id = readLong(d.idField, outcome.student);
        }
        return new StudentRef(outcome.student, id, name);
    }

    private static EnrollOutcome enrollOutcome(Domain d, Object university, String name) {
        Object result = invoke(d.enrollMethod, university, name);
        Object student = findStudentObject(university, name, result);
        if (student == null) {
            throw new AssertionError("Метод зачисления " + d.enrollMethod.getName()
                    + "(String) не создал студента с именем «" + name + "». Зачисление должно"
                    + " создавать нового студента и добавлять его в университет.");
        }
        Long returnedId = result instanceof Number
                ? ((Number) result).longValue()
                : null;
        return new EnrollOutcome(student, returnedId);
    }

    private static Object findStudentObject(Object university, String name, Object result) {
        if (result instanceof Optional<?> optional) {
            result = optional.orElse(null);
        }
        if (result != null && studentClass().isInstance(result)) {
            return result;
        }
        if (result instanceof Number number) {
            Object byId = findStudentById(university, number.longValue());
            if (byId != null) {
                return byId;
            }
        }
        List<Object> students = collectStudents(university);
        for (Object student : students) {
            if (nameMatches(student, name)) {
                return student;
            }
        }
        if (students.size() == 1) {
            return students.get(0);
        }
        return null;
    }

    private static boolean nameMatches(Object student, String name) {
        for (Field field : instanceFields(student.getClass())) {
            if (field.getType() == String.class && name.equals(read(field, student))) {
                return true;
            }
        }
        return false;
    }

    private static Object findStudentById(Object university, long id) {
        for (Object student : collectStudents(university)) {
            for (Field field : longFields(student.getClass())) {
                if (readLong(field, student) == id) {
                    return student;
                }
            }
        }
        for (Method method : instanceMethods(university.getClass())) {
            if (method.getParameterCount() == 1 && isLong(method.getParameterTypes()[0])) {
                try {
                    Object result = invoke(method, university, id);
                    if (result != null && studentClass().isInstance(result)) {
                        return result;
                    }
                } catch (Throwable ignored) {
                    // не поиск по билету
                }
            }
        }
        return null;
    }

    private static List<Object> collectStudents(Object root) {
        List<Object> students = new ArrayList<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        collectStudents(root, seen, students, 0);
        for (Method method : instanceMethods(root.getClass())) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (!looksLikeCollection(returnType)) {
                continue;
            }
            try {
                collectStudents(invoke(method, root), seen, students, 1);
            } catch (Throwable ignored) {
                // не список студентов
            }
        }
        return students;
    }

    private static void collectStudents(Object value, Set<Object> seen, List<Object> students,
            int depth) {
        if (value == null || depth > 5) {
            return;
        }
        if (studentClass().isInstance(value)) {
            students.add(value);
            return;
        }
        Class<?> type = value.getClass();
        if (type.isPrimitive() || type.isEnum() || type == String.class
                || Number.class.isAssignableFrom(type) || type == Boolean.class
                || type == Character.class) {
            return;
        }
        if (!seen.add(value)) {
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                collectStudents(element, seen, students, depth + 1);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object element : map.values()) {
                collectStudents(element, seen, students, depth + 1);
            }
            for (Object element : map.keySet()) {
                collectStudents(element, seen, students, depth + 1);
            }
            return;
        }
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                collectStudents(Array.get(value, i), seen, students, depth + 1);
            }
            return;
        }
        for (Field field : instanceFields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                collectStudents(read(field, value), seen, students, depth + 1);
            } catch (Throwable ignored) {
                // поле недоступно
            }
        }
    }

    private static boolean looksLikeCollection(Class<?> type) {
        return Iterable.class.isAssignableFrom(type)
                || Map.class.isAssignableFrom(type)
                || type.isArray();
    }

    // ---------------------------------------------------------------- операции

    static void grade(Domain d, Object university, StudentRef student, int grade) {
        invoke(d.gradeMethod, university, student.id, grade);
    }

    static void transfer(Domain d, Object university, StudentRef student) {
        invoke(d.transferMethod, university, student.id);
    }

    static void expel(Domain d, Object university, StudentRef student) {
        invoke(d.expelMethod, university, student.id);
    }

    static int course(Domain d, StudentRef student) {
        return readInt(d.courseField, student.object);
    }

    static StudentRef goodStudent(Uni university, String name) {
        StudentRef student = university.enroll(name);
        university.grade(student, 5);
        university.grade(student, 5);
        return student;
    }

    static StudentRef atCourse(Uni university, String name, int course) {
        if (course < FIRST_COURSE || course > LAST_COURSE) {
            throw new AssertionError("В тесте запрошен несуществующий курс: " + course);
        }
        StudentRef student = goodStudent(university, name);
        for (int current = FIRST_COURSE; current < course; current++) {
            university.transfer(student);
        }
        return student;
    }

    static StudentRef expelledStudent(Uni university, String name) {
        StudentRef student = university.enroll(name);
        university.expel(student);
        return student;
    }

    static StudentRef graduatedStudent(Uni university, String name) {
        StudentRef student = atCourse(university, name, LAST_COURSE);
        university.transfer(student);
        return student;
    }

    static void assertRefused(String what, Runnable action) {
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

    static Object state(Domain d, StudentRef student) {
        return read(d.stateField, student.object);
    }

    static String summary(Domain d, Object university) {
        if (d.summaryReturnsString) {
            Object text = invoke(d.summaryMethod, university);
            return text == null ? "" : text.toString();
        }
        return captureStdout(() -> invoke(d.summaryMethod, university));
    }

    // ------------------------------------------------------------- рефлексия

    static Object invoke(Method method, Object target, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Не удалось вызвать " + method.getName()
                    + ": " + e.getMessage(), e);
        }
    }

    static List<Method> instanceMethods(Class<?> type) {
        Map<String, Method> methods = new LinkedHashMap<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())
                        || Modifier.isPrivate(method.getModifiers())
                        || method.isSynthetic() || method.isBridge()) {
                    continue;
                }
                String key = method.getName() + signature(method);
                methods.putIfAbsent(key, method);
            }
        }
        return new ArrayList<>(methods.values());
    }

    static List<Method> singleLongMethods(Class<?> type) {
        return instanceMethods(type).stream()
                .filter(m -> m.getParameterCount() == 1 && isLong(m.getParameterTypes()[0]))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static boolean isLong(Class<?> type) {
        return type == long.class || type == Long.class;
    }

    static boolean isInt(Class<?> type) {
        return type == int.class || type == Integer.class;
    }

    private static String signature(Method method) {
        return "(" + Stream.of(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",")) + ")";
    }

    private static String signatures(List<Method> methods) {
        if (methods.isEmpty()) {
            return "нет";
        }
        return methods.stream()
                .map(m -> m.getName() + signature(m))
                .collect(Collectors.joining(", "));
    }

    private static String names(List<Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName).collect(Collectors.joining(", "));
    }

    // --------------------------------------------------------------- значения

    private static Map<Field, Integer> intValues(Object object, List<Field> fields) {
        Map<Field, Integer> values = new LinkedHashMap<>();
        for (Field field : fields) {
            values.put(field, readInt(field, object));
        }
        return values;
    }

    private static Map<Field, Object> enumValues(Object object, List<Field> fields) {
        Map<Field, Object> values = new LinkedHashMap<>();
        for (Field field : fields) {
            values.put(field, read(field, object));
        }
        return values;
    }

    private static Object enumValue(Domain d, Object student) {
        return d.stateField == null ? null : read(d.stateField, student);
    }

    private static Field changedInt(Map<Field, Integer> before, Object object, int from, int to) {
        Field found = null;
        for (Map.Entry<Field, Integer> entry : before.entrySet()) {
            if (entry.getValue() == from && readInt(entry.getKey(), object) == to) {
                if (found != null) {
                    return null;
                }
                found = entry.getKey();
            }
        }
        return found;
    }

    private static Field changedEnum(Map<Field, Object> before, Object object, List<Field> fields) {
        Field found = null;
        for (Field field : fields) {
            if (!Objects.equals(before.get(field), read(field, object))) {
                if (found != null) {
                    return null;
                }
                found = field;
            }
        }
        return found;
    }

    // -------------------------------------------------------------- вывод

    static String captureStdout(ThrowingRunnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            try {
                action.run();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            System.out.flush();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Throwable;
    }

    // ------------------------------------------------------ поиск классов

    private static List<Class<?>> mainClasses() {
        Set<String> names = new LinkedHashSet<>();
        try {
            URI location = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path root = Paths.get(location);
            if (Files.isDirectory(root)) {
                Path packageDir = root.resolve("ru/ranepa");
                if (Files.isDirectory(packageDir)) {
                    try (Stream<Path> files = Files.list(packageDir)) {
                        files.filter(Files::isRegularFile)
                                .map(path -> path.getFileName().toString())
                                .filter(name -> name.endsWith(".class"))
                                .filter(name -> !name.contains("$"))
                                .map(name -> name.substring(0, name.length() - ".class".length()))
                                .forEach(name -> names.add("ru.ranepa." + name));
                    }
                }
            } else if (Files.isRegularFile(root)) {
                try (JarFile jar = new JarFile(root.toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith("ru/ranepa/") && name.endsWith(".class")
                                && !name.contains("$")) {
                            names.add(name.substring(0, name.length() - ".class".length())
                                    .replace('/', '.'));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // ниже — запасной поиск по classpath
        }

        if (names.isEmpty()) {
            try {
                Enumeration<URL> resources = Lab3Support.class.getClassLoader()
                        .getResources("ru/ranepa");
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    if ("file".equals(url.getProtocol())) {
                        Path dir = Paths.get(url.toURI());
                        try (Stream<Path> files = Files.list(dir)) {
                            files.filter(Files::isRegularFile)
                                    .map(path -> path.getFileName().toString())
                                    .filter(name -> name.endsWith(".class"))
                                    .filter(name -> !name.contains("$"))
                                    .map(name -> name.substring(0,
                                            name.length() - ".class".length()))
                                    .filter(name -> !name.endsWith("Test"))
                                    .filter(name -> !name.equals("Lab3Support"))
                                    .forEach(name -> names.add("ru.ranepa." + name));
                        }
                    }
                }
            } catch (Exception ignored) {
                // ниже сообщим, что ничего не нашли
            }
        }

        List<Class<?>> classes = new ArrayList<>();
        for (String name : names) {
            try {
                classes.add(Class.forName(name, false, Lab3Support.class.getClassLoader()));
            } catch (Throwable ignored) {
                // сломанный класс не должен ронять остальные тесты
            }
        }
        return classes;
    }

    // -------------------------------------------------------------- структуры

    static final class StudentRef {
        final Object object;
        final long id;
        final String name;

        StudentRef(Object object, long id, String name) {
            this.object = object;
            this.id = id;
            this.name = name;
        }
    }

    private static final class EnrollOutcome {
        final Object student;
        final Long returnedId;

        EnrollOutcome(Object student, Long returnedId) {
            this.student = student;
            this.returnedId = returnedId;
        }
    }

    static final class Uni {
        final Domain domain;
        final Object object;

        Uni(Domain domain) {
            this.domain = domain;
            this.object = newUniversity(domain.universityClass);
        }

        StudentRef enroll(String name) {
            return Lab3Support.enroll(domain, object, name);
        }

        void grade(StudentRef student, int grade) {
            Lab3Support.grade(domain, object, student, grade);
        }

        void transfer(StudentRef student) {
            Lab3Support.transfer(domain, object, student);
        }

        void expel(StudentRef student) {
            Lab3Support.expel(domain, object, student);
        }

        int course(StudentRef student) {
            return Lab3Support.course(domain, student);
        }

        Object state(StudentRef student) {
            return Lab3Support.state(domain, student);
        }

        String summary() {
            return Lab3Support.summary(domain, object);
        }

        Object invoke(Method method, Object... args) {
            return Lab3Support.invoke(method, object, args);
        }
    }

    static final class Domain {
        final Class<?> universityClass;
        final Class<?> studentClass;
        Method enrollMethod;
        Method gradeMethod;
        Method transferMethod;
        Method expelMethod;
        Method summaryMethod;
        Field idField;
        Field courseField;
        Field stateField;
        Object enrolledState;
        Object expelledState;
        Object graduatedState;
        List<Method> longMethods;
        List<Method> variantMethods;
        boolean summaryReturnsString;

        Domain(Class<?> universityClass, Class<?> studentClass) {
            this.universityClass = universityClass;
            this.studentClass = studentClass;
        }

        Uni uni() {
            return new Uni(this);
        }
    }
}
