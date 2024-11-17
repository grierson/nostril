# Questions

All the questions I ask myself while I work on this project?
(Expect lots of dumb questions)

## 16/11

Should I connect many relay streams into one stream?

Why? Only need to focus on one stream

Questions:

- How would I send "fetch latest" events to each stream?
  - I could REQ to each relay ["REQ" "sub-id"]
    and take latest but might would need to throttle incoming if relay is busy.
    So would need to use "since" and "until"
