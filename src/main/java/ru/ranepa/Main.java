package ru.ranepa;

public class Main {

    public static void main(String[] args) {
        Student student = Student.create("Иванов Иван");
        student.getGrades().add(4);
        student.getGrades().add(5);
        System.out.println(student.average());
        student.printReport();

        // TODO: создать университет
        University university = new University();
        // TODO: зачислить 2–3 студентов и поставить оценки
        long id1 = university.addStudent("Иванов Иван").getStudentId();
        long id2 = university.addStudent("Петров Григорий").getStudentId();
        long id3 = university.addStudent("Сидоров Роман").getStudentId();
        university.addGrade(id1, 5);
        university.addGrade(id1, 4);
        university.addGrade(id2, 2);
        university.addGrade(id2, 3);
        university.addGrade(id3, 4);
        university.addGrade(id3, 5);
        // TODO: перевести студента со средним баллом больше 2.5
        university.transferToNextCourse(id1);
        System.out.println("Иванов переведён на курс: " + university);
        // TODO: перевести студента со средним баллом 2.5 или меньше
        university.transferToNextCourse(id2);
        // TODO: перевести студента с 4 курса
        university.transferToNextCourse(id3);
        university.transferToNextCourse(id3);
        university.transferToNextCourse(id3);
        university.transferToNextCourse(id3);
        // TODO: отчислить студента и показать отказ
        university.expel(id1);
        try {
            university.addGrade(id1, 5);
        } catch (IllegalArgumentException e) {
            System.out.println("Отказ: " + e.getMessage());
        }
        // TODO: [вариант N] — выполнить свой кейс, строки начинать с [вариант N]
        // TODO: напечатать сводку — в ней должна быть информация из кейса
        university.printSummary();
    }
}
