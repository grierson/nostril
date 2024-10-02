# Questions

All the questions I ask myself while I work on this project?
(Expect lots of dumb questions)

- How do I fetch tag events? (`{:tags [["e" "<event-id>"]]}`)

  - How do I fetch that specific event?
    - `["REQ" "sub-id" {#e: [<event-id-1>, <event-id-2>]}]`
  - How do I distinguish the results from that REQ and the initial REQ?
    - [Step-back] When I make the first REQ how many events to I get?
    - [Step-back] When I `take!` from a `stream` does it request another event or are events buffered on the stream?

- `{:tags [["p" "<pereson-id>"]]}`
  - How is a person "p" event different?
