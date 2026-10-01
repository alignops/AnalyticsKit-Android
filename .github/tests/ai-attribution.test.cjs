const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { test } = require('node:test');

const trailer = (level = 'all') => `Co-authored-by: Codex <noreply@openai.com>\nAI-Assisted: ${level}\nAI-Tool: codex`;
const commit = (message, author = 'Engineer') => ({ sha: 'abc123', commit: { message, author: { name: author } } });

async function check(commits, body = 'AI-Assisted: Codex', count = commits.length, capture = {}) {
  // Execute the actual workflow script with a read-only GitHub API fixture.
  const workflow = readFileSync(join(__dirname, '../workflows/ai-attribution.yml'), 'utf8').replace(/\r\n/g, '\n');
  const script = workflow.split('          script: |\n')[1].split('\n').map(line => line.replace(/^ {12}/, '')).join('\n');
  const failures = [];
  const github = {
    rest: {
      pulls: { get: async () => ({ data: { body, commits: count, head: { sha: 'abc123' } } }), listCommits: 'listCommits' },
      repos: { createCommitStatus: async status => { (capture.statuses ??= []).push(status); return { data: status }; } },
    },
    paginate: async () => commits,
  };
  const core = { setFailed: message => failures.push(message), info: () => {} };
  const context = { repo: { owner: 'test', repo: 'test' }, payload: { pull_request: { number: 1, head: { sha: 'abc123' } } }, serverUrl: 'https://github.com', runId: 123 };
  const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
  await new AsyncFunction('github', 'core', 'context', script)(github, core, context);
  return failures;
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

test('uses a trusted metadata-only workflow and reports a required status on the PR head', async () => {
  const workflow = readFileSync(join(__dirname, '../workflows/ai-attribution.yml'), 'utf8').replace(/\r\n/g, '\n');
  assert.match(workflow, /^  pull_request_target:$/m);
  assert.match(workflow, /^  statuses: write$/m);
  assert.doesNotMatch(workflow, /actions\/checkout/);
  const capture = {};
  assert.deepEqual(await check([commit(trailer())], 'AI-Assisted: Codex', 1, capture), []);
  assert.deepEqual(capture.statuses.map(status => status.state), ['pending', 'success']);
  assert.ok(capture.statuses.every(status => status.sha === 'abc123' && status.context === 'AI attribution policy'));
});

test('reports failed validation on the PR head', async () => {
  const capture = {};
  assert.equal((await check([commit(trailer('none'))], 'AI-Assisted: Codex', 1, capture)).length, 1);
  assert.deepEqual(capture.statuses.map(status => status.state), ['pending', 'failure']);
});
test('requires complete attribution for any tool, including an unfamiliar agent', async () => {
  for (const [name, tool] of [['Claude', 'claude-code'], ['Gemini', 'gemini-cli'], ['GitHub Copilot', 'copilot'], ['Cursor', 'cursor'], ['New Agent', 'new-agent']]) {
    const message = `Change\n\nCo-authored-by: ${name} <agent@example.invalid>\nAI-Assisted: minor\nAI-Tool: ${tool}`;
    assert.deepEqual(await check([commit(message)], `AI-Assisted: ${name}`), []);
    assert.equal((await check([commit(message.replace(/^Co-authored-by:.*\n/m, ''))])).length, 1);
  }
});
for (const [label, message] of [
  ['none level', trailer('none')],
  ['unknown level', trailer('some')],
  ['missing coauthor', 'AI-Assisted: all\nAI-Tool: codex'],
  ['missing assistance level', 'Co-authored-by: Codex <noreply@openai.com>\nAI-Tool: codex'],
  ['missing tool', 'Co-authored-by: Codex <noreply@openai.com>\nAI-Assisted: all'],
  ['reordered trailers', 'AI-Assisted: all\nCo-authored-by: Codex <noreply@openai.com>\nAI-Tool: codex'],
  ['blank line inside block', trailer().replace('\nAI-Assisted', '\n\nAI-Assisted')],
  ['text after block', trailer() + '\nMore text'],
  ['duplicate assistance', 'AI-Assisted: minor\n' + trailer()],
  ['missing coauthor email', trailer().replace(' <noreply@openai.com>', '')],
  ['placeholder tool', trailer().replace('AI-Tool: codex', 'AI-Tool: <tool-id>')],
  ['none tool', trailer().replace('AI-Tool: codex', 'AI-Tool: none')],
  ['duplicate lowercase key', 'ai-assisted: minor\n' + trailer()],
]) {
  test(`rejects ${label}`, async () => {
    assert.equal((await check([commit(message)])).length, 1);
  });
}
test('requires attribution even when a Codex-authored commit has no trailers', async () => {
  assert.equal((await check([commit('Change', 'Codex')])).length, 1);
});
test('requires the PR attribution as its own visible exact line', async () => {
  for (const body of [null, 'Summary', 'AI-Assisted:   ', 'AI-Assisted: none', 'AI-Assisted: <Agent Name>', '<!--\nAI-Assisted: Codex\n-->']) {
    assert.equal((await check([commit(trailer())], body)).length, 1);
  }
  assert.deepEqual(await check([commit(trailer())], 'Summary\r\n\r\nAI-Assisted: Codex\r\n'), []);
});
test('handles CRLF commit messages', async () => {
  assert.deepEqual(await check([commit(trailer().replaceAll('\n', '\r\n'))]), []);
});
test('fails closed when the API returns fewer commits than the PR contains', async () => {
  assert.equal((await check([commit('Human change')], null, 251)).length, 1);
});

test('all agent entry points contain the identical shared policy', () => {
  const root = join(__dirname, '../..');
  const block = content => content.match(/<!-- BEGIN:ai-attribution -->[\s\S]*?<!-- END:ai-attribution -->/)?.[0];
  const canonical = block(readFileSync(join(root, 'AGENTS.md'), 'utf8'));
  assert.ok(canonical);
  for (const entry of ['CLAUDE.md', 'GEMINI.md', '.github/copilot-instructions.md', '.cursor/rules/ai-attribution.mdc']) {
    assert.equal(block(readFileSync(join(root, entry), 'utf8')), canonical, entry);
  }
  assert.match(readFileSync(join(root, '.cursor/rules/ai-attribution.mdc'), 'utf8'), /^alwaysApply: true$/m);
});
