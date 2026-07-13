# Stage 62. Путь установки stable версии

## Что исправлено

Stable установщик больше не устанавливает программу в папку:

```text
C:\Program Files\DISMTools\Stable
```

Теперь stable версия устанавливается в корневую папку программы:

```text
C:\Program Files\DISMTools
```

Preview версия по прежнему может использовать отдельную папку:

```text
C:\Program Files\DISMTools\Preview
```

## Что изменено

1. В `Installer\dt.iss` значение `pfDir` изменено на `{commonpf}\DISMTools`.

2. `DefaultDirName` и `UninstallFilesDir` используют это же значение через `{#pfDir}`.

3. `Installer\PrepareISScript.ps1` обновлён так, чтобы stable сборка возвращала путь установки в корень `DISMTools`, а preview сборка могла переключать путь на `DISMTools\Preview`.

4. `tools\build\VerifyInstallerLocalization.ps1` теперь проверяет путь установки и не даст вернуть `DISMTools\Stable` для stable установщика.

5. Шаблоны issue обновлены. Для stable логов теперь указан путь `\Program Files\DISMTools\logs`, а для preview `\Program Files\DISMTools\Preview\logs`.

## Что не менялось

Ветки настроек в реестре `Software\DISMTools\Stable` оставлены как есть. Это не папка установки, а раздел настроек stable канала. Программа уже использует такую схему через `dtBranch`.
