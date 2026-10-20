package ru.ranepa;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private static long id = 0;
    private final String name;
    private final long studentID;
    private int course;
    private State state;
    private final List<Integer> grades;

    private Student(String name) {
        this.studentID = ++id;
        this.name = name;
        this.grades = new ArrayList<>();
        this.course = 1;
        this.state = State.ACTIVE;
    }

    static Student createStudent(String name) {
        return new Student(name);
    }

    public String getName() { return name; }

    public int getCourse() { return course; }
    public State getState() { return state; }
    public long getStudentID() { return studentID; }

    public void setCourse(int course) { this.course = course; }
    public void setState(State state) { this.state = state; }

    public void addGrade(int grade) {
        if (grade < 2 || grade > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 2 до 5");
        }
        grades.add(grade);
    }

    public double average() {
        if (grades.isEmpty()) return 0.0;
        int sum = 0;
        for (int grade : grades) sum += grade;
        return (double) sum / grades.size();
    }

    public void printReport() {
        System.out.println("Студент: " + name);
        System.out.println("Курс: " + course);
        System.out.println("Статус: " + state);
        System.out.println("Оценки: " + grades);
        System.out.println("Средний балл: " + average());
    }
}