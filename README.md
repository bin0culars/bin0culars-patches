# 🔭 bin0culars patches

Unofficial patches for apps patched with [Morphe](https://morphe.software), made by bin0culars. Not affiliated with or endorsed by the Morphe project.

## ❓ About

bin0culars patches provides quality-of-life and layout modifications for third-party Android applications. The initial patch set targets Reddit, adding responsive tablet layout improvements designed to work seamlessly alongside Morphe's official Reddit patches.

### How to use these patches

In Morphe Manager, add this repository as a patch source: https://github.com/bin0culars/bin0culars-patches

<a href="https://morphe.software/add-source?github=bin0culars/bin0culars-patches">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a new release.
     
     If you wish for the patches list to be collapsed, then remove the word 'EXPANDED' from the comment tag above.

     If you wish to manually keep this list updated then remove the PATCHES_START and PATCHES_END 
     comment blocks entirely. -->

#### A list of your patches will automatically be shown here after your first patches release is created.

&nbsp;

<!-- PATCHES_END -->

## 📱 Reddit: Tablet grid feed

Displays a multi-column masonry grid (2–3 columns depending on screen width) on tablets and wide screens. Supported feeds include Home, Popular, Latest, News, communities, Following, and custom feeds. Phones and comments remain unchanged.

To toggle:
1. Open Reddit Settings → **bin0culars mods** → Tablet grid feed (this row replaces the "Reddit rules" link).
2. Restart Reddit to apply changes.

- **Tested version:** 2026.45.0
- **Tip:** Pairs well with Morphe's "Start as guest" and "Hide ads" patches.

## 🐛 Issues and requests

For bug reports and feature requests, visit https://github.com/bin0culars/bin0culars-patches/issues.

## 🛠️ Building

Build the patches bundle with:

```bash
./gradlew buildAndroid
```

Building requires a GitHub personal access token with `read:packages` permissions, configured via `gpr.user`/`gpr.key` in `~/.gradle/gradle.properties` or via the `GITHUB_ACTOR`/`GITHUB_TOKEN` environment variables.

The built bundle is output to `patches/build/libs/patches-*.mpp`.

## 📜 License

bin0culars patches are licensed under the [GNU General Public License v3.0](LICENSE), with additional conditions under section 7 — see [NOTICE](NOTICE). Parts are adapted from [Morphe patches](https://github.com/MorpheApp/morphe-patches).
