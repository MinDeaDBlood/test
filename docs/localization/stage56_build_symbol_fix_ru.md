# Stage 56. Исправление ошибок сборки после переименований

## Что было найдено

Сборка дошла до компиляции и показала ошибки в местах, где после переименования классов остались старые обращения.

Проблемные места были такими:

```text
Casters.SignatureStatus
Casters.ApplicabilityStatus
Casters.OfflineInstallationType
PEScratch.ShowDialog
ApplyUnattend.ShowDialog
```

## Что исправлено

Старые обращения к методам `Casters` заменены на реальные методы класса:

```text
SignatureStatus
Applicability
OfflineInstallType
```

Открытие окна настройки scratch space теперь использует реальный класс формы:

```text
SetPEScratchSpace
```

Открытие окна применения unattended файла теперь использует реальный класс формы:

```text
ApplyUnattendFile
```

## Дополнительная чистка

Секция `PEScratch` переименована в более понятную секцию:

```ini
[PE.Scratch]
Window.Title=Set Windows PE scratch space
Header.Title={0}
Description.Message=The scratch space is the amount of writable space available on the Windows PE system volume when its contents are copied to memory. Please specify a scratch space amount and click OK.
Space.Label=Scratch space:
Ok.Button=OK
Cancel.Button=Cancel
```

Переводы в других языках перенесены из существующих языковых файлов. Английские значения не копировались поверх других языков.

## Проверка

Обновлён скрипт:

```text
tools/localization/verify_project_items.py
```

Теперь он дополнительно ловит старые обращения, которые уже ломали сборку, и вызовы `ShowDialog` у пунктов меню.
