# Repository agent instructions

<!-- BEGIN:ai-attribution -->
## AI attribution

This policy applies to every AI coding agent, regardless of vendor, model, editor, or interface.

### When to add attribution

Add attribution only when an AI agent actually assisted the commit: it wrote or changed at least one line of code, test, documentation, or configuration that is in the commit.

Do not add any attribution trailer when:

- the agent only answered questions, explained code, reviewed code, or ran commands, and wrote nothing that is in the commit;
- the developer wrote all of the changes by hand.

A commit with no AI assistance carries no AI trailers. Never add them "to be safe".

### Commit trailers

When the commit was assisted, end the commit message with these three trailers in one contiguous block, on consecutive lines:

```text
Co-authored-by: <agent-name> <agent-email>
AI-Assisted: <level>
AI-Tool: <tool-id>
```

- Write each key exactly as shown, including its capitalization: `Co-authored-by`, `AI-Assisted`, `AI-Tool`. Do not write `Co-Authored-By`.
- Replace every placeholder with its actual value. Keep the angle brackets around the email, as git's coauthor format requires.
- Name the agent and tool actually used. Never claim another tool's identity.
- Use the tool's configured coauthor name and email. If a tool has none, use its actual name with `noreply@example.invalid`.
- If your tool adds its own coauthor trailer by default, such as Claude Code's `Co-Authored-By` line, do not add it as well. This block replaces it, so the commit has one coauthor line for each agent.
- `<tool-id>` is one of: `antigravity`, `claude`, `codex`, `copilot`, `cursor`, `gemini-cli`. Another agent uses its own lowercase identifier made of letters, digits, periods, underscores, or hyphens.
- If several agents contributed, add a `Co-authored-by` line for each extra agent before the final three-line block. The final block names the agent that created the commit.

### Choosing the level

Choose `<level>` for this commit only, by looking at the staged diff, not at how the session felt:

- `all`: the agent wrote essentially every changed line, and the developer made no material edits to them afterward.
- `majority`: the agent wrote more than half of the changed lines.
- `minor`: the agent wrote less than half, or the developer retyped or reworked the agent's suggestions.

Use `all` only when you can say the developer did not edit the agent's output. If unsure between two levels, choose the lower one.

Never use `none`. If the agent did not assist, add no trailers.

Do not omit, reorder, or reword the trailer keys.

### Pull requests

When any commit in the pull request carries AI trailers, include this visible line in the description:

```text
AI-Agents: <Agent Name>[, <Agent Name>]
```

List every agent named in a `Co-authored-by` trailer of the pull request's commits, including agents that other people's commits name. Leave the line out when no commit in the pull request was AI-assisted.

### Human commits and merges

These trailers are written by the agent. Engineers are not required to add or maintain them by hand, and no check will block a commit or pull request for missing them. When an engineer chooses to commit agent-authored changes, keeping the agent's trailers is welcome but optional. When squash merging, keeping the trailers from the squashed commits is optional; if kept, set the level for the whole squashed change.
<!-- END:ai-attribution -->
