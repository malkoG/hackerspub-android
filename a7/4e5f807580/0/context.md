# Session Context

## User Prompts

### Prompt 1

You are helping me publish my Android app to F-Droid.

My app details:
- Package name: [e.g. com.example.myapp]
- Source code repository: [GitHub/GitLab URL]
- App name: [Your App Name]
- License: [e.g. GPL-3.0, MIT, Apache-2.0]
- Current version name: [e.g. 1.0.0]
- Current version code: [e.g. 1]

Please do the following:

1. Generate a complete F-Droid metadata YAML file (metadata/com.example.myapp.yml)
   with the correct structure including: Categories, License, SourceCode,
   IssueTracker, ...

### Prompt 2

<bash-input>git switch -c chore/fdroid-deployment</bash-input>

### Prompt 3

<bash-stdout>Switched to a new branch 'chore/fdroid-deployment'</bash-stdout><bash-stderr></bash-stderr>

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

