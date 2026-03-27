# Session Context

## User Prompts

### Prompt 1

Could you change compose button's theme color? also for compose screen please

### Prompt 2

Could you display it with html?

### Prompt 3

Bring me visual companion

### Prompt 4

# Create Commit

Create a commit with auto-generated message from staged changes.

**Arguments**: `` (optional commit message override)

## Instructions

1. Check for staged changes:
   - Run `git diff --cached --stat` to see staged files summary
   - If no staged changes, inform the user and suggest `git add`

2. Gather information about staged changes:
   - Run `git diff --cached` to see the actual diff
   - Run `git status` to see overall status

3. Generate commit message:
   - If `` is prov...

