# Session Context

## User Prompts

### Prompt 1

Add bottom padding for each post/article detail enough

### Prompt 2

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

### Prompt 3

Base directory for this skill: /Users/kodingwarrior/.claude/plugins/marketplaces/zipsa/minimalism-workflow/skills/ghpr

# Create Pull Request

Create a pull request with auto-generated title and body from commits.

**Arguments**: `onto upstream main` (optional base branch, defaults to `main`)

## Instructions

1. Parse the base branch:
   - If `onto upstream main` is provided, use it as the base branch
   - Otherwise, default to `main`

2. Gather information about the current branch:
   - Run `g...

### Prompt 4

Also, we need to add url thumbnail attachment, as PostCard

### Prompt 5

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

