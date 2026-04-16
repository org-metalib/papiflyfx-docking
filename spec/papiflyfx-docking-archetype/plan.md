# PapiflyFX Application Archetype - Implementation Plan

**Lead**: @ops-engineer | **Reviewer**: @spec-steward  
**Priority**: P2 (Normal) | **Date**: 2026-04-14

---

## Intake Summary

| Field | Value |
|-------|-------|
| **Priority** | P2 - Normal feature work |
| **Lead** | @ops-engineer |
| **Reviewers** | @spec-steward (plan/docs), @qa-engineer (test template), @core-architect (BOM shape) |
| **Impacted Modules** | New: `papiflyfx-docking-bom`, `papiflyfx-docking-archetype`. Modified: root `pom.xml` (module list) |
| **Key Invariants** | Existing build must not break. Version properties stay centralized. Platform profiles must be preserved in generated projects. BOM must not alter existing module dependency resolution. |

---

## Part 1: `papiflyfx-docking-bom` Module

### 1.1 Module Definition

```
papiflyfx-docking-bom/
  pom.xml
```

The BOM is a `<packaging>pom</packaging>` module that lives in the framework repository and is published alongside other artifacts.

### 1.2 BOM `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-bom</artifactId>
    <version>0.0.18-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>papiflyfx-docking-bom</name>
    <description>PapiflyFX Docking Bill of Materials — import to align all framework dependency versions.</description>

    <properties>
        <papiflyfx.version>${project.version}</papiflyfx.version>
        <javafx.version>25.0.2</javafx.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- PapiflyFX framework artifacts -->
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-api</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-docks</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-settings-api</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-settings</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-login-idapi</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-login-session-api</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-login</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-code</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-tree</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-media</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-hugo</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-github</artifactId>
                <version>${papiflyfx.version}</version>
            </dependency>

            <!-- JavaFX version aligned with framework -->
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-controls</artifactId>
                <version>${javafx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-web</artifactId>
                <version>${javafx.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

**Design decisions**:
- The BOM does NOT inherit the framework parent POM. It is a standalone POM with its own `<groupId>`, `<version>`, and `<packaging>pom</packaging>`. This follows the Maven BOM convention (like `spring-boot-dependencies`).
- However, for simplicity in this repository, it CAN be a child module of the root POM using `<parent>` and `${project.version}` - this avoids version drift during development. The BOM then inherits the version but overrides packaging to `pom`.
- The BOM also exports the JavaFX version so consumers can use `${javafx.version}` if they import the BOM's properties (or the BOM manages JavaFX artifacts directly in `dependencyManagement`).

**Recommended approach**: Make the BOM a child module with `<parent>` pointing to the root POM. This keeps the version synchronized during `mvn versions:set` and release plugin operations. The BOM `pom.xml` only needs `<dependencyManagement>` - it inherits no unwanted plugins because it has `<packaging>pom</packaging>`.

### 1.3 Root POM Change

Add `papiflyfx-docking-bom` as the **first** module in the root `<modules>` list:

```xml
<modules>
    <module>papiflyfx-docking-bom</module>    <!-- NEW -->
    <module>papiflyfx-docking-api</module>
    ...existing modules...
</modules>
```

### 1.4 Consumer Usage (in generated app)

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-bom</artifactId>
            <version>${papiflyfx.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Then use without version: -->
<dependencies>
    <dependency>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking-docks</artifactId>
    </dependency>
</dependencies>
```

---

## Part 2: `papiflyfx-docking-archetype` Module

### 2.1 Archetype Module Structure

```
papiflyfx-docking-archetype/
  pom.xml
  src/main/resources/
    META-INF/maven/archetype-metadata.xml
    archetype-resources/
      pom.xml
      .mvn/wrapper/maven-wrapper.properties
      app/
        pom.xml
        src/main/java/__packageDir__/App.java
        src/main/java/__packageDir__/AppLauncher.java
        src/test/java/__packageDir__/AppTest.java
      .github/
        workflows/
          ci.yml
        copilot-instructions.md
      AGENTS.md
      CLAUDE.md
      README.md
      spec/
        agents/
          README.md
```

**Note**: `mvnw` and `mvnw.cmd` wrapper scripts are NOT bundled in the archetype. Instead, the generated `README.md` instructs the developer to run `mvn wrapper:wrapper -Dmaven=3.9.12` after generation. This avoids archetype packaging issues with shell scripts and ensures the latest wrapper version.

### 2.2 Archetype `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking</artifactId>
        <version>0.0.18-SNAPSHOT</version>
    </parent>

    <artifactId>papiflyfx-docking-archetype</artifactId>
    <packaging>maven-archetype</packaging>
    <name>papiflyfx-docking-archetype</name>
    <description>Maven archetype for bootstrapping a PapiflyFX docking application.</description>

    <build>
        <extensions>
            <extension>
                <groupId>org.apache.maven.archetype</groupId>
                <artifactId>archetype-packaging</artifactId>
                <version>3.3.1</version>
            </extension>
        </extensions>
    </build>
</project>
```

### 2.3 `archetype-metadata.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<archetype-descriptor
    xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.1.0
    http://maven.apache.org/xsd/archetype-descriptor-1.1.0.xsd"
    name="papiflyfx-app">

    <requiredProperties>
        <requiredProperty key="papiflyfxVersion">
            <defaultValue>0.0.17-SNAPSHOT</defaultValue>
        </requiredProperty>
        <requiredProperty key="javaVersion">
            <defaultValue>25</defaultValue>
        </requiredProperty>
        <requiredProperty key="javafxVersion">
            <defaultValue>25.0.2</defaultValue>
        </requiredProperty>
        <requiredProperty key="mavenVersion">
            <defaultValue>3.9.12</defaultValue>
        </requiredProperty>
    </requiredProperties>

    <fileSets>
        <fileSet filtered="true" encoding="UTF-8">
            <directory></directory>
            <includes>
                <include>pom.xml</include>
                <include>README.md</include>
                <include>AGENTS.md</include>
                <include>CLAUDE.md</include>
            </includes>
        </fileSet>
        <fileSet filtered="true" encoding="UTF-8">
            <directory>.mvn/wrapper</directory>
            <includes>
                <include>maven-wrapper.properties</include>
            </includes>
        </fileSet>
        <fileSet filtered="true" encoding="UTF-8">
            <directory>.github/workflows</directory>
            <includes>
                <include>ci.yml</include>
            </includes>
        </fileSet>
        <fileSet filtered="true" encoding="UTF-8">
            <directory>.github</directory>
            <includes>
                <include>copilot-instructions.md</include>
            </includes>
        </fileSet>
        <fileSet filtered="true" encoding="UTF-8">
            <directory>spec/agents</directory>
            <includes>
                <include>README.md</include>
            </includes>
        </fileSet>
    </fileSets>

    <modules>
        <module id="app" dir="app" name="app">
            <fileSets>
                <fileSet filtered="true" packaged="true" encoding="UTF-8">
                    <directory>src/main/java</directory>
                    <includes>
                        <include>**/*.java</include>
                    </includes>
                </fileSet>
                <fileSet filtered="true" packaged="true" encoding="UTF-8">
                    <directory>src/test/java</directory>
                    <includes>
                        <include>**/*.java</include>
                    </includes>
                </fileSet>
                <fileSet filtered="true" encoding="UTF-8">
                    <directory></directory>
                    <includes>
                        <include>pom.xml</include>
                    </includes>
                </fileSet>
            </fileSets>
        </module>
    </modules>
</archetype-descriptor>
```

### 2.4 Generated Project Layout

After running `mvn archetype:generate`, the developer gets:

```
my-app/
├── pom.xml                                 # root aggregator
├── .mvn/wrapper/maven-wrapper.properties   # Maven 3.9.12
├── app/
│   ├── pom.xml                             # app module
│   └── src/
│       ├── main/java/com/example/myapp/
│       │   ├── App.java                    # JavaFX Application
│       │   └── AppLauncher.java            # main() trampoline
│       └── test/java/com/example/myapp/
│           └── AppTest.java                # basic smoke test
├── .github/
│   ├── workflows/ci.yml                    # CI workflow
│   └── copilot-instructions.md             # Copilot context
├── AGENTS.md                               # agent team skeleton
├── CLAUDE.md                               # Claude Code instructions
├── README.md                               # setup/build/run guide
└── spec/
    └── agents/
        └── README.md                       # agent operating protocol
```

### 2.5 Generated Root `pom.xml` (Template)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>${groupId}</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>${version}</version>
    <packaging>pom</packaging>
    <name>${artifactId}</name>

    <modules>
        <module>app</module>
    </modules>

    <properties>
        <java.awt.headless>true</java.awt.headless>
        <testfx.headless>true</testfx.headless>
        <testfx.robot>glass</testfx.robot>
        <testfx.platform>Desktop</testfx.platform>
        <monocle.platform>Headless</monocle.platform>
        <prism.order>sw</prism.order>
        <prism.text>t2k</prism.text>

        <maven.version.required>3.9</maven.version.required>
        <javafx.version>${javafxVersion}</javafx.version>
        <javafx.platform>mac</javafx.platform>
        <papiflyfx.version>${papiflyfxVersion}</papiflyfx.version>
        <maven.compiler.release>${javaVersion}</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.jupiter.version>5.10.2</junit.jupiter.version>
        <testfx.version>4.0.18</testfx.version>
        <monocle.version>21.0.2</monocle.version>
        <maven.surefire.plugin.version>3.2.5</maven.surefire.plugin.version>
        <javafx.maven.plugin.version>0.0.8</javafx.maven.plugin.version>
        <os.maven.plugin.version>1.7.1</os.maven.plugin.version>
        <maven.enforcer.plugin.version>3.6.2</maven.enforcer.plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- PapiflyFX BOM import -->
            <dependency>
                <groupId>org.metalib.papifly.docking</groupId>
                <artifactId>papiflyfx-docking-bom</artifactId>
                <version>${papiflyfx.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- JavaFX (with platform classifier) -->
            <dependency>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-controls</artifactId>
                <version>${javafx.version}</version>
                <classifier>${javafx.platform}</classifier>
            </dependency>
            <!-- Test dependencies -->
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter-api</artifactId>
                <version>${junit.jupiter.version}</version>
            </dependency>
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter-engine</artifactId>
                <version>${junit.jupiter.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testfx</groupId>
                <artifactId>testfx-junit5</artifactId>
                <version>${testfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testfx</groupId>
                <artifactId>testfx-core</artifactId>
                <version>${testfx.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testfx</groupId>
                <artifactId>openjfx-monocle</artifactId>
                <version>${monocle.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>${os.maven.plugin.version}</version>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <version>${maven.enforcer.plugin.version}</version>
                <executions>
                    <execution>
                        <id>enforce-maven</id>
                        <goals><goal>enforce</goal></goals>
                        <configuration>
                            <rules>
                                <requireMavenVersion>
                                    <version>${maven.version.required}</version>
                                </requireMavenVersion>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven.surefire.plugin.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.openjfx</groupId>
                    <artifactId>javafx-maven-plugin</artifactId>
                    <version>${javafx.maven.plugin.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <!-- JavaFX platform profiles (mirrors framework) -->
    <profiles>
        <profile>
            <id>javafx-mac-aarch64</id>
            <activation>
                <property>
                    <name>os.detected.classifier</name>
                    <value>osx-aarch_64</value>
                </property>
            </activation>
            <properties>
                <javafx.platform>mac-aarch64</javafx.platform>
            </properties>
        </profile>
        <profile>
            <id>javafx-mac-x86_64</id>
            <activation>
                <property>
                    <name>os.detected.classifier</name>
                    <value>osx-x86_64</value>
                </property>
            </activation>
            <properties>
                <javafx.platform>mac</javafx.platform>
            </properties>
        </profile>
        <profile>
            <id>javafx-windows-x86_64</id>
            <activation>
                <property>
                    <name>os.detected.classifier</name>
                    <value>windows-x86_64</value>
                </property>
            </activation>
            <properties>
                <javafx.platform>win</javafx.platform>
            </properties>
        </profile>
        <profile>
            <id>javafx-linux-x86_64</id>
            <activation>
                <property>
                    <name>os.detected.classifier</name>
                    <value>linux-x86_64</value>
                </property>
            </activation>
            <properties>
                <javafx.platform>linux</javafx.platform>
            </properties>
        </profile>
        <profile>
            <id>javafx-linux-aarch64</id>
            <activation>
                <property>
                    <name>os.detected.classifier</name>
                    <value>linux-aarch_64</value>
                </property>
            </activation>
            <properties>
                <javafx.platform>linux-aarch64</javafx.platform>
            </properties>
        </profile>
    </profiles>
</project>
```

### 2.6 Generated `app/pom.xml` (Template)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${groupId}</groupId>
        <artifactId>${artifactId}</artifactId>
        <version>${version}</version>
    </parent>

    <artifactId>${artifactId}-app</artifactId>
    <name>${artifactId}-app</name>

    <dependencies>
        <!-- PapiflyFX Docking (version from BOM) -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-docks</artifactId>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>openjfx-monocle</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <useModulePath>false</useModulePath>
                    <argLine>
                        --enable-native-access=javafx.graphics
                        --add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
                        --add-exports=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED
                        --add-exports=javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                        --add-opens=javafx.base/com.sun.javafx.logging=ALL-UNNAMED
                    </argLine>
                    <systemPropertyVariables>
                        <testfx.headless>${testfx.headless}</testfx.headless>
                        <testfx.robot>${testfx.robot}</testfx.robot>
                        <testfx.platform>${testfx.platform}</testfx.platform>
                        <monocle.platform>${monocle.platform}</monocle.platform>
                        <prism.order>${prism.order}</prism.order>
                        <prism.text>${prism.text}</prism.text>
                        <java.awt.headless>${java.awt.headless}</java.awt.headless>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.openjfx</groupId>
                <artifactId>javafx-maven-plugin</artifactId>
                <configuration>
                    <mainClass>${package}.AppLauncher</mainClass>
                    <options>
                        <option>--enable-native-access=javafx.graphics</option>
                    </options>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>headless-tests</id>
            <activation>
                <property>
                    <name>testfx.headless</name>
                    <value>true</value>
                </property>
            </activation>
            <properties>
                <testfx.platform>Monocle</testfx.platform>
                <testfx.robot>glass</testfx.robot>
                <monocle.platform>Headless</monocle.platform>
                <prism.order>sw</prism.order>
                <prism.text>t2k</prism.text>
            </properties>
        </profile>
    </profiles>
</project>
```

### 2.7 Generated `App.java`

```java
package ${package};

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.metalib.papifly.fx.docks.DockManager;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        var dockManager = new DockManager();
        var tabGroup = dockManager.createTabGroup();
        dockManager.setRoot(tabGroup);

        var scene = new Scene(dockManager.getMainContainer(), 1024, 768);
        stage.setTitle("${artifactId}");
        stage.setScene(scene);
        stage.show();

        dockManager.setOwnerStage(stage);
    }
}
```

### 2.8 Generated `AppLauncher.java`

```java
package ${package};

/**
 * Plain main() trampoline — required because JavaFX Application classes
 * cannot be launched directly when the module path is not configured.
 */
public class AppLauncher {
    public static void main(String[] args) {
        App.main(args);
    }
}
```

### 2.9 Generated `AppTest.java`

```java
package ${package};

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppTest extends ApplicationTest {

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        new App().start(stage);
    }

    @Test
    void applicationStarts() {
        assertNotNull(stage.getScene());
    }
}
```

---

## Part 3: Generated CI/CD Workflow

### 3.1 `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: checkout repository
        uses: actions/checkout@v4
      - name: install java with maven
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '${javaVersion}'
          java-package: 'jdk+fx'
          cache: 'maven'
      - name: build and test (headless)
        run: |
          ./mvnw -B -U --errors \
            -Dsurefire.useFile=false \
            -Djava.awt.headless=true \
            -Dtestfx.headless=true \
            package
