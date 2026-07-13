# Stage 36. Разделение сборки приложения и сборки установщика

## Что было не так

MSBuild собирал приложение и внутри post build события сразу запускал Inno Setup. После этого workflow запускал отдельный скрипт `tools\build\BuildInstaller.ps1`, который должен был собирать тот же самый установщик ещё раз.

Из за этого получалась смешанная схема. Ошибка Inno Setup попадала внутрь MSBuild как `MSB3077`, а реальная причина терялась среди текста post build события.

## Что изменено

Теперь `DISMTools.vbproj` только готовит выходную папку приложения `bin\Debug`.

Сборка установщика вынесена в отдельный шаг workflow:

```text
MSBuild
BuildInstaller.ps1
VerifyInstallerLocalization.ps1
PublishReleaseAsset.ps1
```

Файл `dt_setup.exe` создаёт только `tools\build\BuildInstaller.ps1`.

## Почему так лучше

У приложения и установщика теперь разные зоны ответственности.

MSBuild отвечает за сборку DISMTools и копирование нужных файлов в `bin\Debug`.

`BuildInstaller.ps1` отвечает за подготовку `Installer\files`, запуск `Installer\ISCC.exe` и проверку наличия `Installer\Output\dt_setup.exe`.

Если Inno Setup упадёт, ошибка появится на отдельном шаге `Build installer output`, а не внутри общего `MSB3077` из MSBuild.

## Что важно для локализации

Схема установщика не изменилась:

```text
Installer\Languages\English.isl
Installer\Languages\Spanish.isl
Installer\Languages\French.isl
Installer\Languages\German.isl
Installer\Languages\Italian.isl
Installer\Languages\Portuguese.isl
```

Один язык установщика редактируется в одном `.isl` файле.

`compiler:Default.isl` не используется.
