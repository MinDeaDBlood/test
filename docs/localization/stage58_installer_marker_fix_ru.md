# Stage 58. Исправление проверки языка установщика

## Что было найдено

Проверка `tools/build/VerifyInstallerLocalization.ps1` искала обязательный маркер `NoDetails.Message=` в файле `bin\Debug\language\en-US.ini`.

После предыдущей чистки ключ блока ошибок новостной ленты был назван `Additional.Feed.None.Message`. Такой ключ был технически рабочим, но он не совпадал с утверждённым коротким стандартом и ломал проверку установщика.

## Что исправлено

Ключ блока ошибок новостной ленты возвращён к короткому и понятному виду:

```ini
[Main.News.Error]
NoDetails.Message=No additional news feed error details are available. Try refreshing the news feed.
```

Обновлены все языковые файлы:

```text
en-US.ini
es-ES.ini
fr-FR.ini
it-IT.ini
pt-PT.ini
```

Обновлено обращение в коде:

```vb
LocalizationService.ForSection("Main.News.Error")("NoDetails.Message")
```

Обновлён индекс ключей локализации.

## Почему выбран именно этот ключ

`NoDetails.Message` короче и чище, чем `Additional.Feed.None.Message`.

Секция `[Main.News.Error]` уже даёт контекст ошибки новостной ленты, поэтому внутри ключа не нужно повторять `Feed` или длинное описание причины.

## Проверка

Валидатор локализации проходит.

Проверка проектных файлов проходит.

Глубокий аудит UI строк не нашёл кандидатов.

Повторное использование ключей не найдено.
