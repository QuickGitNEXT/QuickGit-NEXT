/**
 * Top-level entry so the Linux X11 WM_CLASS is "QuickGitNEXT"
 * instead of "com-quickgit-desktop-MainKt".
 * GNOME/Ubuntu match the dock icon via StartupWMClass against this value.
 */
object QuickGitNEXT {
    @JvmStatic
    fun main(args: Array<String>) {
        com.quickgit.desktop.bootstrap(args)
    }
}
