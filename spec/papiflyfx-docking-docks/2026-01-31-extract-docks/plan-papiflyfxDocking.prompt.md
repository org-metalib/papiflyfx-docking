## Plan: Extract Docks to New Repo

Define new Maven coordinates and repo layout, then move the docks module with updated POMs and docs.
Start from current `papiflyfx-docks` plus shared parent properties, create a new parent POM for 
the new repo, rename the module, and realign README/spec references. Finish by validating 
versioning/groupId choices and documenting the new structure for consumers.

### Steps 4
1. Decide Maven coordinates and versioning for new repo and module; document in `README.md`.
2. Create new multimodule maven `papiflyfx-docking` repo in github using properties from `pom.xml`; 
   remove non-docks management.
3. Move `papiflyfx-docks/` to `papiflyfx-docking-docks/` and update `pom.xml` parent/artifactId.
4. Relocate `spec/papiflyfx-docks/` to `spec/papiflyfx-docking-docks/` and update README references.
5. GroupId choice: switch to `org.metalib.papifly.docking`.
6. Versioning: continue `0.0.1-SNAPSHOT` for the new repo?
