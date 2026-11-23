#!/usr/bin/env python3
from pathlib import Path

ROOT = Path("tmoe")

# Start verified Telegram 13.3.1 R8 hooks before ordinary TMoe hooks.
p = ROOT / "app/src/main/java/cc/ioctl/tmoe/base/MainStartInit.java"
s = p.read_text()
if "Telegram1331CompatHooks" not in s:
    s = s.replace(
        "import cc.ioctl.tmoe.hook.func.DatabaseCorruptionWarning;\n",
        "import cc.ioctl.tmoe.hook.func.DatabaseCorruptionWarning;\n"
        "import cc.ioctl.tmoe.hook.core.Telegram1331CompatHooks;\n"
    )
    s = s.replace(
        "            // Always load functional/core hooks even if the old fragment RTTI layer failed.\n"
        "            DynamicHookInit.loadHooks();",
        "            // Install the Telegram 13.3.1 compatibility layer first.\n"
        "            Telegram1331CompatHooks.INSTANCE.initialize();\n"
        "            DynamicHookInit.loadHooks();"
    )
p.write_text(s)

# Keep the compatibility layer live when switches are changed at runtime.
p = ROOT / "app/src/main/java/cc/ioctl/tmoe/ui/dsl/item/FunctionSwitch.kt"
s = p.read_text()
if "Telegram1331CompatHooks" not in s:
    s = s.replace(
        "import cc.ioctl.tmoe.hook.base.DynamicHook\n",
        "import cc.ioctl.tmoe.hook.base.DynamicHook\n"
        "import cc.ioctl.tmoe.hook.core.Telegram1331CompatHooks\n"
    )
    s = s.replace(
        "            hook.isEnabledByUser = checked\n"
        "            if (checked) {",
        "            hook.isEnabledByUser = checked\n"
        "            Telegram1331CompatHooks.onPreferenceChanged(hook.javaClass.simpleName, checked)\n"
        "            if (checked) {"
    )
p.write_text(s)

# Restore switches from the actual ExTelegram 12.7.1 fork.
p = ROOT / "app/src/main/java/cc/ioctl/tmoe/fragment/SettingsFragment.kt"
s = p.read_text()
if 'FakePremium, "FakePremium"' not in s:
    marker = '''            functionSwitch(
                EnableDebugMode.INSTANCE, "EnableDebugMode", R.string.EnableDebugMode
            )
'''
    extra = marker + '''            functionSwitch(FakePremium, "FakePremium", R.string.FakePremium)
            functionSwitch(GhostMode, "GhostMode", R.string.GhostMode)
            functionSwitch(BoostSpeed, "BoostSpeed", R.string.BoostSpeed)
            functionSwitch(AlwaysShowDownloads, "AlwaysShowDownloads", R.string.AlwaysShowDownloads)
            functionSwitch(HideStories, "HideStories", R.string.HideStories)
            functionSwitch(HideSeenStories, "HideSeenStories", R.string.HideSeenStories)
'''
    if marker not in s:
        raise SystemExit("SettingsFragment basic marker not found")
    s = s.replace(marker, extra, 1)

if 'AntiRecall, "AntiRecall"' not in s:
    marker = '        category("LostMsgMitigation", R.string.LostMsgMitigation) {\n'
    extra = marker + '''            functionSwitch(
                AntiRecall, "AntiRecall", R.string.AntiRecall,
                "AntiRecallDesc", R.string.AntiRecallDesc
            )
'''
    if marker not in s:
        raise SystemExit("SettingsFragment lost-message marker not found")
    s = s.replace(marker, extra, 1)
p.write_text(s)

# Additional resources. TMoe keeps the host locale and these strings follow it.
values = ROOT / "app/src/main/res/values/strings_compat.xml"
values.write_text("""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="FakePremium">Local Premium</string>
    <string name="GhostMode">Ghost mode</string>
    <string name="BoostSpeed">Faster transfers</string>
    <string name="AlwaysShowDownloads">Always show downloads</string>
    <string name="HideStories">Hide stories</string>
    <string name="HideSeenStories">Do not mark stories as viewed</string>
    <string name="AntiRecall">Protect deleted messages</string>
    <string name="AntiRecallDesc">Keep received messages when the sender deletes them</string>
</resources>
""")

ru = ROOT / "app/src/main/res/values-ru/strings_compat.xml"
ru.parent.mkdir(parents=True, exist_ok=True)
ru.write_text("""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="FakePremium">Локальный Premium</string>
    <string name="GhostMode">Режим невидимки</string>
    <string name="BoostSpeed">Ускорение загрузок</string>
    <string name="AlwaysShowDownloads">Всегда показывать загрузки</string>
    <string name="HideStories">Скрыть истории</string>
    <string name="HideSeenStories">Не отмечать истории просмотренными</string>
    <string name="AntiRecall">Защита от удаления сообщений</string>
    <string name="AntiRecallDesc">Сохранять полученные сообщения, если отправитель удалил их</string>
</resources>
""")

print("TMoe 13.3.1 modern compatibility patch applied")
