const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

const trailer = (level = 'all') => `Co-authored-by: Codex <noreply@openai.com>\nAI-Assisted: ${level}\nAI-Tool: codex`;
const commit = (message, author = 'Engineer') => ({ id: 'abc123', message, author: { name: author } });
const readWorkflow = () => readFileSync(join(__dirname, '../workflows/ai-attribution.yml'), 'utf8').replace(/\r\n/g, '\n');

// Runs the actual workflow script against a push to main. `prs` is what GitHub returns as the
// pull requests associated with the pushed head commit; by default, one merged PR with `body`.
async function check(commits, body = 'AI-Agents: Codex', prs = [{ number: 7, body, merged_at: '2026-10-05T00:00:00Z' }]) {
  const script = readWorkflow().split('          script: |\n')[1].split('\n').map(line => line.replace(/^ {12}/, '')).join('\n');
  const notes = [];
  const github = { rest: { repos: { listPullRequestsAssociatedWithCommit: async () => (prs instanceof Error ? Promise.reject(prs) : { data: prs }) } } };
  // The audit is silent: findings are info lines, and any warning or failure is a bug.
  const never = kind => () => { throw new Error(`the audit must never ${kind}`); };
  const core = { warning: never('warn'), error: never('report an error'), setFailed: never('fail the job'), info: message => { if (!/^AI attribution is valid/.test(message)) notes.push(message); } };
  const context = { repo: { owner: 'test', repo: 'test' }, payload: { after: 'abc123', commits } };
  const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
  await new AsyncFunction('github', 'core', 'context', script)(github, core, context);
  return notes;
}

for (const level of ['all', 'majority', 'minor']) {
  test(`accepts ${level} with the exact contiguous trailer block`, async () => {
    assert.deepEqual(await check([commit(`Change\n\n${trailer(level)}\n`)]), []);
  });
}
test('allows human commits and mixed authorship without attributing human commits', async () => {
  assert.deepEqual(await check([commit('Human change')], null), []);
  assert.deepEqual(await check([commit('Human change'), commit(trailer())]), []);
  assert.deepEqual(await check([commit('Human change\n\nCo-authored-by: Claude Smith <claude@example.com>', 'Claude Smith')], null), []);
});

test('runs only as a background audit of the default branch, never on pull requests', () => {
  const workflow = readWorkflow();
  assert.match(workflow, /^  push:\n    branches: \[[^\]\n]+\]$/m);
  assert.doesNotMatch(workflow, /pull_request/);
  assert.doesNotMatch(workflow, /statuses:|createCommitStatus|actions\/checkout/);
  assert.match(workflow, /^    continue-on-error: true$/m);
});

test('logs an API failure instead of failing', async () => {
  assert.equal((await check([commit(trailer())], undefined, new Error('boom'))).length, 1);
});

