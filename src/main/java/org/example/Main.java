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

        List<Group> groups = new ArrayList<>(); // Это массив групп, он нужен, чтобы в конце было удобно выводить ответ.
        GroupFinder finder = new GroupFinder(); // Объект для быстрого поиска групп, которым должна принадлежать строка.

        HashSet<String> uniqueStrings = new HashSet<>(); // Множество используется для удаления повторяющихся строк.

        while (in.hasNext()) {
            int[] nums;
            String newString = in.nextLine();

            if (uniqueStrings.contains(newString)){ // Если такая строка уже была, не обрабатываем ее 2 раз, просто переходим к следующей
                continue;
            } else {
                uniqueStrings.add(newString);
            }

            try {
                nums = parseString(newString); // пропускаем некорректно записанные строки
            } catch (NumberFormatException e) {
                continue;
            }

            ArrayList<Group> suitableGroup = finder.find(nums); // это массив групп, которым должна принадлежать строка nums

            if (suitableGroup.isEmpty()) {
                // если таких групп нет, то создаем новую
                Group newGroup = new Group();
                newGroup.add(nums);
                finder.add(newGroup);
                groups.add(newGroup);
            } else {
                // если есть, то все группы нужно объединить в одну
                Group mainGroup = suitableGroup.get(0); // В этой группе будем объединять
                for (int i = 1; i < suitableGroup.size(); i++) {
                    // все остальные группы вливаем в главную и удаляем их
                    mainGroup.add(suitableGroup.get(i));
                    groups.remove(suitableGroup.get(i));
                }

                mainGroup.add(nums); // добавляем нашу новую строку

                finder.add(mainGroup); // и обновляем объект для поиска
            }
        }

        // считаем количество групп с более чем одним элементом
        groups = groups.stream().sorted((x, y) -> y.strs.size() - x.strs.size()).toList();

        String answerFilePath = filePath.replace(".txt", "-answer.txt");
        writeFile(groups, answerFilePath);
    }

    // Преобразует считанную строку в массив целых чисел. Так как первые две цифры числа всегда одинаковы, то они удаляются
    // для экономии памяти (используем int вместо long)
    public static int[] parseString(String str) {
        String[] strArr = str.split(";");
        int[] nums = new int[strArr.length];

        for (int i = 0; i < strArr.length; i++) {
            String strNum = strArr[i].substring(1, strArr[i].length() - 1);
            nums[i] = strNum.isEmpty() ? 0 : (int) (Long.parseLong(strNum) % 1000000000L);
        }

        return nums;
    }

    // Следующие 2 метода нужны для записи ответа в файл
    public static void writeFile(List<Group> groups, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            long count = groups.stream().filter(x -> x.strs.size() > 1).count();
            writer.write(count + "\n");

            for (int i = 0; i < groups.size(); i++) {
                writer.write("Группа " + (i + 1) + "\n");

                writeGroup(writer, groups.get(i));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void writeGroup(FileWriter writer, Group group) throws IOException {
        for (Str str : group.strs) {
            for (int j = 0; j < str.nums.length; j++) {
                if (str.nums[j] == 0){
                    writer.write("\"\"");
                }else {
                    writer.write("\"79" + str.nums[j] + "\"");
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
    public ArrayList<HashMap<Integer, Group>> arr;


    GroupFinder() {
        arr = new ArrayList<>();
    }

    // Метод для поиска групп, которым должна принадлежать конкретная строка
    public ArrayList<Group> find(int[] str) {
        ArrayList<Group> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            if (str.length <= i)
                break;
            if (str[i] == 0)
                continue;
            Group found = arr.get(i).get(str[i]);

            if (found != null)
                result.add(found);
        }

        return result;
    }

    // Метод используемый чтобы добавить только что созданную группу.
    // Также используется при объединении нескольких групп в одну.
    public void add(Group newGrp) {
        for (Str str : newGrp.strs) {
            for (int i = 0; i < str.nums.length; i++) {
                if (arr.size() <= i) {
                    arr.add(new HashMap<>());
                }

                arr.get(i).put(str.nums[i], newGrp);
            }
        }
    }
}

//Класс создан для того, чтобы код был более читаем. Представляет собой группу в состав которой входит множество строк.
class Group {
    public HashSet<Str> strs;

    Group() {
        strs = new HashSet<>();
    }

    public void add(int[] str) {
        strs.add(new Str(str));
    }

    public void add(Group anotherGrp) {
        strs.addAll(anotherGrp.strs);
    }
}

// Тип для использования внутри множества. Представляет собой одну строку, хранимую в формате массива чисел
class Str {
    public int[] nums;

    Str(int[] nums) {
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