```

Matches the framework's `test.yaml` pattern. A release/publish workflow skeleton is NOT generated by default because most new applications do not publish to Maven Central immediately.

---

## Part 4: Generated Multi-Agent Documentation

### 4.1 `AGENTS.md` (Starter)

A minimal agent team with three roles suitable for a new application:

| Agent | Domain |
|-------|--------|
| `@app-dev` | Application features, UI, docking layout |
| `@ops-engineer` | Build, dependencies, CI/CD, settings |
| `@spec-steward` | Planning, docs, coordination |

### 4.2 `CLAUDE.md` (Starter)

Tailored to the generated project:
- groupId, artifactId, version
- Java/JavaFX versions
- Build commands
- Module structure (root + `app`)
- PapiflyFX dependency note via BOM

### 4.3 `.github/copilot-instructions.md` (Starter)

Abbreviated version covering:
- Project type (multi-module Maven JavaFX)
- Key commands (`./mvnw clean package`, `./mvnw -pl app javafx:run`)
- Headless testing note
- Pointer to CLAUDE.md for full details

### 4.4 `spec/agents/README.md` (Starter)

Minimal operating model skeleton following the framework pattern.

---

## Part 5: Environment Setup Instructions (for Generated App)

### 5.1 Prerequisites

```bash
# 1. Install SDKMAN
curl -s "https://get.sdkman.io" | bash

