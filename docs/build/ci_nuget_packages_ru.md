# Подготовка NuGet пакетов в CI

CI сборка использует один локальный источник NuGet пакетов:

```text
tools/build/pkgsrc.bundle
```

`pkgsrc.bundle` является обычным ZIP архивом с другим расширением. Такой путь выбран, чтобы не возвращать корневой `pkgsrc.zip` и не создавать путаницу с ZIP правилами и Git LFS.

## Как работает процесс

1. Workflow делает checkout.
2. `nugetpkgprep.bat` запускает `tools/build/PrepareNuGetPackages.ps1`.
3. Скрипт требует наличие `tools/build/pkgsrc.bundle`.
4. Архив проверяется на Git LFS pointer и ZIP сигнатуру.
5. Пакеты распаковываются в нужные папки.
6. Скрипт проверяет ключевые DLL, targets файлы и `HintPath` из проектов.

Если `tools/build/pkgsrc.bundle` отсутствует, сборка должна остановиться с понятной ошибкой. Корневой `pkgsrc.zip` больше не используется и не является совместимым источником.

## Какие папки пакетов создаются

```text
packages
Helpers/extps1/PE_Helper/tools/HotInstall/packages
Tools/DT_ThemeDesigner/packages
```

Это нужно потому, что разные старые проекты используют разные относительные `HintPath`.

## Что проверять при новых зависимостях

Если добавляется новый NuGet пакет, нужно обновить один из файлов `packages.config`, затем пересобрать `tools/build/pkgsrc.bundle` через `updatelocalpkg.bat`. Если проект использует нестандартный `HintPath`, нужно добавить отдельную проверку или копирование в `PrepareNuGetPackages.ps1`.
