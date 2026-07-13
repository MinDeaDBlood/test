# Stage 53. Финальный аудит локализации

Локализация приведена к утверждённому стандарту.

Формат остался INI. Основные языковые файлы находятся в корневой папке `language`.

Секции и ключи одинаковые во всех языках. Значения переводов сохранены из своих языковых файлов.

Основные ограничения имён после очистки:

```text
Максимальная длина полного пути ключа: 59
Максимальная длина ключа внутри секции: 28
Максимальная длина компонента секции или ключа: 18
Ключей длиннее 28 символов: 0
Полных путей длиннее 60 символов: 0
Компонентов длиннее 18 символов: 0
```

Примеры исправленного стиля:

```ini
[Updater.Main]
NewVersion.Label=...

[AddDrivers]
CommitImage.CheckBox=...
Drivers.Required.Message=...

[AddDrivers.Actions]
Packages.None.Message=...

[AddPackage]
Packages.Choose.RadioButton=...

[Designer.ImageFilePicker]
MountList.Prompt.Label=...
```

Проверки пройдены:

```text
Localization validation passed.
Deep UI localization candidates: 0
Unclassified localization candidates: 0
Localization keys with more than one source use: 0
```

Сборка через Visual Studio или MSBuild на этом этапе не запускалась.