# 2. Install Java 25 with JavaFX (Zulu FX)
sdk install java 25.0.1.fx-zulu
sdk use java 25.0.1.fx-zulu

# 3. Verify
java -version
# Expected: openjdk version "25.0.1" ... Zulu25.30+17-CA
```

### 5.2 Generate the Project

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.metalib.papifly.docking \
  -DarchetypeArtifactId=papiflyfx-docking-archetype \
  -DarchetypeVersion=0.0.17-SNAPSHOT \
  -DgroupId=com.example \
  -DartifactId=my-papiflyfx-app \
  -Dversion=0.1.0-SNAPSHOT \
  -Dpackage=com.example.myapp \
  -DpapiflyfxVersion=0.0.17-SNAPSHOT \
  -DjavaVersion=25 \
  -DjavafxVersion=25.0.2 \
  -DmavenVersion=3.9.12 \
  -DinteractiveMode=false
```

### 5.3 Post-Generation Setup

```bash
cd my-papiflyfx-app

# Install Maven wrapper
mvn wrapper:wrapper -Dmaven=3.9.12

# Verify build
./mvnw clean package
```

### 5.4 Build, Test, and Run Commands

```bash
# Compile all modules
./mvnw compile

# Full build (compile + test + package)
./mvnw clean package

# Run tests headless (CI mode, default)
./mvnw test

# Run tests with display (interactive)
./mvnw -Dtestfx.headless=false test

# Run the application
./mvnw -pl app javafx:run

# Focused module build
./mvnw -pl app -am clean package
```

