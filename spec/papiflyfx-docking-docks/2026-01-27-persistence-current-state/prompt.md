
## Persistence current state

What is the difference in two persistence implementations in DockManager?
• set of methods like save/load
•and set of method like saveSession/loadSession

It looks like the implement the same concept, but differntly.

Can these two set of methods be merged with no functionality lost, backword compatibility is not an issue?

Save all findings and suggestions in spec/papiflyfx-docks/2026-01-27-persistence-current-state/README.md

## Next: 

* Remove all code related to `Layout-only persistence`. Use 
  spec/papiflyfx-docks/2026-01-27-persistence-current-state/README.md as a guide.
* Refactor all test cases and DemoApp applicaiton to use Session persistence.
* Save progress to spec/papiflyfx-docks/2026-01-27-persistence-current-state/
