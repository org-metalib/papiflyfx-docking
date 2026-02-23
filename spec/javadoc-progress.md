# Javadoc Warning Fix Progress

## Objective
- Make this command pass without relaxing Javadoc checks:
  - `mvn clean javadoc:jar -Dmaven.javadoc.failOnWarnings=true`
- Keep runtime behavior unchanged.

## Configuration Note
- Reverted the temporary Javadoc plugin relaxation in `pom.xml` (`doclint=all,-missing`).
- Final state uses strict warning checks with no plugin workaround.

## Verification Timeline
1. Re-ran strict Javadoc build after config revert; failures moved through modules (`papiflyfx-docking-code`, then `papiflyfx-docking-samples`).
2. Iteratively fixed missing Javadocs/tags/comments and reran:
   - `mvn -pl papiflyfx-docking-code javadoc:jar -Dmaven.javadoc.failOnWarnings=true`
3. Final full verification:
   - `mvn clean javadoc:jar -Dmaven.javadoc.failOnWarnings=true`
   - Result: `EXIT_CODE:0`

## Change Policy Used
- Documentation-only edits:
  - Added/expanded Javadocs (`@param`, `@return`, method/class/record comments).
  - Added enum constant comments where required.
  - Added explicit no-op constructors (with Javadoc) only where implicit default constructors caused Javadoc warnings.
- No functional behavior changes were introduced.

## Files Updated

### `papiflyfx-docking-api`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/ContentStateAdapter.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/LeafContentData.java`
- `papiflyfx-docking-api/src/main/java/org/metalib/papifly/fx/docking/api/Theme.java`

### `papiflyfx-docking-docks`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/Leaf.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/Split.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/TabGroup.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockElement.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockLeaf.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockSplitGroup.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockTabGroup.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragContext.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/DragManager.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTestResult.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/drag/HitTester.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/floating/FloatingDockWindow.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/floating/FloatingWindowManager.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/ContentStateRegistry.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/BoundsData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/DockSessionData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/FloatingLeafData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/LayoutNode.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/LeafData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/MaximizedLeafData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/MinimizedLeafData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/RestoreHintData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/SplitData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/data/TabGroupData.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/minimize/MinimizedBar.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/minimize/MinimizedStore.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/minimize/RestoreHint.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/render/OverlayCanvas.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/serial/DockSessionPersistence.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/serial/DockSessionSerializer.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/serial/LayoutSerializer.java`

### `papiflyfx-docking-code`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorFactory.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/GoToLineController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/CaretRange.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/EditorCommand.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/KeyBinding.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/KeymapTable.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineBlock.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineEditService.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/MultiCaretModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/WordBoundary.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DeleteEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DocumentChangeEvent.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/InsertEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/LineIndex.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/ReplaceEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/TextSource.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/Marker.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/MarkerModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/MarkerType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/AbstractCStyleLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JavaLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JavaScriptLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JsonLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexResult.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexState.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/Lexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexerRegistry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LineTokens.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/MarkdownLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/PlainTextLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/Token.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenMap.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/GlyphCache.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/WrapMap.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchIcons.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchMatch.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/CaretStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`

### `papiflyfx-docking-samples`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SampleLauncher.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SampleScene.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/SamplesApp.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/catalog/SampleCatalog.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/JavaEditorSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/JavaScriptEditorSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/JsonEditorSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/code/MarkdownEditorSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/BasicSplitSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/FloatingSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/MinimizeSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/NestedSplitSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/PersistSample.java`
- `papiflyfx-docking-samples/src/main/java/org/metalib/papifly/fx/samples/docks/TabGroupSample.java`

## Final Result
- Strict Javadoc build now succeeds end-to-end:
  - `mvn clean javadoc:jar -Dmaven.javadoc.failOnWarnings=true`
