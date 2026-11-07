# QuickGitNEXT

A **GitHub Desktop–style git client** for Android (phones, tablets, Chromebooks / Googlebook OS) and **native Linux/Windows/macOS desktop**.

Clone, browse, stage, commit, push/pull, branch, review PRs and issues, run Actions, and resolve conflicts — with multi-account GitHub, GitLab, and Gerrit support. Git runs **locally on-device** via [JGit](https://www.eclipse.org/jgit/); network traffic goes only to the remotes and hosts you configure.

Built with **Kotlin**, **Jetpack Compose** (Android), and **Compose Multiplatform** (desktop).

---

## Platforms

| Platform | Notes |
|----------|--------|
| **Android phones** | Full client; adaptive UI (compact layout under ~6.3″, floating nav and denser chrome on larger phones) |
| **Android tablets** | Wide repo-detail chrome, bottom tab bar, Git Actions dialog, pull-to-refresh |
| **Chromebook / Googlebook OS** | Android app; Chromebook-oriented top chrome; works on Intel and other devices (verify large repos / LFS on target hardware) |
| **Wear OS** | Companion Wear module in the same project |
| **Linux desktop** | Native Compose Multiplatform app (`.deb` / `.rpm` / AppImage) |
| **Windows / macOS** | Desktop packaging targets via Compose Multiplatform |

---

## Features

### Repositories & local git
- **Repo list** — local clones with branch and dirty-state indicators; import a folder; create a local repo; pull-to-refresh
- **Clone** — HTTPS (PAT) or SSH (imported private key), live progress and notifications; clone from connected accounts or paste a URL; **Browse my repos** for GitHub/GitLab/Gerrit
- **Changes / staging** — staged, unstaged, untracked; stage / unstage / discard (per file or bulk); commit message, amend, sign-off
- **Push / Pull / Fetch** — including rebase variants, LFS-aware options, force-with-lease / force; auth failures route to Credentials
- **Diff viewer** — colorized unified diff for working tree, staged, or historical commits
- **History** — commit log with author, SHA, date; browse tree at commit; cherry-pick / revert / reset flows where supported
- **Branches & remotes** — local and remote branches; create, checkout, delete; manage remotes
- **Merge conflicts** — after a failed pull; keep ours / theirs / hand-edit; commit or abort merge
- **Files browser** — walk the working tree; up-directory and system **back gesture** to go up; open files in the editor
- **Code editor** — monospace editing, line numbers, syntax highlighting; **Find**; **Undo / Redo**
- **Git LFS** — pointer detection, fetch/pull/push helpers, track/untrack patterns (HTTPS-oriented)

### Hosted git & collaboration
- **Browse remote repos** — GitHub / GitLab (and Gerrit where connected)
- **Issues** — list, open, create (GitHub / GitLab)
- **Pull / merge requests** — list, detail, create (GitHub / GitLab)
- **Actions / CI** — GitHub Actions and GitLab pipelines; run detail, job logs, live watching; **Releases** merged into Actions on tablet/wide layouts
- **Notifications** — workflow finish and clone progress; clone notification clears when done
- **In-app update checks** — from the **QuickGitNEXT** GitHub Releases feed

### Accounts, profile & credentials
- **Multi-account** — multiple **GitHub**, **GitLab**, and **Gerrit** accounts side by side; switch active account per provider
- **Profile** — avatar and bio; public repos with clone/fork; provider switcher; **followers / following** (GitHub) with open-user → their repos; pull-to-refresh; credentials entry point on large phones
- **User search** — find people (large phones: from Profile FAB; tablets may expose Users in nav)
- **Credentials manager** — encrypted-at-rest tokens, SSH key + passphrase, optional OpenPGP key for commit signing; import account / CSV
- **Credentials lock** — biometrics / device credential on phones & tablets when available; in-app password on Chromebook-style devices without biometrics; desktop password gate for Credentials only
- **GPG signing** — sign commits with an imported armored secret key; toggle in Settings
- **Commit identity** — author name and email in Settings

### Adaptive UI & polish
- **Material 3** expressive surfaces, floating bars, and circular icon chips
- **Floating bottom navigation** on tablets and phones ≥ ~6.3″ (Repos, Clone, Profile, Settings, etc.; Browse/Users/Logs/Creds compacted or moved on large phones)
- **Tablet repo detail** — bottom tab strip (Changes, Files, Branches, Issues, PRs, Actions, History); Stage all / Unstage all / Discard / **Git Actions** dialog; no app bottom nav on that screen
- **Large-phone repo detail** — floating page nav, git ops, and commit section; custom **QuickGitNEXT** loading animation
- **Chromebook chrome** — top floating menu and profile affordances; credentials reachable from Profile
- **Snappy tab transitions** on the floating nav
- **Debug logs** UI (build-flagged), also reachable from Settings on large phones

### Desktop (native)
- Full standalone client (not Android-in-a-window): repo list, clone, staging, commit, push/pull/fetch, history, branches, encrypted credentials, settings
- Left **NavigationRail** for primary destinations
- Auto-update check against GitHub Releases (prefers `.deb` or AppImage as appropriate)
- Data under `~/QuickGitNEXT` and `~/.config/quickgitnext/` (independent from the Android app)

---

## Credits

QuickGitNEXT was **vibecoded** with:

- **[Claude AI](https://www.anthropic.com/claude)** (Anthropic)
- **[Grok AI](https://grok.x.ai/)** (xAI)

Human direction, product decisions, testing, and shipping remain with the project maintainers. No AI vendor is required at runtime — the app is a normal offline-capable git client.

---

## Build (Android)

Requirements: **JDK 17+**, Android SDK, project Gradle wrapper.

```bash
# From the project root
./gradlew :app:assembleStoreRelease
# Debug / logging variant also available, e.g.:
./gradlew :app:assembleLoggingRelease
```

Sync in Android Studio or use the wrapper on the CLI. Target **API 26+** (phones, tablets, Chromebooks, Googlebook OS).

GitHub Actions can sign and publish release APKs; see `.github/workflows/release.yml`.

---

## Linux / Desktop

```bash
./gradlew :desktop:run

./gradlew :desktop:packageDeb
./gradlew :desktop:packageRpm
./gradlew :desktop:packageAppImage
```

Artifacts under `desktop/build/compose/binaries/main/`.

| Item | Path |
|------|------|
| Repos root | `~/QuickGitNEXT` (configurable) |
| Credentials | `~/.config/quickgitnext/credentials.enc` |
| Preferences | `~/.config/quickgitnext/prefs.properties` |

---

## Permissions (Android)

`INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` (progress), and (on older APIs) external storage so clones can live under **Documents/QuickGitNEXT**. Newer Android prefers that public folder when writable and falls back to app-specific storage. Uninstall does **not** delete `Documents/QuickGitNEXT` when the public location is used.

---

## Known limitations

- **SSH host key verification is relaxed** in the current SSH support — fine for personal use; harden with a known_hosts store before treating this as production-grade.
- **Single SSH identity** app-wide (not per-host).
- **Diff is read-only** — whole-file stage/unstage, not per-hunk.
- **Conflict UI** is ours / theirs / manual edit, not a full 3-way merge editor.
- **LFS** focuses on common HTTPS pointer download/smudge and helper ops — not the full `git-lfs` CLI surface.
- Googlebook / ARC translation layers may need validation for very large repos and heavy LFS workloads.

---

## Tested on

- Samsung Galaxy S25 Ultra, M55, A52s, A34, A25, A20e, A13, A12 Nacho  
- Google Pixel 10, 10a, 9a  
- Amazon Fire 10 (13th gen)  
- Samsung Chromebook 4 (Intel)  

---

## License & contributing

See repository license and contribution guidelines if present. Feature requests and bug reports are welcome via GitHub Issues on the QuickGitNEXT project.
