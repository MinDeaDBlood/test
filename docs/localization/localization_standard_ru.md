# Стандарт локализации DISMTools

## Общий принцип

Локализация хранится в INI файлах.

Секции называют смысловой путь интерфейса или функции.

Ключ внутри секции не повторяет имя секции. Он описывает конкретный элемент, действие, сообщение, подсказку или значение внутри этого контекста.


## Расположение языковых файлов

Основные INI файлы лежат в корневой папке проекта:

```text
language
```

Внутри этой папки должны лежать файлы:

```text
en-US.ini
es-ES.ini
fr-FR.ini
it-IT.ini
pt-PT.ini
```

Корневой папки `Localization` для основных языковых файлов быть не должно. При сборке файлы из `language` копируются в папку `language` рядом с исполняемым файлом программы.

## Формат секций

Хороший формат секций:

```ini
[Options.Logs]
[Options.Logs.Clear]
[Main.News]
[Main.News.Load]
[Main.News.Error]
[Designer.NewsFeedCard]
```

Секция должна помогать быстро понять, к какому месту интерфейса относится строка.

## Формат ключей

Хороший формат ключей:

```ini
[Options.Logs]
Location.Label=Logs location
Location.Tooltip=Shows where log files are stored.
Clear.Button=Clear logs
OpenFolder.Button=Open logs folder

[Options.Logs.Clear]
Title=Clear logs
Message=Do you want to delete all log files?

[Main.News]
Updated.Label=News last updated:
LearnMore.Link=Learn more

[Main.News.Load]
Retry.Button=Retry

[Main.News.Error]
NoDetails.Message=No additional news feed error details are available.
```

Плохой формат ключей:

```ini
Label=...
Button=...
Text=...
Message=...
GeneratedControl.Event.NumberedMessage=...
ControlName.InternalHandler.Message=...
```

Ключ должен быть коротким, но понятным внутри своей секции.

## Использование в коде

Для формы, панели или логического блока создаётся секционный локализатор:

```vb
Dim L = LocalizationService.ForSection("Options.Logs")

LblLogsLocation.Text = L("Location.Label")
BtnClearLogs.Text = L("Clear.Button")
BtnOpenLogsFolder.Text = L("OpenFolder.Button")
```

Если нужен один отдельный вызов, можно использовать короткую форму через секцию:

```vb
SomeLabel.Text = LocalizationService.ForSection("Main.News")("Updated.Label")
```

## Ошибки локализации

Fallback на другой язык не используется.

Если ключ отсутствует, приложение должно показать подробную ошибку локализации. В ошибке должны быть язык, файл языка, секция, ключ, полный путь и пример записи, которую нужно добавить в INI файл.

## Технические названия

Технические названия остаются на английском, если так понятнее и точнее.

Примеры:

```text
Windows 10
DISM
ISO
ESD
WIM
FFU
AppX
WinPE
ARM64
AMD64
```

Такие слова можно использовать в переводах как оригинальные технические термины.

## Правило для всех языков

Во всех языковых файлах должны быть одинаковые секции и одинаковые ключи.

Значения должны быть на языке конкретного файла. Английские значения нельзя автоматически копировать в другие языки вместо перевода.

Исключение составляют технические названия, форматы файлов, команды, пути, имена API, архитектуры и диагностические значения, которые должны оставаться оригинальными по смыслу.


## Запрещённые технические остатки

В ключах и секциях нельзя использовать имена обработчиков, номера контролов и искусственные варианты.

Плохо:

```ini
[Old.HandlerName]
SaveProject.Button.AlternateVariant=...
GeneratedControl.Text=...
OldButtonClick.Message=...
```

Хорошо:

```ini
[Main.Interface]
SaveProject.Button=...

[Designer.AddDrivers]
AddFile.Button=...
AddFolder.Button=...
```

Если одинаковый текст встречается в разных окнах, секции должны быть разными. Внутри конкретной секции ключ можно повторять по смыслу, если это действительно один и тот же элемент этой формы или блока.

## Добавление нового языка

Программа больше не использует фиксированный список языков в окнах первого запуска и настроек.

Список языков строится автоматически из файлов `*.ini`, которые лежат в папке `language` рядом с исполняемым файлом программы.

Чтобы добавить новый язык, нужно:

1. Скопировать существующий INI файл из папки `language`.
2. В секции `[LanguageFileInformation]` указать код и отображаемое имя языка.
3. Перевести значения ключей, не меняя названия секций и ключей.
4. Перезапустить программу, чтобы папка языков была перечитана.

Пример:

```ini
[LanguageFileInformation]
LanguageAuthor=Translator name
LanguageCode=de-DE
LanguageName=Deutsch
```

Технические имена секций и ключей остаются на английском. Меняются только значения справа от знака `=`.

Язык хранится только как строковый `LanguageCode`, например `en-US`, `pt-PT` или `de-DE`. Английский `en-US` является значением по умолчанию.
