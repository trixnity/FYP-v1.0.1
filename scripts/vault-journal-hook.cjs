#!/usr/bin/env node
/*
 * Claude Code Stop hook: keep the Obsidian dev journal (docs/vault/Changelog) in
 * sync with code changes.
 *
 * On session stop:
 *   - If Claude is already continuing because of this hook (stop_hook_active) -> allow stop.
 *   - If the repo state (HEAD + working-tree status) is unchanged since the last
 *     recorded entry -> allow stop silently (pure Q&A sessions are not journaled).
 *   - Otherwise -> block once and ask Claude to write/append today's entry, then
 *     run this script with --mark to re-baseline.
 *
 * Usage:
 *   node scripts/vault-journal-hook.cjs           (hook mode, reads JSON on stdin)
 *   node scripts/vault-journal-hook.cjs --mark     (record current state as journaled)
 */
'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execSync } = require('child_process');

const REPO = process.env.CLAUDE_PROJECT_DIR || process.cwd();
const MARK_FILE = path.join(REPO, 'docs', 'vault', '.journal-state');

function git(args) {
  try {
    return execSync(`git ${args}`, { cwd: REPO, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    return '';
  }
}

function stateSignature() {
  const head = git('rev-parse HEAD') || 'no-head';
  const status = git('status --porcelain');
  const digest = crypto.createHash('sha1').update(status).digest('hex');
  return `${head}:${digest}`;
}

function readMark() {
  try {
    return fs.readFileSync(MARK_FILE, 'utf8').trim();
  } catch {
    return '';
  }
}

function writeMark() {
  fs.mkdirSync(path.dirname(MARK_FILE), { recursive: true });
  fs.writeFileSync(MARK_FILE, stateSignature() + '\n');
}

// --- --mark mode -----------------------------------------------------------
if (process.argv.includes('--mark')) {
  writeMark();
  console.error('vault-journal-hook: state baseline recorded.');
  process.exit(0);
}

// --- hook mode ------------------------------------------------------------
let input = {};
try {
  input = JSON.parse(fs.readFileSync(0, 'utf8') || '{}');
} catch {
  input = {};
}

// Loop guard: we already asked Claude to journal this turn.
if (input.stop_hook_active) process.exit(0);

const current = stateSignature();
if (current === readMark()) process.exit(0); // nothing changed since last entry

const today = new Date().toISOString().slice(0, 10);
const changed = git('status --porcelain')
  .split('\n')
  .filter(Boolean)
  .map((l) => '  ' + l)
  .join('\n');

const reason = [
  'This session is ending with repo changes that are not yet in the Obsidian dev journal.',
  '',
  'Before stopping, update the journal:',
  `  1. Open or create docs/vault/Changelog/${today}.md`,
  '     (shape: docs/vault/templates/journal-entry.md). If it already exists, append to it.',
  '  2. Fill in what changed THIS session and why: summary, changes, files touched,',
  '     decisions, follow-ups. Be concise; skip sections that do not apply.',
  `  3. Ensure docs/vault/Changelog/_index.md has a bullet linking [[${today}]].`,
  '  4. Run: node scripts/vault-journal-hook.cjs --mark',
  '',
  'Current working-tree changes:',
  changed || '  (none — changes are committed; summarize the new commits)',
  '',
  'If the journal already reflects the current state, run the --mark command and stop.',
].join('\n');

process.stdout.write(JSON.stringify({ decision: 'block', reason }));
process.exit(0);
