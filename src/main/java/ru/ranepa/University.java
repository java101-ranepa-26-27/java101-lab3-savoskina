package ru.ranepa;

import java.util.ArrayList;
import java.util.List;

public class University {
    private final List<Student> students;

    public University() {
        students = new ArrayList<>();
    }

    public Student addStudent(String name) {
        System.out.println("Adding student " + name);
        Student student = Student.createStudent(name);
        student.add(student);
        return student;
    }

    private Student findById(long id) {
        for (Student student : students) {
            if (student.getStudentID() == id) {
               return student;
            }
        }
        return null;
    }

    public Student expellStudent(long id) {
        if (students.isEmpty() || findById(id) == null) {
            throw new IllegalArgumentException("Student with id" + id + "not found");
        }
        Student student = findById(id);
        student.setState(State.DISMISSED);
        return student;
    }

    public void addGrade(long id2, int i) {
    }

    public void transferToNextCourse(long id3) {
    }

    public void expel(long id1) {
    }

    public void printSummary() {
    }
}

