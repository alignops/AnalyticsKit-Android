# Repository agent instructions

<!-- BEGIN:ai-attribution -->
## AI attribution

This policy applies to every AI coding agent, regardless of vendor, model, editor, or interface.

For every git commit you create containing changes you helped author, end the commit message with these three trailers in one contiguous block, on consecutive lines:

```text
Co-authored-by: <agent-name> <agent-email>
AI-Assisted: <level>
AI-Tool: <tool-id>
```

Replace every placeholder with its actual value. Keep the angle brackets around the email address, as required by git's coauthor format. Identify the agent and tool actually used; never claim another tool's identity.

Use the tool's documented coauthor name and email when available. For Codex, use `Codex <noreply@openai.com>`. If a tool has no documented identity, use its actual name with `noreply@example.invalid`, a reserved non-deliverable address that makes no claim about a real account.

Use a lowercase tool identifier containing only letters, digits, periods, underscores, or hyphens, such as `codex`, `claude-code`, `gemini-cli`, `copilot`, or `cursor`. Other agents must use their own identifier. If multiple agents contributed, credit additional agents with coauthor trailers before the required final three-line block; the final block identifies the agent creating the commit.

Choose <level> for this commit only:

- all: you wrote essentially all changed code and the developer only prompted or reviewed.
- majority: you wrote more than half of the change.
- minor: you wrote less than half, such as a helper, tests, part of a refactor, or suggestions the developer applied.

Never use none as the level for a commit you made.
Do not omit, reorder, or reword the trailer keys.

For every pull request description you write, include this visible line, replacing the placeholder with the actual agent name (or comma-separated names when multiple agents contributed):

```text
AI-Assisted: <Agent Name>
```

Engineers must preserve attribution when committing agent-authored changes themselves. When squash merging, preserve the final three trailers and choose the level for the entire squashed change.
<!-- END:ai-attribution -->