---

## Part 6: Validation Strategy

### 6.1 Archetype Validation

1. **Build the archetype**: `./mvnw -pl papiflyfx-docking-archetype -am clean install`
2. **Generate a test project**: Run the `archetype:generate` command from Part 5.2 pointing at the local repository.
3. **Build the generated project**: `cd my-papiflyfx-app && ./mvnw clean package`
4. **Run the generated app**: `./mvnw -pl app javafx:run` — verify a window opens with an empty DockManager tab group.
5. **Run headless tests**: `./mvnw -Dtestfx.headless=true test` — verify `AppTest` passes.
6. **CI simulation**: Run the generated `ci.yml` workflow on a test GitHub repository.

### 6.2 BOM Validation

1. **Build the BOM**: `./mvnw -pl papiflyfx-docking-bom install`
2. **Verify resolution**: The generated app's `./mvnw dependency:tree` should show PapiflyFX artifacts resolved at the BOM version without explicit version declarations in `app/pom.xml`.
3. **Version alignment**: After `mvn versions:set -DnewVersion=X.Y.Z`, both the BOM and archetype should pick up the new version.

### 6.3 Archetype Integration Test

Maven archetypes support integration tests via `archetype:integration-test`. Add a test project under `src/test/resources/projects/basic/` with a `goal.txt` containing `package` and an `archetype.properties` file. This runs automatically during `mvn verify` of the archetype module.

