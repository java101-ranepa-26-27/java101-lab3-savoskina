package ru.ranepa;

import java.util.ArrayList;
import java.util.List;

public class Student {

    private final String name;
    // тут статик поле
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

    // тут статик метод

    public String getName() {
        return name;
    }

    public List<Integer> getGrades() {
        return grades;
    }

    public double average() {
        if (grades.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (int grade : grades) {
            sum += grade;
        }
        return (double) sum / grades.size();
    }

    public void setCourse(int course) {
        this.course = course;
    }

    public void setState(State state) {
        this.state = state;
    }

    private String report() {
        return "Студент: " + name + System.lineSeparator()
                + "Оценки: " + grades + System.lineSeparator()
                + "Средний балл: " + average();
    }

    public void printReport() {
        System.out.println(report());
    }

    public int getCourse() {
        return course;
    }

    public State getState() {
        return state;
    }

    public long getStudentID() {
        return studentID;
    }
}
