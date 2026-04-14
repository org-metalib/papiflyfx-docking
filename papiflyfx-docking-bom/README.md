# papiflyfx-docking-bom

Bill of Materials (BOM) for the PapiflyFX Docking framework. Import this POM to align all framework dependency versions in your application without repeating version declarations.

## Usage

Add the BOM to your project's `<dependencyManagement>` section:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-bom</artifactId>
            <version>0.0.17-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Then declare framework dependencies without version tags:

```xml
<dependencies>
    <dependency>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking-docks</artifactId>
    </dependency>
    <dependency>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking-code</artifactId>
    </dependency>
</dependencies>
```

## Managed Artifacts

| Artifact | Description |
|----------|-------------|
| `papiflyfx-docking-api` | Shared docking API, theme, UI primitives |
| `papiflyfx-docking-docks` | Core docking framework |
| `papiflyfx-docking-settings-api` | Settings and secret-management SPI |
| `papiflyfx-docking-settings` | Settings runtime and persistence |
| `papiflyfx-docking-login-idapi` | Identity-provider SPI |
| `papiflyfx-docking-login-session-api` | Auth session lifecycle SPI |
| `papiflyfx-docking-login` | Login runtime and UI |
| `papiflyfx-docking-code` | Canvas-based code editor |
| `papiflyfx-docking-tree` | Canvas-based virtualized tree |
| `papiflyfx-docking-media` | Media/image/video viewer |
| `papiflyfx-docking-hugo` | Hugo preview content |
| `papiflyfx-docking-github` | GitHub toolbar integration |

## Notes

- The BOM version matches the framework version. Use the same version for both.
- JavaFX and test dependency versions (JUnit, TestFX, Monocle) are **not** managed by this BOM. Manage those in your own parent POM or use the archetype which sets them up automatically.
- The `papiflyfx-docking-samples` module is excluded — it is a demo module not intended for external consumption.
