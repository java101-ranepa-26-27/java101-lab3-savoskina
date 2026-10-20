package ru.ranepa;

public class Main {

    public static void main(String[] args) {
        Student student = Student.create("Иванов Иван");
        student.getGrades().add(4);
        student.getGrades().add(5);
        System.out.println(student.average());
        student.printReport();

        // TODO: создать университет
        // TODO: зачислить 2–3 студентов и поставить оценки
        // TODO: перевести студента со средним баллом больше 2.5
        // TODO: перевести студента со средним баллом 2.5 или меньше
        // TODO: перевести студента с 4 курса
        // TODO: отчислить студента и показать отказ
        // TODO: [вариант N] — выполнить свой кейс, строки начинать с [вариант N]
        // TODO: напечатать сводку — в ней должна быть информация из кейса
    }
}
