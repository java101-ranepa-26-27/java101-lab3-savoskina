package ru.ranepa;

public class Main {
    public static void main(String[] args) {
        // TODO: создать университет
        University university = new University();

        // TODO: зачислить 2–3 студентов и поставить оценки
        long id1 = university.addStudent("Иванов Иван").getStudentID();
        long id2 = university.addStudent("Петров Антон").getStudentID();
        long id3 = university.addStudent("Сидоров Матвей").getStudentID();

        university.addGrade(id1, 5);
        university.addGrade(id1, 4);
        university.addGrade(id1, 5);

        university.addGrade(id2, 2);
        university.addGrade(id2, 3);
        university.addGrade(id2, 2);

        university.addGrade(id3, 4);
        university.addGrade(id3, 5);
        university.addGrade(id3, 4);

        // TODO: перевести студента со средним баллом больше 2.5
        university.transferToNextCourse(id1);

        // TODO: перевести студента с 4 курса
        university.transferToNextCourse(id3);
        university.transferToNextCourse(id3);
        university.transferToNextCourse(id3);
        university.transferToNextCourse(id3);

        // TODO: отчислить студента и показать отказ
        university.transferToNextCourse(id2);
        try {
            university.addGrade(id1, 5);
        } catch (IllegalArgumentException e) {
            System.out.println("Отказ: " + e.getMessage());
        }
        university.expellStudent(id1);

        // TODO: [вариант N] — выполнить свой кейс, строки начинать с [вариант N]
        // TODO: напечатать сводку — в ней должна быть информация из кейса
        university.printSummary();
    }
}
