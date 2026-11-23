package cc.ioctl.tmoe.hook.func

import cc.ioctl.tmoe.hook.base.CommonDynamicHook

/**
 * Preference holders for functions that were present in the ExTelegram 12.7.1
 * TMoe fork but are absent from clean upstream r243. Telegram 13.3.1 behavior
 * is provided by Telegram1331CompatHooks.
 */
object AlwaysShowDownloads : CommonDynamicHook() { override fun initOnce() = true }
object AntiRecall : CommonDynamicHook() { override fun initOnce() = true }
object BoostSpeed : CommonDynamicHook() { override fun initOnce() = true }
object FakePremium : CommonDynamicHook() { override fun initOnce() = true }
object GhostMode : CommonDynamicHook() { override fun initOnce() = true }
object HideSeenStories : CommonDynamicHook() { override fun initOnce() = true }
object HideStories : CommonDynamicHook() { override fun initOnce() = true }
