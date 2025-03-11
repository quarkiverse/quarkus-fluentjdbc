package io.quarkiverse.fluentjdbc.deployment;

import io.quarkiverse.fluentjdbc.runtime.FluentJdbcConfig;
import io.quarkiverse.fluentjdbc.runtime.FluentJdbcRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import jakarta.inject.Singleton;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.ParamSetter;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

public class FluentJdbcProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("fluent-jdbc");
    }

    @BuildStep
    RuntimeInitializedClassBuildItem registerConfigForRuntime() {
        return new RuntimeInitializedClassBuildItem(FluentJdbcConfig.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem.builder(FluentJdbcConfig.class).build();
    }

    @BuildStep
    public AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(ParamSetter.class)
                .addBeanClass(FluentJdbc.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    SyntheticBeanBuildItem registerFluentJdbcProducer(FluentJdbcRecorder recorder) {
        return SyntheticBeanBuildItem
                .configure(FluentJdbc.class)
                .setRuntimeInit()
                .scope(Singleton.class)
                .unremovable()
                .runtimeValue(recorder.createFluentJdbc())
                .done();
    }
}