---

## Part 7: Risks, Assumptions, and Follow-ups

### Risks

| Risk | Mitigation |
|------|------------|
| PapiflyFX BOM not yet published to Maven Central | Generated apps that reference SNAPSHOT versions need the framework installed locally. Document this. |
| Velocity template escaping for `${}` in POM files | Maven archetype Velocity uses `${variable}` — literal Maven properties must use `\${...}` or `#set` directives. Requires careful testing. |
| Maven wrapper scripts in archetypes | Shell scripts can have line-ending issues. Recommend post-generation `mvn wrapper:wrapper` instead of bundling. |
| `os-maven-plugin` as build extension | Must be present in generated root POM for platform profiles to work. Verified in template. |

### Assumptions

- The PapiflyFX framework will publish a release to Maven Central (or a shared repository) before external consumers use the archetype.
- The archetype is published alongside the framework with the same version.
- Generated projects use the classpath (not module path) for simplicity, matching `useModulePath=false` in Surefire.

### Follow-up Tasks

1. **Implement `papiflyfx-docking-bom`** — create the module, add to root POM, verify with `./mvnw install`.
2. **Implement `papiflyfx-docking-archetype`** — create the archetype module with all templates.
3. **Test archetype generation** — validate the full generate-build-run-test cycle.
4. **Update root README** — mention the archetype and BOM in the framework README.
5. **Add archetype integration test** — `src/test/resources/projects/basic/` with `goal.txt`.
6. **Consider adding more generated content modules** — e.g., a `code-editor` example tab pre-wired in the starter app.
7. **Document BOM usage in framework README** — show how external apps import the BOM.

