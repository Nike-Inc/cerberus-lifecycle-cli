# Cerberus CLI

Project for building and running the CLI for Cerberus.

## User Environment Requirements

1. JDK 1.8, with Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy 

## Development Environment Requirements

1. JDK 1.8, with Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy

2. Python 3
    1. boto3 and troposphere

### Install Python 3 for Mac OS X

Use homebrew to install Python 3.

`$ brew install python3`

Use pip3 to install the requirements.  Run this command from the project root.

`$ pip3 install -r smaas-cf/requirements.txt`

## Developing

If making changes to the project, branch off of master:

`$ git checkout -b feature/feature-name`

To test your changes, use the provided `debug.sh` bash script:

`$ ./debug.sh [--debug] -e [environment] -r [region] <command> [command options]`

This will suspend and await a debugger being attached on port 5005.

## Running

Make sure you are on the master branch:

`$ git checkout master`

Run the tool with the provided bash script:

`$ ./run.sh [--debug] -e [environment] -r [region] <command> [command options]`

## Command Line Arguments

**--debug** - Turns on debug level logging.

**-e \[environment\]** - The Cerberus environment to run the command against.

**-r \[region\]** - The AWS region hosting the Cerberus environment.

## Misc

### Bash aliases for running the Cerberus CLI

Ever dreamed of running the CLI from anywhere on the command line?  Yeah, me either... but here\'s how!

Add to your ~/.profile or whatever gets sourced when you start a terminal session:

```
alias cerberus="/path/to/java-app-cerberus-cli/run.sh"
alias cerberus_debug="/path/to/java-app-cerberus-cli/debug.sh"
```

### Updating or adding new dependencies

To update the dependency lock file `./gradlew generateLock saveLock`
