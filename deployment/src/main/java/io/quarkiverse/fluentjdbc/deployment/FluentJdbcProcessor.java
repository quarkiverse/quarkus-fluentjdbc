package io.quarkiverse.fluentjdbc.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import jakarta.inject.Singleton;

import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.ParamSetter;

import io.quarkiverse.fluentjdbc.runtime.FluentJdbcRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class FluentJdbcProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("fluent-jdbc");
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return ReflectiveClassBuildItem.builder(
                "io.quarkiverse.fluentjdbc.runtime.FluentJdbcConfig",
                "io.quarkiverse.fluentjdbc.runtime.FluentJdbcConfig$$CMImpl").build();
    }

    @BuildStep
    public AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(ParamSetter.class)
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