---

## Implementation Phases

| Phase | Scope | Estimated Artifacts |
|-------|-------|-------------------|
| **Phase 1** | Create `papiflyfx-docking-bom` module | `papiflyfx-docking-bom/pom.xml`, root `pom.xml` update |
| **Phase 2** | Create `papiflyfx-docking-archetype` module | Archetype POM, metadata, all template files |
| **Phase 3** | End-to-end validation | Integration test, manual generation test, CI test |
| **Phase 4** | Documentation | Root README updates, archetype README |

---

## Handoff Contract

```
Lead Agent: @ops-engineer
Priority: P2
Task Scope: Plan Maven archetype + BOM modules for PapiflyFX application bootstrap
Impacted Modules: New: papiflyfx-docking-bom, papiflyfx-docking-archetype. Modified: root pom.xml
Files Changed: spec/papiflyfx-docking-archetype/research.md, spec/papiflyfx-docking-archetype/plan.md
Key Invariants: Existing build unaffected. Version centralization preserved. Platform profiles preserved.
Validation Performed: Research phase only — plan review pending
Open Risks / Follow-ups: BOM not yet published. Velocity escaping needs testing. Wrapper script bundling deferred.
Required Reviewer: @spec-steward (plan/docs), @core-architect (BOM shape), @qa-engineer (test template)
```