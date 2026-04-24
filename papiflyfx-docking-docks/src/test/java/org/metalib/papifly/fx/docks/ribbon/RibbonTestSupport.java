package org.metalib.papifly.fx.docks.ribbon;

import org.metalib.papifly.fx.api.ribbon.RibbonCommand;
import org.metalib.papifly.fx.api.ribbon.RibbonButtonSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonContext;
import org.metalib.papifly.fx.api.ribbon.RibbonGroupSpec;
import org.metalib.papifly.fx.api.ribbon.RibbonProvider;
import org.metalib.papifly.fx.api.ribbon.RibbonTabSpec;
import org.metalib.papifly.fx.docks.testutil.FxTestUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class RibbonTestSupport {

    private RibbonTestSupport() {
    }

    static void settleFx() {
        FxTestUtil.waitForFxEvents();
        FxTestUtil.waitForFxEvents();
    }

    static RibbonContext contextWithType(String activeContentTypeKey) {
        return new RibbonContext(null, null, activeContentTypeKey, Map.of());
    }

    static RibbonProvider provider(
        String providerId,
        Function<RibbonContext, List<RibbonTabSpec>> tabsFactory
    ) {
        return provider(providerId, 0, tabsFactory);
    }

    static RibbonProvider provider(
        String providerId,
        int providerOrder,
        Function<RibbonContext, List<RibbonTabSpec>> tabsFactory
    ) {
        return new InMemoryProvider(providerId, providerOrder, tabsFactory);
    }

    static RibbonTabSpec simpleButtonTab(String tabId, String tabLabel, String commandId, String commandLabel) {
        return new RibbonTabSpec(
            tabId,
            tabLabel,
            0,
            false,
            ribbonContext -> true,
            List.of(new RibbonGroupSpec(
                "actions",
                "Actions",
                0,
                0,
                null,
                List.of(new RibbonButtonSpec(RibbonCommand.of(commandId, commandLabel, () -> {
                })))
            ))
        );
    }

    static RibbonCommand command(RibbonManager manager, String commandId) {
        return manager.getCommandRegistry()
            .find(commandId)
            .orElseThrow(() -> new AssertionError("Missing command: " + commandId));
    }

    static List<String> tabIds(RibbonManager manager) {
        return manager.getTabs().stream()
            .map(RibbonTabSpec::id)
            .toList();
    }

    private record InMemoryProvider(
        String providerId,
        int providerOrder,
        Function<RibbonContext, List<RibbonTabSpec>> tabsFactory
    ) implements RibbonProvider {

        @Override
        public String id() {
            return providerId;
        }

        @Override
        public int order() {
            return providerOrder;
        }

        @Override
        public List<RibbonTabSpec> getTabs(RibbonContext context) {
            return tabsFactory.apply(context);
        }
    }
}