test('skips the PR line for direct pushes with no merged PR', async () => {
  assert.deepEqual(await check([commit(trailer())], undefined, []), []);
  assert.deepEqual(await check([commit(trailer())], undefined, [{ number: 8, body: null, merged_at: null }]), []);
});
test('requires complete attribution for any tool, including an unfamiliar agent', async () => {
  for (const [name, tool] of [['Claude', 'claude-code'], ['Gemini', 'gemini-cli'], ['GitHub Copilot', 'copilot'], ['Cursor', 'cursor'], ['New Agent', 'new-agent']]) {
    const message = `Change\n\nCo-authored-by: ${name} <agent@example.invalid>\nAI-Assisted: minor\nAI-Tool: ${tool}`;
    assert.deepEqual(await check([commit(message)], `AI-Agents: ${name}`), []);
    assert.equal((await check([commit(message.replace(/^Co-authored-by:.*\n/m, ''))])).length, 1);
  }
});
for (const [label, message] of [
  ['reordered trailers', 'AI-Assisted: all\nCo-authored-by: Codex <noreply@openai.com>\nAI-Tool: codex'],
  ['Claude Code key case', 'Change\n\nCo-Authored-By: Claude <claude@anthropic.com>\nAI-Assisted: all\nAI-Tool: claude'],
  ['Claude Code key case and order', 'Change\n\nAI-Assisted: all\nAI-Tool: claude\nCo-Authored-By: Claude <claude@anthropic.com>'],
  ['lowercase keys', 'co-authored-by: Codex <noreply@openai.com>\nai-assisted: Majority\nai-tool: codex'],
  ['other trailers in the block', 'Signed-off-by: Dev <dev@example.com>\n' + trailer()],
  ['blank line inside block', trailer().replace('\nAI-Assisted', '\n\nAI-Assisted')],
  ['Claude Code coauthor in its own paragraph', 'Change\n\nAI-Assisted: all\nAI-Tool: claude\n\nCo-Authored-By: Claude <claude@anthropic.com>'],
  ['several blank lines before the block', 'Change\n\n\nAI-Assisted: all\nAI-Tool: claude\n\n\nCo-authored-by: Claude <claude@anthropic.com>'],
  ['a squash of several AI commits', 'Fix (#12)\n\n* First\n\n' + trailer('all') + '\n\n* Second\n\n' + trailer('minor') + '\n\n---------\n\nCo-authored-by: Claude <claude@anthropic.com>'],
  ['a squash of one AI commit', 'Fix (#12)\n\n* First\n\nAI-Assisted: all\nAI-Tool: claude\n\n---------\n\nCo-authored-by: Claude <claude@anthropic.com>'],
]) {
  test(`accepts ${label}`, async () => {
    assert.deepEqual(await check([commit(message)]), []);
  });
}
for (const [label, message] of [
  ['none level', trailer('none')],
  ['unknown level', trailer('some')],
  ['missing coauthor', 'AI-Assisted: all\nAI-Tool: codex'],
  ['missing assistance level', 'Co-authored-by: Codex <noreply@openai.com>\nAI-Tool: codex'],
  ['missing tool', 'Co-authored-by: Codex <noreply@openai.com>\nAI-Assisted: all'],
  ['text after block', trailer() + '\nMore text'],
  ['text paragraph after block', trailer() + '\n\nMore text'],
  ['text between trailer paragraphs', 'AI-Assisted: all\nAI-Tool: claude\n\nMore text\n\nCo-authored-by: Claude <claude@anthropic.com>'],
  ['duplicate assistance', 'AI-Assisted: minor\n' + trailer()],
  ['missing coauthor email', trailer().replace(' <noreply@openai.com>', '')],
  ['placeholder tool', trailer().replace('AI-Tool: codex', 'AI-Tool: <tool-id>')],
  ['none tool', trailer().replace('AI-Tool: codex', 'AI-Tool: none')],
  ['duplicate lowercase key', 'ai-assisted: minor\n' + trailer()],
  ['uppercase none tool', trailer().replace('AI-Tool: codex', 'AI-Tool: NONE')],
  ['duplicate tool outside the block', 'AI-Tool: codex\n\n' + trailer()],
  ['a squash with an invalid level', 'Fix (#12)\n\n* First\n\n' + trailer('all') + '\n\n* Second\n\n' + trailer('none') + '\n\n---------\n\nCo-authored-by: Claude <claude@anthropic.com>'],
  ['a squash with a level missing its tool', 'Fix (#12)\n\n* First\n\n' + trailer('all') + '\n\n* Second\n\nAI-Assisted: minor'],
  ['a squash with no coauthor', 'Fix (#12)\n\n* First\n\nAI-Assisted: all\nAI-Tool: claude\n\n---------'],
]) {
  test(`rejects ${label}`, async () => {
    assert.equal((await check([commit(message)])).length, 1);
  });
}
test('requires attribution even when a Codex-authored commit has no trailers', async () => {
  assert.equal((await check([commit('Change', 'Codex')])).length, 1);
});
test('requires the PR attribution as its own visible exact line', async () => {
  for (const body of [null, 'Summary', 'AI-Agents:   ', 'AI-Agents: none', 'AI-Agents: <Agent Name>', 'AI-Assisted: <Agent Name>', '<!--\nAI-Agents: Codex\n-->']) {
    assert.equal((await check([commit(trailer())], body)).length, 1);
  }
  assert.deepEqual(await check([commit(trailer())], 'Summary\r\n\r\nAI-Agents: Codex\r\n'), []);
  // The older AI-Assisted key and any key case stay accepted.
  for (const body of ['AI-Agents: Codex, Claude', 'AI-Assisted: Codex', 'AI-assisted: Claude Code']) {
    assert.deepEqual(await check([commit(trailer())], body), [], body);
  }
});
test('handles CRLF commit messages', async () => {
  assert.deepEqual(await check([commit(trailer().replaceAll('\n', '\r\n'))]), []);
});

test('all agent entry points contain the identical shared policy', () => {
  const root = join(__dirname, '../..');
  const block = content => content.match(/<!-- BEGIN:ai-attribution -->[\s\S]*?<!-- END:ai-attribution -->/)?.[0];
  const canonical = block(readFileSync(join(root, 'AGENTS.md'), 'utf8'));
  assert.ok(canonical);
  // Cursor, Copilot's coding agent, and Antigravity read AGENTS.md itself, so they need no copy.
  for (const entry of ['CLAUDE.md', 'GEMINI.md', '.github/copilot-instructions.md']) {
    assert.equal(block(readFileSync(join(root, entry), 'utf8')), canonical, entry);
  }
});
