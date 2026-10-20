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
        students.add(student);
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

    public void expellStudent(long id) {
        if (students.isEmpty() || findById(id) == null) {
            throw new IllegalArgumentException("Student with id " + id + " not found");
        }
        Student student = findById(id);
        assert student != null;
        student.setState(State.DISMISSED);
    }

    public void addGrade(long id, int grade) {
        if (students.isEmpty() || findById(id) == null) {
            throw new IllegalArgumentException("Student with id " + id + " not found");
        }
        Student student = findById(id);
        assert student != null;
        if (student.getState() != State.ACTIVE) {
            throw new IllegalArgumentException("Student is already dismissed or graduated");
        }
        student.addGrade(grade);
    }

    public void transferToNextCourse(long id) {
        if (students.isEmpty() || findById(id) == null) {
            throw new IllegalArgumentException("Student with id " + id + " not found");
        }
        Student student;
        student = findById(id);
        assert student != null;
        if (student.getState() != State.ACTIVE) {
            throw new IllegalArgumentException("Student is already dismissed or graduated");
        }

        if (student.average() > 2.5) {
            if (student.getCourse() == 4) {
                student.setState(State.GRADUATED);
                System.out.println("Student " + student.getName() + " graduated.");
            } else {
                student.setCourse(student.getCourse() + 1);
            }
        } else {
            System.out.println("Student " + student.getName() + " dismissed due to low average grade.");
            student.setState(State.DISMISSED);
        }
    }
    public void printSummary() {
        System.out.println("Summary");
        for (Student student : students) {
            student.printReport();
            System.out.println("-");
        }
    }
}
