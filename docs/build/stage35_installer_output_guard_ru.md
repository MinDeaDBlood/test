# Stage 35. Проверка сборки установщика перед публикацией

В этом этапе исправлена проблема, когда workflow доходил до публикации релиза, но файла `Installer\Output\dt_setup.exe` уже не было. Из за этого `PublishReleaseAsset.ps1` падал на этапе релиза с сообщением, что установщик не найден.

Причина не в схеме `English.isl`. Ошибка означала, что publish шаг получил пустой путь к установщику. Теперь сборка установщика вынесена в отдельную явную проверку перед публикацией.

Что изменено:

1. Добавлен скрипт `tools\build\BuildInstaller.ps1`.
2. Workflow теперь после MSBuild отдельно запускает сборку Inno Setup установщика.
3. Скрипт проверяет наличие `Installer\ISCC.exe`.
4. Скрипт проверяет наличие `Installer\dt.iss`.
5. Скрипт проверяет наличие всех языковых файлов установщика в `Installer\Languages`.
6. Скрипт копирует готовые файлы программы из `bin\Debug` в `Installer\files`.
7. Скрипт запускает Inno Setup из папки `Installer`, чтобы относительные пути работали предсказуемо.
8. Скрипт проверяет, что появился файл `Installer\Output\dt_setup.exe`.
9. Если установщик не создан, workflow падает до публикации релиза и показывает понятную ошибку.

Также обновлён post build в `DISMTools.vbproj`. Теперь, если компилятор Inno Setup отсутствует или не создал `dt_setup.exe`, сборка падает сразу, а не продолжает публикацию.

Схема языков установщика оставлена простой:

```text
Installer\Languages\English.isl
Installer\Languages\Spanish.isl
Installer\Languages\French.isl
Installer\Languages\German.isl
Installer\Languages\Italian.isl
Installer\Languages\Portuguese.isl
```

Каждый язык редактируется в одном своём `.isl` файле. `compiler:Default.isl` в `dt.iss` не используется.
