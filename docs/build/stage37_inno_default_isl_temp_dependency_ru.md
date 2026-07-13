# Stage 37. Временный Default.isl для компилятора Inno Setup

## Что было не так

GitHub Actions показал точную ошибку Inno Setup:

```text
Couldn't open include file "Installer\Default.isl"
```

Это не ошибка выбора языка пользователем. Это внутреннее требование компилятора Inno Setup. При наличии секции `[Messages]` в `dt.iss` компилятор пытается прочитать файл `Default.isl` рядом с `ISCC.exe`.

При этом для сопровождения переводов нам всё равно нужна понятная схема:

```text
один язык установщика = один файл в Installer\Languages
```

## Как исправлено

Постоянный файл `Installer\Default.isl` в проект не возвращён.

Единственный файл английского языка, который нужно редактировать, остаётся здесь:

```text
Installer\Languages\English.isl
```

Перед запуском `ISCC.exe` скрипт `tools\build\BuildInstaller.ps1` временно копирует:

```text
Installer\Languages\English.isl
```

в:

```text
Installer\Default.isl
```

После завершения работы `ISCC.exe` временный файл удаляется.

## Почему так лучше

Для человека, который редактирует локализацию, ничего не меняется. Английский установщика редактируется только в `Installer\Languages\English.isl`.

Для Inno Setup появляется нужный технический файл `Default.isl`, но только на время сборки.

Так мы не держим два английских файла в репозитории и не заставляем переводчика искать, где именно править английский текст.

## Проверка

`VerifyInstallerLocalization.ps1` теперь дополнительно проверяет, что после сборки не остался постоянный файл:

```text
Installer\Default.isl
```

Если такой файл остался, проверка остановит workflow.

## Статус после stage38

Этот подход заменён в stage38. Временный `Installer\Default.isl` больше не создаётся. Вместо этого из `Installer\dt.iss` удалена секция `[Messages]`, которая и заставляла Inno Setup искать `Default.isl`.
