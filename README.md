# QuickGitNEXT

A **GitHub Desktop–style git client** for Android (phones, tablets, Chromebooks / Googlebook OS) and **native Linux/Windows/macOS desktop**.

Clone, browse, stage, commit, push/pull, branch, review PRs and issues, run Actions, manage **Releases**, and resolve conflicts — with multi-account GitHub, GitLab, and Gerrit support. Git runs **locally on-device** via [JGit](https://www.eclipse.org/jgit/); network traffic goes only to the remotes and hosts you configure.

Built with **Kotlin**, **Jetpack Compose** (Android), and **Compose Multiplatform** (desktop).

---

## Platforms

| Platform | Notes |
|----------|--------|
| **Android phones** | Full client; **Classic** vs **NEXT** navigation modes (Settings → Layout), or **Auto** by ~6.3″ screen size |
| **Android tablets** | Wide repo-detail chrome, floating bottom nav, optional **Chromebook mode** (Settings → Layout), system-bar fit for multi-window |
| **Chromebook / Googlebook OS** | Dedicated **ChromeOS-style top chrome** (destinations + icon-only section strip + repo tabs); no floating bottom bars |
| **Wear OS** | Companion Wear module in the same project |
| **Linux desktop** | Native Compose Multiplatform app (`.deb` / `.rpm` / AppImage) |
| **Windows / macOS** | Desktop packaging targets via Compose Multiplatform |

---

## Features

### Repositories & local git
- **Repo list** — local clones with branch and dirty-state indicators; import a folder; create a local repo; pull-to-refresh
- **Clone** — HTTPS (PAT) or SSH (imported private key), live progress and notifications; clone from connected accounts or paste a URL; **Browse my repos** for GitHub/GitLab/Gerrit
- **Changes / staging** — staged, unstaged, untracked; stage / unstage / discard (per file or bulk); commit message, amend, sign-off; **co-authors** (`Co-authored-by` trailers)
- **Push / Pull / Fetch** — including rebase variants, LFS-aware options, force-with-lease / force; auth failures route to Credentials
- **Diff viewer** — colorized unified diff for working tree, staged, or historical commits
- **History** — commit log with author, SHA, date; browse tree at commit; cherry-pick / revert / reset flows where supported
- **Branches & remotes** — local and remote branches; create, checkout, delete; manage remotes
- **Merge conflicts** — after a failed pull; keep ours / theirs / hand-edit; commit or abort merge
- **Files browser** — walk the working tree; up-directory and system **back gesture** to go up; open files in the editor; pull-to-refresh
- **Code editor** — monospace editing, line numbers, syntax highlighting; **Find**; **Undo / Redo**
- **Git LFS** — pointer detection, fetch/pull/push helpers, track/untrack patterns (HTTPS-oriented)

### Hosted git & collaboration
- **Browse remote repos** — GitHub / GitLab (and Gerrit where connected)
- **Issues** — list, open, create (GitHub / GitLab)
- **Pull / merge requests** — list, detail, create (GitHub / GitLab)
- **Actions / CI** — GitHub Actions and GitLab pipelines; workflow list, run detail, job logs, live watching; **run workflow** dispatch
- **Releases** — list and open GitHub releases; **create / edit** custom releases (title, notes, tag, assets) from the app; first-class section on repo chrome
- **Notifications** — workflow finish and clone progress; clone notification clears when done
- **In-app update checks** — from the **QuickGitNEXT** GitHub Releases feed (Android tracks **V\*A** / legacy **A\*** channels; desktop tracks **V\*D** / legacy desktop tags)

### Accounts, profile & credentials
- **Multi-account** — multiple **GitHub**, **GitLab**, and **Gerrit** accounts side by side; switch active account per provider
- **Profile** — avatar and bio; public repos with clone/fork; provider switcher; **followers / following** (GitHub) with open-user → their repos; pull-to-refresh; credentials entry point on large phones
- **User search** — find people (available from nav / Profile depending on layout)
- **Credentials manager** — encrypted-at-rest tokens, SSH key + passphrase, optional OpenPGP key for commit signing; import account / CSV
- **Credentials lock** — biometrics / device credential on phones & tablets when available; in-app password on Chromebook-style devices without biometrics; desktop password gate for Credentials only
- **GPG signing** — sign commits with an imported armored secret key; toggle in Settings
- **Commit identity** — author name and email in Settings

### Adaptive UI & polish

#### Phone layout modes (Settings → Layout)
- **Auto** — picks style from approximate diagonal (~6.3″ threshold)
- **NEXT** — floating bottom navigation and denser large-phone repo chrome
- **Classic** — original compact phone chrome (in-screen sub-page row, FAB menus where applicable)

**Classic (compact) highlights**
- Repo details top bar: Back · title · **History**
- Sub-page row: Files, Branches, Issues, PRs, Actions, Releases
- Profiles control on **Repos** sits in the **top-left**
- Primary create actions use a **top-right floating +** (or ▶ for run workflow) on Branches, Issues, PRs, Releases, and Actions — Issues shortcut removed from those top-right slots

**NEXT (floating nav) highlights**
- Floating bottom bar for primary destinations
- Repo details top-right **Issues** shortcut
- Sub-pages and Releases on the floating repo bar; git ops / commit controls arranged for large phones

#### Tablets
- Floating bottom navigation for primary destinations
- Repo detail: wide layout with section tabs (Changes, Files, Branches, Issues, PRs, Actions, Releases); Stage / Unstage / Discard / **Git Actions**; **History** from Changes
- Pull-to-refresh on list-style screens
- **Settings → Layout**: **Auto**, **Tablet mode**, or **Chromebook mode**
- Multi-window / status region: tablet windows **fit system bars** so content is not drawn under the system pill

#### Chromebook & tablet Chromebook mode
- **Top ChromeOS-style toolbar**: Repos, Clone, Browse, Search, Profiles, Settings (no brand wordmark in the bar)
- **Repo section strip** under the main menu when a repo is open — **icon-only** Changes / Files / Branches / Issues / PRs / Actions / Releases
- **Repo tabs** for multiple open clones (select / close)
- Soft **logo watermark** at the bottom of pages
- **No floating bottom navigation** under this shell
- **Changes**: Stage all / Unstage all / Discard / Git Actions on the **title line**, left of **History**
- **Files**: current directory centered on the repo/branch line; **split view** — file tree left, **embedded editor** right
- **Workflow run detail**: name + run # centered on the repo/branch line; pause / restart / cancel / refresh on the right; no secondary back/title row
- **Release detail**: name + tag centered on the repo/branch line; **Edit** on the right; no secondary back/title row
- Tablet Chromebook mode uses slightly **denser** chrome than hardware Chromebooks so controls fit mid-size windows

#### Shared polish
- **Material 3** expressive surfaces, floating bars, and circular icon chips
- Snappy tab transitions on the floating nav
- **Debug logs** UI (build-flagged), reachable from Settings when enabled
- Custom **QuickGitNEXT** loading animation on large-phone flows

### Desktop (native)
- Full standalone client (not Android-in-a-window): repo list, clone, staging, commit, push/pull/fetch, history, branches, encrypted credentials, settings
- Left **NavigationRail** for primary destinations
- Auto-update check against GitHub Releases (prefers `.deb` or AppImage as appropriate)
- Data under `~/QuickGitNEXT` and `~/.config/quickgitnext/` (independent from the Android app)

### CI / releases
- Separate GitHub Actions workflows for **Android** and **Desktop**
- Tags: **`V{version}A`** (Android), **`V{version}D`** (Desktop) — e.g. `V2.7.0A`, `V2.7.0D`
- Release titles: **`QuickGitNEXT {version} Android`** / **`QuickGitNEXT {version} Desktop`**
- Changelogs scoped to `app/` + `wear/` (Android) or `desktop/` (Desktop); previous-tag discovery still understands legacy `A*` / `D*` / `v*` tags
- AppImage packaging installs **desktop-file-utils** (`desktop-file-validate`) for reliable fallback packaging

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

GitHub Actions can sign and publish release APKs; see `.github/workflows/release-android.yml`.

---

## Linux / Desktop

```bash
./gradlew :desktop:run

./gradlew :desktop:packageDeb
./gradlew :desktop:packageRpm
./gradlew :desktop:packageAppImage
```

Artifacts under `desktop/build/compose/binaries/main/`.

Desktop releases: `.github/workflows/release-desktop.yml`.

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
