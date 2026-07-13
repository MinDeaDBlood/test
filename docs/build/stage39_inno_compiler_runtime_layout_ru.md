# Stage 39. Разделение файлов компилятора Inno Setup и переводов установщика

Проблема из GitHub Actions показала реальное поведение Inno Setup. Даже когда все языки подключены через `Installer\Languages`, компилятор всё равно читает базовый файл `Default.isl` как часть своего рабочего окружения.

Ошибка была такой:

```text
Couldn't open include file "Installer\Default.isl"
```

Правильное решение не в том, чтобы создавать `Default.isl` временно, и не в том, чтобы снова смешивать его с переводами проекта. Правильное решение такое:

```text
Installer\Compiler
```

хранит файлы самого компилятора Inno Setup.

```text
Installer\Languages
```

хранит редактируемые языки установщика DISMTools.

Теперь `ISCC.exe` лежит в `Installer\Compiler`, и его служебный `Default.isl` лежит рядом с ним. Это обычная часть поставки компилятора, а не файл перевода проекта.

Редактировать переводы установщика нужно только здесь:

```text
Installer\Languages\English.isl
Installer\Languages\Spanish.isl
Installer\Languages\French.isl
Installer\Languages\German.isl
Installer\Languages\Italian.isl
Installer\Languages\Portuguese.isl
```

Файл `Installer\Compiler\Default.isl` не предназначен для редактирования переводов DISMTools. Он нужен компилятору как базовый системный файл Inno Setup.

Если нужно изменить английский текст кнопки, страницы или строки установщика, менять нужно `Installer\Languages\English.isl`.

Если обновляется версия Inno Setup, нужно обновить содержимое папки `Installer\Compiler`, включая `Default.isl`, из той же версии Inno Setup.
