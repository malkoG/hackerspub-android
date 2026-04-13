# Session Context

## User Prompts

### Prompt 1

❯ Web link not correctly works.
  For example,
  - Clicking on hackers.pub/@<handle>, flashes profile page, but redirects to
  profile
  - Clicking on /notifications, flashes, and fallback to profile
  - Same for /@<Handle>/<postuuid>

### Prompt 2

[Request interrupted by user]

### Prompt 3

Install release

### Prompt 4

[Request interrupted by user for tool use]

### Prompt 5

No, it should be release

### Prompt 6

[Request interrupted by user]

### Prompt 7

Hackers'Pub Keystore generation

 # 2. Generate a keystore (one-time only)
  keytool -genkey -v \
    -keystore hackerspub-release.jks \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -alias hackerspub

  # 1. Build signed APK
  export KEYSTORE_FILE="$(pwd)/hackerspub-release.jks"
  export STORE_PASSWORD="REDACTED"
  export KEY_ALIAS="hackerspub"
  export KEY_PASSWORD="$STORE_PASSWORD"

  ./gradlew assembleRelease
## Base 64 keystore (KEYSTORE_BASE64)


```
MII...

### Prompt 8

Install again

### Prompt 9

How can I trigger weblink?

### Prompt 10

How can I do it in app?

### Prompt 11

With installedrelease, clicking from mobile device didn't trigger

### Prompt 12

What???? how is the docs/

### Prompt 13

Try again this


Hackers'Pub Keystore generation

 # 2. Generate a keystore (one-time only)
  keytool -genkey -v \
    -keystore hackerspub-release.jks \
    -keyalg RSA -keysize 2048 \
    -validity 10000 \
    -alias hackerspub

  # 1. Build signed APK
  export KEYSTORE_FILE="$(pwd)/hackerspub-release.jks"
  export STORE_PASSWORD="REDACTED"
  export KEY_ALIAS="hackerspub"
  export KEY_PASSWORD="$STORE_PASSWORD"

  ./gradlew assembleRelease
## Base 64 keystore (KEYSTORE_...

### Prompt 14

Where has been assetlinks go off? Find the cause

### Prompt 15

[Request interrupted by user for tool use]

### Prompt 16

I mean, in this codebase

### Prompt 17

Could you also check ../hackerspub-android-prod?

### Prompt 18

Where did "AD:C6" came from

### Prompt 19

How it became?

### Prompt 20

But, it worked before the change

### Prompt 21

No, fit to AD:C6

### Prompt 22

See ../hackerspub, This is the source of truth.

### Prompt 23

Don't edit. You can take a look for history.

### Prompt 24

Wait, so, was it a valid change? How about supporting passkey again?

### Prompt 25

So, I just can see the result only if i install?

### Prompt 26

Second

### Prompt 27

Okay, I can only know by install from appstore. right?

