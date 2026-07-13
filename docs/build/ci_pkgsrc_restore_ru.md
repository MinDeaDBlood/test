# Подготовка NuGet пакетов

Подготовка пакетов использует один локальный bundle:

```text
tools/build/pkgsrc.bundle
```

`nugetpkgprep.bat` запускает:

```text
tools/build/PrepareNuGetPackages.ps1
```

Скрипт делает следующее:

1. Ищет `tools/build/pkgsrc.bundle`.
2. Завершает работу с ошибкой, если bundle отсутствует.
3. Проверяет, что bundle не является Git LFS pointer.
4. Проверяет ZIP сигнатуру.
5. Распаковывает архив в `packages`.
6. Копирует нужные пакеты во вложенные папки `HotInstall` и `DT_ThemeDesigner`.
7. Проверяет `packages.config` и реальные `HintPath` из проектов.
8. Завершает сборку ошибкой, если какой то пакет или DLL отсутствует.

Для обычной CI сборки нужен именно:

```text
tools/build/pkgsrc.bundle
```

Корневой `pkgsrc.zip` больше не используется. Его не нужно поставлять в полном архиве проекта и не нужно поддерживать как совместимый путь.
