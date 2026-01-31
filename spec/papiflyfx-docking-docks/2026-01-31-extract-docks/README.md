# Extracting docks module to a separate repository

## Implementation plan
1. Decide and document Maven coordinates for the new repo and module (groupId/artifactId/version) in this README.
2. Create a new multi-module Maven repo `papiflyfx-docking` using properties from the current root `pom.xml`, but remove non-docks management.
3. Move `papiflyfx-docks/` to `papiflyfx-docking-docks/` in the new repo and update its `pom.xml` parent and `artifactId`.
4. Relocate `spec/papiflyfx-docks/` to `spec/papiflyfx-docking-docks/` and update any README/spec references to the new paths.
5. Apply the chosen groupId (tentative: `org.metalib.papifly.docking`) across parent and module POMs.
6. Keep versioning consistent with the current module unless explicitly reset (tentative: `0.0.1-SNAPSHOT`).
7. Verify the new repo builds with `./mvnw -pl papiflyfx-docking-docks -am test` after the move.

## New Maven Repository
- GroupId: `org.metalib.papifly.docking`
- Version: `0.0.1-SNAPSHOT`
- Git repo:
  - https://github.com/org-metalib/papiflyfx-docking
  - name: papiflyfx-docking
  - ownership: org-metalib
  - visibility: public
  - description: PapiflyFX Docking Framework
  - license: Apache-2.0
  - topics: papiflyfx, docking, java, javafx, maven
  - Initialize with README: Yes
