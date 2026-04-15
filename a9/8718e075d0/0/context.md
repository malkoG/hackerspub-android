# Session Context

## User Prompts

### Prompt 1

For reaction notification, we also need to specify number of reacted actors, and "~~ reacted to your post with <emoji>" Or suggest

### Prompt 2

Yes

### Prompt 3

othersposted. Hey...

### Prompt 4

Base directory for this skill: /Users/kodingwarrior/.claude/plugins/marketplaces/zipsa/minimalism-workflow/skills/commit

# Create Commit

Create a commit with auto-generated message from staged changes.

**Arguments**: `` (optional commit message override)

## Instructions

1. Check for staged changes:
   - Run `git diff --cached --stat` to see staged files summary
   - If no staged changes, inform the user and suggest `git add`

2. Gather information about staged changes:
   - Run `git diff --...

