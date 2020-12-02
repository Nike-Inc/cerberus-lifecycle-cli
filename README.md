![Build](https://github.com/Nike-Inc/cerberus-lifecycle-cli/workflows/Build/badge.svg?branch=master)
[![codecov](https://codecov.io/gh/Nike-Inc/cerberus-lifecycle-cli/branch/master/graph/badge.svg?token=UfewGZnHdt)](https://codecov.io/gh/Nike-Inc/cerberus-lifecycle-cli)

# Cerberus Lifecycle Management CLI

This project is a CLI for managing the lifecycle of a Cerberus environment using CloudFormation, native Vault APIs, and AWS API calls.

This command line tool includes features for:

- Provisioning
- Bootstrapping
- Maintenance
- Recovery

To learn more about Cerberus, please see the [Cerberus website](http://engineering.nike.com/cerberus/).

## User Environment Requirements

JDK 1.8, with Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy 

## Development Environment Requirements

JDK 1.8, with Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy

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
