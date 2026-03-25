# Session Context

## User Prompts

### Prompt 1

Could you also add appbundle to release note

### Prompt 2

[Request interrupted by user for tool use]

### Prompt 3

I mean, from GitHub Actions

### Prompt 4

# Create Pull Request

Create a pull request with auto-generated title and body from commits.

**Arguments**: `onto upstream main` (optional base branch, defaults to `main`)

## Instructions

1. Parse the base branch:
   - If `onto upstream main` is provided, use it as the base branch
   - Otherwise, default to `main`

2. Gather information about the current branch:
   - Run `git branch --show-current` to get the current branch name
   - Run `git log <base>..HEAD --oneline` to see commits not in...

