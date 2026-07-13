# Stage 51. Строгая очистка длинных ключей

В этом проходе убраны ключи и секции, которые всё ещё выглядели как механический перенос старого кода.

Примеры нового вида:

```ini
[AppxProvision.MultiSelect]
Selection.Label=Multiple selection
CommonProperties.Label=View the common properties of all selected applications

[AppxProvision.Scan]
AlreadyAdded.PropertiesMatch.Message=...
AlreadyAdded.DifferentPublisher.Message=...
AlreadyAdded.NewerVersion.Message=...

[WimScriptEditor.Actions]
ConfigList.Title=...
ConfigList.FileTitle=...

[PEHelper.WDSImageGroup]
CreateFailed.Message=...
LoadFailed.Message=...
```

Валидатор теперь проверяет не только наличие ключей, но и длину секций, длину ключей, длину полного пути и остатки длинных старых имён.
