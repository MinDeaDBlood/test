# Stage 54. Исправление проектных ссылок после переименования форм

На этом этапе проверены не только INI файлы и вызовы локализации, но и проектные файлы Visual Basic.

Причина ошибки сборки была в том, что часть реальных файлов уже была переименована, а `DISMTools.vbproj` всё ещё ссылался на старые имена. Из за этого MSBuild пытался связать `.resx` с несуществующими `.vb` файлами.

## Что исправлено

Обновлены `Compile` и `EmbeddedResource` записи в `DISMTools.vbproj`.

Примеры исправлений:

```text
Panels\Img_Ops\Drivers\Driver.Manual.vb
Panels\Img_Ops\Drivers\DriverManualFilePicker.vb

Panels\Img_Ops\Editions\ImageEdition.vb
Panels\Img_Ops\Editions\SetImageEdition.vb

Panels\Img_Ops\Switch\Single.Image.vb
Panels\Img_Ops\Switch\SingleImageIndexError.vb
```

Также исправлен проект `PEHelperMainMenu.vbproj`, где оставалась ссылка на старую форму `SysprepMode`.

## Что дополнительно проверено

Добавлен скрипт:

```text
tools/localization/verify_project_items.py
```

Он проверяет все `.vbproj` файлы и смотрит, что `Compile` и `EmbeddedResource` указывают на существующие файлы. Для `.resx` дополнительно проверяется `DependentUpon`, чтобы ресурс был связан с реальным `.vb` файлом.

## Результат проверки

```text
Project item validation passed. Projects checked: 8. Items checked: 572.
```

Локализация после исправления тоже проверена:

```text
Language files checked: 5
Localization keys in language files: 5336
Localization keys used by source code: 5336
Localization validation passed.
```
