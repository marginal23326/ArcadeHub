# AI Regression Suite

```sh
./gradlew test --tests "com.example.arcadehub.games.neonsnake.regression.ScenarioRegressionTest"
```

Pick a different search depth (default is 6):

```sh
./gradlew test --tests "*.ScenarioRegressionTest" -Dsnake.regressionDepth=10
```

A failure prints every mismatched scenario with the expected vs. actual move. Reports land in
`app/build/reports/tests/test/index.html`.

## Adding a scenario

Drop a new `*.json` file into `scenarios/` — same shape as the rest (`board`, `you_id`,
`expectation`).