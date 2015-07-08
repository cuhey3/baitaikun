package com.mycode.baitaikun.sources.computable;

import com.mycode.baitaikun.sources.Source;
import static java.lang.String.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

public abstract class ComputableSource extends Source {

    @Getter
    public String computeEndpoint;
    public String computeImplEndpoint;
    @Getter
    private final Set<Class> superiorSourceClasses = new HashSet<>();
    @Getter
    private final Set<Source> superiorSources = new HashSet<>();
    @Getter
    @Setter
    public boolean computingNow = false;

    @Override
    public void buildEndpoint() throws Exception {
        super.buildEndpoint();
        computeEndpoint = format("direct:%s.compute", sourceKind);
        computeImplEndpoint = format("direct:%s.computeImpl", sourceKind);
    }

    @Override
    public void configure() throws Exception {
        from(initEndpoint)
                .bean(this, "injectSuperiorSources()")
                .to(initImplEndpoint)
                .bean(this, "ready()");

        from(computeEndpoint)
                .to(computeImplEndpoint)
                .process((exchange) -> computingNow = false)
                .to("direct:broker.poll");
    }

    public void injectSuperiorSources() {
        superiorSourceClasses.stream()
                .forEach(clazz
                        -> superiorSources.add((Source) factory.getBean(clazz)));
    }

    @Override
    public boolean isUpToDate() {
        long parentUpdateTime = 0L;
        for (Source source : superiorSources) {
            parentUpdateTime = Math.max(parentUpdateTime, source.getUpdateTime());
        }
        return checkForUpdateTime >= parentUpdateTime;
    }

    public abstract Object compute();
}
