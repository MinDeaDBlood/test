# Stage 38. Чистая сборка Inno Setup без Installer\Default.isl

Проблема stage37 была в том, что BuildInstaller.ps1 временно создавал `Installer\Default.isl` из `Installer\Languages\English.isl`.

Это работало, но выглядело как костыль. Причина была не в секции `[Languages]`, а в секции `[Messages]` внутри `Installer\dt.iss`.

Когда в `.iss` есть собственная секция `[Messages]`, Inno Setup сначала пытается прочитать стандартные сообщения из `Default.isl`. Так как компилятор `ISCC.exe` лежит в папке `Installer`, он искал `Installer\Default.isl` и падал, если файла не было.

В этом этапе секция `[Messages]` удалена из `Installer\dt.iss`. Теперь стандартные сообщения установщика полностью берутся из выбранного `.isl` файла.

Итоговая схема:

```text
Installer\Languages\English.isl
Installer\Languages\Spanish.isl
Installer\Languages\French.isl
Installer\Languages\German.isl
Installer\Languages\Italian.isl
Installer\Languages\Portuguese.isl
```

`Installer\Default.isl` больше не нужен и не создаётся даже временно.

Английский установщика редактируется только в `Installer\Languages\English.isl`.

Проектные строки, которые используются через `{cm:...}`, остаются в секции `[CustomMessages]` каждого `.isl` файла.

Стандартные строки Inno Setup, например кнопки `Next`, `Back`, `Cancel`, `Browse`, `Install`, тоже лежат в соответствующем `.isl` файле выбранного языка.

Компромисс: кастомные заголовки `SetupAppTitle`, `SetupWindowTitle`, `UninstallAppTitle`, `UninstallAppFullTitle` больше не переопределяются в `dt.iss`. Они берутся из языкового файла Inno Setup. Это чище, потому что убирает зависимость от `Default.isl` и не создаёт второго английского источника.
