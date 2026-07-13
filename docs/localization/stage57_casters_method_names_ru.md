# Stage 57. Короткие имена методов Casters

В этом этапе укорочены имена вспомогательных методов `Casters`, которые стали слишком длинными после предыдущего исправления сборки.

Было:

```vb
Casters.CastDismSignatureStatus(...)
Casters.CastDismApplicabilityStatus(...)
Casters.CastDismFullyOfflineInstallationType(...)
```

Стало:

```vb
Casters.SignatureStatus(...)
Casters.Applicability(...)
Casters.OfflineInstallType(...)
```

Причина изменения простая: класс уже называется `Casters`, а типы аргументов уже DISM типы. Повторять `CastDism` в каждом имени не нужно. Новые имена короче, читаются легче и не теряют смысл.

Проверка проектных файлов теперь запрещает старые длинные вызовы, чтобы они не вернулись.
