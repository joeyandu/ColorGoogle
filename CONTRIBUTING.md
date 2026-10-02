# Contributing

Start with [README](README.md), [architecture](docs/ARCHITECTURE.md) and
[build instructions](SIGNING.md). Keep changes focused; preserve upstream attribution
and GPL/third-party notices. Describe trigger, expected/actual behavior, tradeoffs and
validation in a pull request. Do not claim support for an untested phone.

Run the relevant unit tests, release sanity checks, build and lint. Record existing
baselines separately from new findings. Never use a user's production device for
unrequested package removal, data clearing or system-wide setting changes.

For bugs include app version, model, Android/ColorOS build, which trigger fails,
agent/wake state, whether Wi-Fi/wireless debugging/pairing/service are available,
and reproducible steps. Distinguish permission grants from agent and log-session health.
Only share minimal relevant logs after removing serials, account data, network
identifiers, pairing codes, keys, tokens and unrelated screen content.

English or Chinese reports are welcome. Keep both README languages in sync.
