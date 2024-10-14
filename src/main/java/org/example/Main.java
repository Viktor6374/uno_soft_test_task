package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        long begin = System.nanoTime();

        solve(args[0]);

        long end = System.nanoTime();
        System.out.println("Время выполнения: " + (end - begin) / 1000000 + " мсек");
    }

    public static void solve(String filePath){
        Scanner in;
        try {
            in = new Scanner(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла");
            return;
        }

        HashSet<Group> groups = new HashSet<>(); // Это массив групп, он нужен, чтобы в конце было удобно выводить ответ.
        GroupFinder finder = new GroupFinder(); // Объект для быстрого поиска групп, которым должна принадлежать строка.

        HashSet<String> uniqueStrings = new HashSet<>(); // Множество используется для удаления повторяющихся строк.

        while (in.hasNext()) {
            Number[] nums;
            String newString = in.nextLine();

            if (uniqueStrings.contains(newString)){ // Если такая строка уже была, не обрабатываем ее 2 раз, просто переходим к следующей
                continue;
            } else {
                uniqueStrings.add(newString);
            }

            try {
                nums = parseString2(newString); // пропускаем некорректно записанные строки
            } catch (NumberFormatException e) {
                continue;
            }

            ArrayList<Group> suitableGroup = finder.find(nums); // это массив групп, которым должна принадлежать строка nums

            if (suitableGroup.isEmpty()) {
                // если таких групп нет, то создаем новую
                Group newGroup = new Group();
                newGroup.add(nums);
                finder.add(newGroup, nums);
                groups.add(newGroup);
            } else {
                // если есть, то все группы нужно объединить в одну
                Group mainGroup = suitableGroup.get(0); // В этой группе будем объединять
                suitableGroup.remove(0);

                mainGroup.add(suitableGroup);

                mainGroup.add(nums); // добавляем нашу новую строку
                finder.add(mainGroup, nums); // и обновляем объект для поиска
            }
        }

        

        // считаем количество групп с более чем одним элементом
        List<Group> listGroups = groups.stream().filter(g -> !g.hasParent()).sorted((x, y) -> y.countGroupSize() - x.countGroupSize()).toList();

//        for (Group group : listGroups){
//            group.checkParent();
//        }
        String answerFilePath = filePath.replace(".csv", "-answer.csv");
        writeFile(listGroups, answerFilePath);
    }

    // Преобразует считанную строку в массив целых чисел. Так как первые две цифры числа всегда одинаковы, то они удаляются
    // для экономии памяти (используем int вместо long)
    // Этот метод работает для первого из файлов
    public static Number[] parseString1(String str) {
        String[] strArr = str.split(";");
        Number[] nums = new Number[strArr.length];

        for (int i = 0; i < strArr.length; i++) {
            String strNum = strArr[i].substring(1, strArr[i].length() - 1);

            nums[i] = new Number(strNum.isEmpty() ? 0 : (int) (Long.parseLong(strNum) % 1000000000L), 0);
        }

        return nums;
    }

    // Этот метод работает для второго(большого) файла
    public static Number[] parseString2(String str) {
        String[] strArr = str.split(";", -1);
        Number[] nums = new Number[strArr.length];

        for (int i = 0; i < strArr.length; i++) {
            int num;
            int zero = 0;
            if (strArr[i].isEmpty()){
                num = 0;
            } else {
                String strNum = strArr[i].substring(1, strArr[i].length() - 1);
                if (strNum.charAt(strNum.length() - 1) == '0')
                    zero = 1;
                num = Math.round(Float.parseFloat(strNum) * 100);
            }

            nums[i] = new Number(num, zero);
        }

        return nums;
    }

    // Следующие 3 метода нужны для записи ответа в файл
    public static void writeFile(List<Group> groups, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            long count = groups.stream().filter(x -> x.countGroupSize() > 1).count();
            writer.write(count + "\n");

            for (int i = 0; i < groups.size(); i++) {
                writer.write("Группа " + (i + 1) + "\n");

                writeGroup2(writer, groups.get(i));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    // метод работает для первого файла
    public static void writeGroup1(FileWriter writer, Group group) throws IOException {
        for (Str str : group.getStrs()) {
            for (int j = 0; j < str.nums.length; j++) {
                if (str.nums[j].num == 0){
                    writer.write("\"\"");
                }else {
                    writer.write("\"79" + str.nums[j].num + "\"");
                }
                if (j != str.nums.length - 1)
                    writer.write(";");
            }

            writer.write("\n");
        }
    }

    // метод работает для второго(большого) файла
    public static void writeGroup2(FileWriter writer, Group group) throws IOException {
        for (Str str : group.getGroupStrs()) {
            for (int j = 0; j < str.nums.length; j++) {
                if (str.nums[j].num != 0){
                    int realPart = str.nums[j].num % 100;
                    if (str.nums[j].num % 10 == 0) {
                        realPart /= 10;
                        if (str.nums[j].insignificantZeros == 1)
                            realPart *=  10;
                    }

                    writer.write("\"" + str.nums[j].num / 100 + "." + realPart + "\"");
                }

                if (j != str.nums.length - 1)
                    writer.write(";");
            }

            writer.write("\n");
        }
    }
}

// Класс для быстрого поиска групп, которым должна принадлежать конкретная строка
class GroupFinder {
    // Эта структура позволяет найти все группы, которым должна принадлежать строка за O(n), где n - количество колонок.
    // Здесь внешний массив - это массив колонок. Соответственно нулевой индекс - это первая колонка, первый индекс - вторая и тд.
    // Во внутреннем словаре key - это значение, которое может быть в соотвествующей колонке, а value - это группа,
    // которой принадлежит это значние. Если у очередной строки совпадает значение в какой то колонке со значением имеющемся
    // в ключе, то нужно отнести ее к группе в значении.
    public ArrayList<HashMap<Number, Group>> arr;


    GroupFinder() {
        arr = new ArrayList<>();
    }

    // Метод для поиска групп, которым должна принадлежать конкретная строка
    public ArrayList<Group> find(Number[] str) {
        ArrayList<Group> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            if (str.length <= i)
                break;
            if (str[i].num == 0)
                continue;
            Group found = arr.get(i).get(str[i]);

            if (found != null)
                result.add(found);
        }

        return result;
    }

    // Метод используемый чтобы добавить только что созданную группу.
    // Также используется при объединении нескольких групп в одну.
    public void add(Group newGrp, Number[] nums) {
        for (int i = 0; i < nums.length; i++) {
            if (arr.size() <= i) {
                arr.add(new HashMap<>());
            }

            if (nums[i].num == 0){
                continue;
            }

            arr.get(i).put(nums[i], newGrp);
        }

    }
}

//Класс создан для того, чтобы код был более читаем. Представляет собой группу в состав которой входит множество строк.
// Для того, чтобы объекты не изменять кучу раз, они просто ссылаются друг на друга при помощи отношений вида родитель-ребенок
class Group {
    private HashSet<Str> strs;
    private HashSet<Group> children;
    private Group parent;
    // уникальный номер объекта. Нужен чтобы однозначно определить принадлежат ли объекты одной группе.
    // Если у двух объектов совпадает номер самого верхнего родителя, значит у них общий родитель и они в одной группе.
    private int number;
    // counter нужен для присвоения уникальных номеров
    private static int counter;

    Group() {
        strs = new HashSet<>();
        children = new HashSet<>();
        parent = null;
        number = counter++;
    }
    
    public boolean hasParent(){
        return parent != null;
    }

    public int getNumber(){
        if (hasParent()){
            return parent.getNumber();
        }

        return number;
    }

    public List<Str> getGroupStrs(){
        if (hasParent()){
            return parent.getGroupStrs();
        }

        return getStrs();
    }

    public List<Str> getStrs(){
        List<Str> result = new ArrayList<>(strs);
//        System.out.println(result.get(0).nums.length + ";");
        result.addAll(children.stream().flatMap(g -> g.getStrs().stream()).toList());
        return result;
    }

    public int countGroupSize(){
        if (hasParent()){
            return parent.countGroupSize();
        }

        return getSize();
    }

    private int getSize(){
        return strs.size() + children.stream().mapToInt(Group::getSize).sum();
    }

    public void add(Number[] str) {
        strs.add(new Str(str));
    }

    public void add(Group anotherGrp) {
        if (this.getNumber() == anotherGrp.getNumber()) {
            return;
        }
//        strs.addAll(anotherGrp.strs);
        if (hasParent()){
            parent.add(anotherGrp);
            return;
        }
        Group newChild = anotherGrp.addParent(this);
        children.add(newChild);
    }

    private Group addParent(Group newParent){
        if (hasParent()){
            return parent.addParent(newParent);
        }

        parent = newParent;
        return this;
    }

    public void add(List<Group> groups){
        for (Group group : groups)
            this.add(group);
    }
}

// Тип для использования внутри множества. Представляет собой одну строку, хранимую в формате массива чисел
class Str {
    public Number[] nums;

    Str(Number[] nums) {
        this.nums = nums;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(nums);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Str) {
            return Arrays.equals(nums, ((Str) o).nums);
        }
        return false;
    }
}

// Этот объект нужен так как в файле присутствуют вещественные числа, на конце которых есть незначащие нули
// и нужно как то различать числа с и без нулей
class Number{
    public int num;
    public int insignificantZeros;

    Number(int num, int insignificantZeros){
        this.num = num;
        this.insignificantZeros = insignificantZeros;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(num) / 2 + Integer.hashCode(insignificantZeros) / 2;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Number) {
            Number obj = (Number) o;
            return this.num == obj.num && this.insignificantZeros == obj.insignificantZeros;
        }
        return false;
    }
}