# Do/Don't 

## **Нейминг**

1. Следуйте [правилам нейминга Java](https://www.oracle.com/java/technologies/javase/codeconventions-namingconventions.html):
   - **Классы**: `PascalCase` (Student, StudentManager)
   - **Методы/переменные**: `camelCase` (getStudent, studentName)  
   - **Константы**: `UPPER_SNAKE_CASE` (MAX_STUDENTS)
   - **Пакеты**: `lowercase` (com.hse.java)

2. **Не используйте** сокращения и транслит
   ```java
   // Плохо
   class Thread { void joinStudent(Student s) {} }
   
   // Хорошо  
   class StudyGroup { void addStudent(Student student) {} }

3. Используйте **английские термины**: `getName()`, `calculateTotal()`, `userList`

## **Do (Делай так):**

1. **Следуйте кодстайлу** Google Java Style или Oracle conventions
2. **Семантические коммиты**: `feat: add student validation`, `fix: correct average calculation`
3. **Инициализация через конструкторы**:
   ```java
   // Плохо
   Student student = new Student();
   student.setName("Иван");
   
   // Хорошо
   Student student = new Student("Иван");
   ```
4. **Private поля по умолчанию** — минимизируйте `public`
5. **Валидация в конструкторе**:
   ```java
   public Student(String name) {
       if (name == null || name.isEmpty()) {
           throw new IllegalArgumentException("Name cannot be empty");
       }
       this.name = name;
   }
   ```
6. **Guard clauses** вместо вложенности:
   ```java
   // Плохо - вложенность
   if (students != null) {
       if (!students.isEmpty()) {
           if (students.get(0) != null) { ... }
       }
   }
   
   // Хорошо - guard clauses
   if (students == null || students.isEmpty()) return;
   Student first = students.get(0);
   if (first == null) return;
   ```
7. **Иммутабельность** через `final`:
   ```java
   public final class Student {
       private final String name;
       public Student(String name) { this.name = name; }
       public String getName() { return name; }
   }
   ```
8. **Streams вместо циклов**:
   ```java
   // Плохо
   List<Integer> odds = new ArrayList<>();
   for (int num : numbers) {
       if (num % 2 != 0) odds.add(num);
   }
   
   // Хорошо
   List<Integer> odds = numbers.stream()
       .filter(n -> n % 2 != 0)
       .toList();
   ```
9. **Optional вместо null**:
   ```java
   public Optional<Student> findStudent(String id) { ... }
   ```
10. Соблюдайте принципы [DRY, KISS, YAGNI](https://habr.com/ru/articles/144611/)

- **DRY - Don’t repeat yourself**. Вместо копирования одного куска кода, рекомендуется вынести его в метод для
  дальшейшего переиспользования.
- **KISS - Keep it stupid simple**. Старайтесь делать методы максимально атомарными и не переусложнять логику. Для
  большой сложной операции рекомендуется декомпозировать сценарий на несколько простых.
- **YAGNI - You ain’t gonna need this**. Не добавляйте преждевременные абстракции для того чтобы поддержать какой-то
  сценарий из будущего, для которого ещё неизвестны бизнес требования. Проектируйте непостредственно ту предметную
  область, которая описана в текущей задаче.

## **Don't (Не делай так):**

1. **Не используйте магические числа**:
   ```java
   // Плохо
   if (age > 18) { ... }
   
   // Хорошо  
   private static final int ADULT_AGE = 18;
   if (age > ADULT_AGE) { ... }
   ```

2. **Не ловите и игнорируйте исключения**:
   ```java
   // Плохо
   try { ... } catch (Exception e) {} // 🤮
   
   // Хорошо
   try { ... } catch (IllegalArgumentException e) {
       log.error("Invalid input", e);
   }
   ```

3. **Не используйте `null` для "не найдено"**:
   ```java
   // Плохо
   public Student getStudent(String id) { ... return null; }
   
   // Хорошо
   public Optional<Student> findStudent(String id) { ... }
   ```

4. **Не используйте циклы для поиска**:
   ```java
   // Плохо
   for (Student s : students) {
       if (s.getId().equals(id)) return s;
   }
   
   // Хорошо
   return students.stream()
       .filter(s -> s.getId().equals(id))
       .findFirst()
       .orElse(null);
   ```

5. **Не комментируйте очевидное**:
   ```java
   // Плохо
   public void printHello() {
       // Выводит приветствие
       System.out.println("Hello!");
   }
   ```

6. **Не используйте примитивы для доменных типов**:
   ```java
   // Плохо - int для денег
   int price;
   
   // Хорошо - класс
   public class Money {
       private final int amount;
       private final String currency;
   }
   ```
