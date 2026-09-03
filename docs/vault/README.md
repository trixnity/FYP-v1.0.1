# EduChess Vault

Obsidian vault for the **Smart Chess Education & Trainer System** (FYP v1.0.1).

## How to open

1. Open Obsidian → **Open folder as vault**
2. Select this folder: `docs/vault/`

## What's in here

| Folder | Purpose |
| --- | --- |
| `Changelog/` | Dated dev journal — one note per day, appended automatically at the end of each Claude Code session when there are code changes. |
| `templates/` | Note templates (used by the journal automation). |

## How updates happen

A **Stop hook** (`scripts/vault-journal-hook.cjs`, wired in `.claude/settings.json`) runs
when a Claude Code session ends. If the working tree changed or new commits landed since
the last recorded entry, Claude is prompted to write/append today's entry in
`Changelog/` before the session closes, then stamps `docs/vault/.journal-state`.

Pure question-and-answer sessions with no code changes are skipped.

## Manual entry

You can always add notes by hand. To re-baseline the automation after a manual update:

```bash
node scripts/vault-journal-hook.cjs --mark
```

## Index

- [[Changelog/_index|Changelog index]]
