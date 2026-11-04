# Redact PDF

## Web and CLI based tool for redacting PDFs for name-blind recruitment

This tool attempts to remove names, pronouns, email addresses and urls from CVs so that recruiters selecting people for
interview aren't influenced by the candidates gender. It is not perfect, so I'd recommend a manual check of the results.

## Dev

> [!NOTE]
> For optimal developer experience, we recommend using [mise](https://mise.jdx.dev/getting-started.html) for Java version management.

### CLI tool

Compile the project and build a JAR

```
sbt assembly
```

Add incoming CVs to the `incoming_cvs` folder. The filename needs to follow the convention `firstname_lastname.pdf`, using an underscore, hyphen, or plus sign as the separator (e.g. `firstname_lastname.pdf`, `firstname-lastname.pdf`, or `firstname+lastname.pdf`).

Run the script:

```sh
./scripts/run.sh
# Note: Run this command from the project root directory
```

Redacted CVs will be output in the `redacted_cvs` folder

### Web app

#### Run locally

There is a script to run the project locally. This starts the app with flags to set the memory settings required to run multiple CVs through.

Run `./startapp.sh` in project root

#### Deployment

This project is continuously deployed using `riff-raff`.
