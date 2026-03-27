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

### Prompt 5

For compose screen, I want you to display also visibility control over the keyboard button. I mean, top safe area

### Prompt 6

[Request interrupted by user]

### Prompt 7

Upper keyboard area.

### Prompt 8

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

### Prompt 9

When vertically overflowed, scroll also should follow

### Prompt 10

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

### Prompt 11

For visibility control, we have too much bottom padding

### Prompt 12

Still same.

### Prompt 13

[Request interrupted by user]

### Prompt 14

padding for on keyboard input

### Prompt 15

Still same. Is it because of bottom virtual button's area?

### Prompt 16

Still have extra bottom padding

### Prompt 17

Cool. It works. commit

### Prompt 18

1. For `Post` button, move to bottom area.
2. For visibility control, display icon only along with chevron.
3. For cancel, display only X button. and then compose title should be centered

### Prompt 19

After posting done, force refresh timeline

### Prompt 20

# Create Commit

Create a commit with auto-generated message from staged changes.

**Arguments**: `staged only.` (optional commit message override)

## Instructions

1. Check for staged changes:
   - Run `git diff --cached --stat` to see staged files summary
   - If no staged changes, inform the user and suggest `git add`

2. Gather information about staged changes:
   - Run `git diff --cached` to see the actual diff
   - Run `git status` to see overall status

3. Generate commit message:
   - I...

### Prompt 21

For post card, move posted timestamp to right aligned, and left to that display visivility icon

### Prompt 22

When refresh is done, force scroll to top

### Prompt 23

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

### Prompt 24

Still not scrolled.

### Prompt 25

Still not

### Prompt 26

[Request interrupted by user]

### Prompt 27

Or scroll itself is wrong?

### Prompt 28

Still not

### Prompt 29

Still not scrolled to top

### Prompt 30

[Request interrupted by user]

### Prompt 31

It should be scrolled after refresh

### Prompt 32

Visibility is still not available for PostCard, and for some cards we have right padding bigger

### Prompt 33

Is it right aligned?

### Prompt 34

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

### Prompt 35

For PostCard sometimes for display name, we have image tag. <img />. So we will need separate parser. For rendering this, we just can resize by height. Could you implement parser for such usage, and enhance displaying?

For overflow, you can cutoff by character size

### Prompt 36

Could you also update the profile?

### Prompt 37

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

### Prompt 38

Navigate to PostDetailScreen when click on notification card for post

### Prompt 39

Okay, its about clicking on body

### Prompt 40

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

### Prompt 41

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

### Prompt 42

I said upsteram main

### Prompt 43

Display handle right to displayname for postcard

### Prompt 44

Hmmmmm timestamp/visibility should be aligned to right.

### Prompt 45

I still see right padding

### Prompt 46

Still same

### Prompt 47

handle area is more important prioirty

### Prompt 48

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

### Prompt 49

For overall notation, it should be `{handle}`, not `@{handle}`
It also happens in reply, auto completion

### Prompt 50

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

### Prompt 51

For Timeline view, if we have inReplyTo id, we also should display the replyTarget, and it should be faded view

### Prompt 52

Is there loop bug? If we have such things, how about merge those things?

### Prompt 53

[Request interrupted by user]

### Prompt 54

It crashes

### Prompt 55

It did not still show threaded view

### Prompt 56

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

### Prompt 57

If we don't have image attachment for postcard, and but url exists, we need to show preview of the first occurence url. For this, See ../hackerspub implementation.

It should show, og:head, og:description, thumbnail, meta:fediverse, ...

### Prompt 58

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

### Prompt 59

For sharedPost, we can't know that who shared. Could you see?

### Prompt 60

[Request interrupted by user]

### Prompt 61

I mean for sharea

### Prompt 62

[Request interrupted by user]

### Prompt 63

I mean for sharer

### Prompt 64

Still not

### Prompt 65

Why still not??????

### Prompt 66

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

### Prompt 67

How can we place translation button to another area?
Do you have any idea?

### Prompt 68

Could you show me TUI mockup?

### Prompt 69

Option B would be better

### Prompt 70

Hmmmm...... How about slate, or another accent color

### Prompt 71

Okay, go with it

### Prompt 72

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

### Prompt 73

[Request interrupted by user]

### Prompt 74

For shared, how about displaying it with italic and display green shared icon left to the text?

### Prompt 75

For displaying sharer, replyTarget should be first, and sharer is the after

### Prompt 76

[Request interrupted by user for tool use]

### Prompt 77

For link preview card, we need the border.

### Prompt 78

[Request interrupted by user]

### Prompt 79

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

