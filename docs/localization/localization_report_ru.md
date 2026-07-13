# Итоговый отчёт по локализации DISMTools

## Что проверено

Проверены основные языковые файлы приложения в корневой папке:

```text
language
```

Проверены файлы:

```text
en-US.ini
es-ES.ini
fr-FR.ini
it-IT.ini
pt-PT.ini
```

Проверены реальные вызовы локализации в VB коде, структура INI файлов, синхронность секций и ключей между языками, наличие используемых ключей, отсутствие неиспользуемых ключей, плейсхолдеры форматирования, фильтры диалогов выбора файлов и запрещённые остатки старого стиля.

## Итоговая структура

Основная локализация лежит в корневой папке проекта:

```text
language
```

Старая корневая папка `Localization` для основных языковых файлов не используется.

Секции используют смысловой путь:

```ini
[Options.Logs]
[Options.Logs.Clear]
[Main.News]
[Main.News.Load]
[Main.News.Error]
[Designer.NewsFeedCard]
```

Ключи внутри секций не повторяют контекст секции:

```ini
[Options.Logs]
Location.Label=Logs location
Clear.Button=Clear logs
OpenFolder.Button=Open logs folder
```

## Что дополнительно очищено

Убраны старые технические остатки в основных языковых файлах и вызовах кода:

```text
сгенерированные Line ключи
Placeholder ключи
нумерованные Message ключи
ключи с номерами контролов
Variant и Alternate ключи
секции с именами обработчиков
секции со склеенными путями старых файлов и классов
```

Секции со склеенными старыми путями заменены на понятные смысловые секции, например:

```ini
[StarterScript.Messages]
[DynaViewer.Messages]
[ThemeDesigner.Messages]
[ISOFiles.PECustomizer.Messages]
[ImageOperations.Mount.Messages]
[Updater.Main.Messages]
[PEHelper.ServerPort.Messages]
```

Дизайнерские динамические значения больше не хранят строки с именами контролов. Для них используются пустые значения с понятными ключами, например:

```ini
[Designer.GetImgInfoDlg]
DynamicValue.Label=

[Designer.Main]
ProjectName.DynamicLabel=
Clock.DynamicLabel=
```

## Правила для языков

Секции и ключи одинаковые во всех языковых файлах.

Значения остаются на языке конкретного файла. Английская локализация не копируется поверх испанской, французской, итальянской или португальской.

Технические названия вроде `Windows 10`, `DISM`, `ISO`, `ESD`, `WIM`, `FFU`, `AppX`, `WinPE`, `ARM64`, `AMD64` остаются оригинальными там, где так понятнее и правильнее по смыслу.


## Аудит переводов

Дополнительно создан отчёт строк, которые в неанглийских файлах совпадают с английским значением и не похожи на чисто технические термины:

```text
docs/localization/localization_same_as_english_review.csv
```

Этот отчёт не считается ошибкой структуры локализации. Он нужен для отдельного переводческого прохода, потому что часть таких строк могла быть оставлена на английском ещё в исходных языковых файлах, а часть может быть техническим текстом, который лучше не переводить автоматически.

## Проверка

Актуальный валидатор:

```text
tools/localization/validate_localization.py
```

Актуальный индекс ключей:

```text
docs/localization/localization_key_index.csv
```

Актуальный отчёт проверки:

```text
docs/localization/localization_validation_report.csv
```

Глубокий аудит прямых UI строк:

```text
docs/localization/localization_deep_audit_remaining.csv
```

Результат глубокого аудита: прямых UI строк вне локализации не найдено.

Результат проверки:

```text
Language files checked: 5
Localization keys in language files: 5353
Localization keys used by source code: 5353
Localization validation passed.
```

## Ограничение

Компиляционная проверка через Visual Studio или MSBuild должна быть выполнена отдельно в Windows окружении. В текущем контейнере нет `msbuild`, `dotnet`, `mono` или `xbuild`.
