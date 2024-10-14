# Запуск:<br>
Компиляция - `./gradlew build`<br>
Запуск - `java -Xmx1G -jar .\build\libs\uno_soft_test_task-1.0-SNAPSHOT.jar .\lng-big.csv`<br>
Если неправильно выводит русские буквы, нужно явно указать кодировку - `java -Xmx1G -jar .\build\libs\uno_soft_test_task-1.0-SNAPSHOT.jar .\lng-big.csv -Dfile.encoding=UTF-8`<br>
# Результат:<br>
Время работы будет выведено в консоль. Требуемый файл будет создан или перезаписан в главной папке репозитория под именем "lng-big-answer.csv"
