/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.artifacts.dsl;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.query.ArtifactResolutionQuery;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.artifacts.transform.TransformSpec;
import org.gradle.api.artifacts.type.ArtifactTypeContainer;
import org.gradle.api.attributes.AttributesSchema;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * <p>A {@code DependencyHandler} is used to declare dependencies. Dependencies are grouped into
 * configurations (see {@link org.gradle.api.artifacts.Configuration}).</p>
 *
 * <p>To declare a specific dependency for a configuration you can use the following syntax:</p>
 *
 * <pre>
 * dependencies {
 *     <i>configurationName</i>(<i>dependencyNotation</i>)
 * }
 * </pre>
 *
 * <p>Example shows a basic way of declaring dependencies.
 * <pre class='autoTested'>
 * plugins {
 *     id("java-library")
 * }
 *
 * dependencies {
 *     // Declare dependency on module components
 *     // Coordinates are separated by a single colon -- group:name:version
 *     implementation("org.apache.commons:commons-lang3:3.17.0")
 *     testImplementation("org.mockito:mockito-core:5.18.0")
 *
 *     // Declare dependency on arbitrary files
 *     implementation(files("hibernate.jar", "libs/spring.jar"))
 *
 *     // Declare dependency on all jars from the 'libs' directory
 *     implementation(fileTree("libs"))
 * }
 * </pre>
 *
 * <h2>Advanced dependency configuration</h2>
 * <p>To perform advanced configuration on a dependency when it is declared, you can pass an additional configuration closure:</p>
 *
 * <pre>
 * dependencies {
 *     <i>configurationName</i>(<i>dependencyNotation</i>){
 *         <i>configStatement1</i>
 *         <i>configStatement2</i>
 *     }
 * }
 * </pre>
 *
 * Examples of advanced dependency declaration include:
 * <ul>
 * <li>Forcing certain dependency version in case of the conflict.</li>
 * <li>Excluding certain dependencies by name, group or both.
 *      More details about per-dependency exclusions can be found in
 *      docs for {@link org.gradle.api.artifacts.ModuleDependency#exclude(java.util.Map)}.</li>
 * <li>Avoiding transitive dependencies for certain dependency.</li>
 * </ul>
 *
 * <pre class='autoTestedWithDeprecations'>
 * plugins {
 *     id("java-library")
 * }
 *
 * dependencies {
 *     implementation('org.hibernate:hibernate') {
 *         // In case of versions conflict '3.1' version of hibernate wins:
 *         version {
 *             strictly('3.1')
 *         }
 *
 *         // Excluding a particular transitive dependency:
 *         exclude module: 'cglib' //by artifact name
 *         exclude group: 'org.jmock' //by group
 *         exclude group: 'org.unwanted', module: 'iAmBuggy' //by both name and group
 *
 *         // Disabling all transitive dependencies of this dependency
 *         transitive = false
 *     }
 * }
 * </pre>
 *
 * Below are more examples of advanced configuration, which may be useful when a target component has multiple artifacts:
 * <ul>
 *   <li>Declaring dependency on a specific configuration of the component.</li>
 *   <li>Declaring explicit artifact requests. See also {@link org.gradle.api.artifacts.ModuleDependency#artifact(groovy.lang.Closure)}.</li>
 * </ul>
 *
 * <pre class='autoTested'>
 * plugins {
 *     id("java-library")
 * }
 *
 * dependencies {
 *     // Configuring dependency to specific configuration of the module
 *     // This notation should _only_ be used for Ivy dependencies
 *     implementation("org.someOrg:someModule:1.0") {
 *         targetConfiguration = "someConf"
 *     }
 *
 *     // Configuring dependency on 'someLib' module
 *     implementation("org.myorg:someLib:1.0") {
 *         // Explicitly adding the dependency artifact:
 *         // Prefer variant-aware dependency resolution
 *         artifact {
 *             // Useful when some artifact properties unconventional
 *             name = "someArtifact" // Artifact name different than module name
 *             extension = "someExt"
 *             type = "someType"
 *             classifier = "someClassifier"
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Dependency notations</h2>
 *
 * <p>There are several supported dependency notations. These are described below. For each dependency declared this
 * way, a {@link Dependency} object is created. You can use this object to query or further configure the
 * dependency.</p>
 *
 * <p>You can also always add instances of
 * {@link org.gradle.api.artifacts.Dependency} directly:</p>
 *
 * <code><i>configurationName</i>(&lt;instance&gt;)</code>
 *
 * <p>Dependencies can also be declared with a {@link org.gradle.api.provider.Provider} that provides any of the other supported dependency notations.</p>
 *
 * <h3>External dependencies</h3>
 *
 * <p>The following notation is used to declare a dependency on an external module:</p>
 *
 * <code><i>configurationName</i>("<i>group</i>:<i>name</i>:<i>version</i>:<i>classifier</i>@<i>extension</i>")</code>
 *
 * <p>All properties, except name, are optional.</p>
 *
 * <p>External dependencies are represented by a {@link
 * org.gradle.api.artifacts.ExternalModuleDependency}.</p>
 *
 * <pre class='autoTested'>
 * plugins {
 *     id("java-library")
 * }
 *
 * dependencies {
 *     // Declare dependency on module components
 *     // Coordinates are separated by a single colon -- group:name:version
 *     implementation("org.apache.commons:commons-lang3:3.17.0")
 *     testImplementation("org.mockito:mockito-core:5.18.0")
 * }
 * </pre>
 *
 * <h3>Project dependencies</h3>
 *
 * <p>To add a project dependency, you use the following notation:
 * <p><code><i>configurationName</i>(project(":some-project"))</code>
 *
 * <p>Project dependencies are resolved by treating each consumable configuration in the target
 * project as a variant and performing variant-aware attribute matching against them.
 * However, in order to override this process, an explicit target configuration can be specified:
 * <p><code><i>configurationName</i>(project(path: ":project-a", configuration: "someOtherConfiguration"))</code>
 *
 * <p>Project dependencies are represented using a {@link org.gradle.api.artifacts.ProjectDependency}.
 *
 * <h3>File dependencies</h3>
 *
 * <p>You can also add a dependency using a {@link org.gradle.api.file.FileCollection}:</p>
 * <code><i>configurationName</i>(files("a file"))</code>
 *
 * <pre class='autoTested'>
 * plugins {
 *     id("java-library")
 * }
 *
 * dependencies {
 *     // Declare dependency on arbitrary files
 *     implementation(files("hibernate.jar", "libs/spring.jar"))
 *
 *     // Declare dependency on all jars from the 'libs' directory
 *     implementation(fileTree("libs"))
 * }
 * </pre>
 *
 * <p>File dependencies are represented using a {@link org.gradle.api.artifacts.FileCollectionDependency}.</p>
 *
 * <h3>Gradle distribution specific dependencies</h3>
 *
 * <p>It is possible to depend on certain Gradle APIs or libraries that Gradle ships with.
 * It is particularly useful for Gradle plugin development. Example:</p>
 *
 * <pre class='autoTested'>
 * plugins {
 *     id("groovy")
 * }
 *
 * dependencies {
 *     // Declare dependency on the Groovy version that ships with Gradle
 *     implementation(localGroovy())
 *
 *     // Declare dependency on the Gradle API interfaces and classes
 *     implementation(gradleApi())
 *
 *     // Declare dependency on the Gradle test-kit to test build logic
 *     testImplementation(gradleTestKit())
 * }
 * </pre>
 */
@ServiceScope(Scope.Project.class)
public interface DependencyHandler extends ExtensionAware {
    /**
     * Adds a dependency to the given configuration.
     *
     * @param configurationName The name of the configuration.
     * @param dependencyNotation
     *
     * The dependency notation, in one of the notations described above.
     * @return The dependency, or null if dependencyNotation is a provider.
     */
    @Nullable
    Dependency add(String configurationName, Object dependencyNotation);

    /**
     * Adds a dependency to the given configuration, and configures the dependency using the given closure.
     *
     * @param configurationName The name of the configuration.
     * @param dependencyNotation The dependency notation, in one of the notations described above.
     * @param configureClosure The closure to use to configure the dependency.
     * @return The dependency, or null if dependencyNotation is a provider.
     */
    @Nullable
    Dependency add(String configurationName, Object dependencyNotation, Closure configureClosure);

    /**
     * Adds a dependency provider to the given configuration, eventually configures the dependency using the given action.
     *
     * @param configurationName The name of the configuration.
     * @param dependencyNotation The dependency provider notation, in one of the notations described above.
     * @param configuration The action to use to configure the dependency.
     *
     * @since 6.8
     */
    <T, U extends ExternalModuleDependency> void addProvider(String configurationName, Provider<T> dependencyNotation, Action<? super U> configuration);

    /**
     * Adds a dependency provider to the given configuration.
     *
     * @param configurationName The name of the configuration.
     * @param dependencyNotation The dependency provider notation, in one of the notations described above.
     *
     * @since 7.0
     */
    <T> void addProvider(String configurationName, Provider<T> dependencyNotation);

    /**
     * Adds a dependency provider to the given configuration, eventually configures the dependency using the given action.
     *
     * @param configurationName The name of the configuration.
     * @param dependencyNotation The dependency provider notation, in one of the notations described above.
     * @param configuration The action to use to configure the dependency.
     *
     * @since 7.4
     */
    <T, U extends ExternalModuleDependency> void addProviderConvertible(String configurationName, ProviderConvertible<T> dependencyNotation, Action<? super U> configuration);

    /**
     * Adds a dependency provider to the given configuration.
     *
     * @param configurationName The name of the configuration.
     * @param dependencyNotation The dependency provider notation, in one of the notations described above.
     *
     * @since 7.4
     */
    <T> void addProviderConvertible(String configurationName, ProviderConvertible<T> dependencyNotation);

    /**
     * Creates a dependency without adding it to a configuration.
     *
     * @param dependencyNotation The dependency notation, in one of the notations described above.
     * @return The dependency.
     */
    Dependency create(Object dependencyNotation);

    /**
     * Creates a dependency without adding it to a configuration, and configures the dependency using
     * the given closure.
     *
     * @param dependencyNotation The dependency notation, in one of the notations described above.
     * @param configureClosure The closure to use to configure the dependency.
     * @return The dependency.
     */
    Dependency create(Object dependencyNotation, Closure configureClosure);

    /**
     * Creates a dependency on a project.
     *
     * @param notation The project notation, in one of the notations described above.
     * @return The dependency.
     */
    Dependency project(Map<String, ?> notation);

    /**
     * Creates a dependency on the API of the current version of Gradle.
     *
     * @return The dependency.
     */
    Dependency gradleApi();

    /**
     * Creates a dependency on the <a href="https://docs.gradle.org/current/userguide/test_kit.html" target="_top">Gradle test-kit</a> API.
     *
     * @return The dependency.
     * @since 2.6
     */
    Dependency gradleTestKit();

    /**
     * Creates a dependency on the Groovy that is distributed with the current version of Gradle.
     *
     * @return The dependency.
     */
    Dependency localGroovy();

    /**
     * Returns the dependency constraint handler for this project.
     *
     * @return the dependency constraint handler for this project
     * @since 4.5
     */
    DependencyConstraintHandler getConstraints();

    /**
     * Configures dependency constraint for this project.
     *
     * <p>This method executes the given action against the {@link org.gradle.api.artifacts.dsl.DependencyConstraintHandler} for this project.</p>
     *
     * @param configureAction the action to use to configure module metadata
     * @since 4.5
     */
    void constraints(Action<? super DependencyConstraintHandler> configureAction);

    /**
     * Returns the component metadata handler for this project. The returned handler can be used for adding rules
     * that modify the metadata of depended-on software components.
     *
     * @return the component metadata handler for this project
     * @since 1.8
     */
    ComponentMetadataHandler getComponents();

    /**
     * Configures component metadata for this project.
     *
     * <p>This method executes the given action against the {@link org.gradle.api.artifacts.dsl.ComponentMetadataHandler} for this project.</p>
     *
     * @param configureAction the action to use to configure module metadata
     * @since 1.8
     */
    void components(Action<? super ComponentMetadataHandler> configureAction);

    /**
     * Returns the component module metadata handler for this project. The returned handler can be used for adding rules
     * that modify the metadata of depended-on software components.
     *
     * @return the component module metadata handler for this project
     * @since 2.2
     */
    ComponentModuleMetadataHandler getModules();

    /**
     * Configures module metadata for this project.
     *
     * <p>This method executes the given action against the {@link org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler} for this project.
     *
     * @param configureAction the action to use to configure module metadata
     * @since 2.2
     */
    void modules(Action<? super ComponentModuleMetadataHandler> configureAction);

    /**
     * Creates an artifact resolution query.
     * <p>
     * This is a legacy API and is in maintenance mode. In future versions of Gradle,
     * this API will be deprecated and removed. New code should not use this API. Prefer
     * {@link ArtifactView.ViewConfiguration#withVariantReselection()} for resolving
     * sources and javadoc.
     *
     * @since 2.0
     */
    ArtifactResolutionQuery createArtifactResolutionQuery();

    /**
     * Configures the attributes schema. The action is passed a {@link AttributesSchema} instance.
     * @param configureAction the configure action
     * @return the configured schema
     *
     * @since 3.4
     */
    AttributesSchema attributesSchema(Action<? super AttributesSchema> configureAction);

    /**
     * Returns the attributes schema for this handler.
     * @return the attributes schema
     *
     * @since 3.4
     */
    AttributesSchema getAttributesSchema();

    /**
     * Returns the artifact type definitions for this handler.
     * @since 4.0
     */
    ArtifactTypeContainer getArtifactTypes();

    /**
     * Configures the artifact type definitions for this handler.
     * @since 4.0
     */
    void artifactTypes(Action<? super ArtifactTypeContainer> configureAction);

    /**
     * Registers an <a href="https://docs.gradle.org/current/userguide/artifact_transforms.html">artifact transform</a>.
     *
     * <p>
     *     The registration action needs to specify the {@code from} and {@code to} attributes.
     *     It may also provide parameters for the transform action by using {@link TransformSpec#parameters(Action)}.
     * </p>
     *
     * <p>For example:</p>
     *
     * <pre class='autoTested'>
     * // You have a transform action like this:
     * abstract class MyTransform implements TransformAction&lt;Parameters&gt; {
     *     interface Parameters extends TransformParameters {
     *         {@literal @}Input
     *         Property&lt;String&gt; getStringParameter();
     *         {@literal @}InputFiles
     *         ConfigurableFileCollection getInputFiles();
     *     }
     *
     *     void transform(TransformOutputs outputs) {
     *         // ...
     *     }
     * }
     *
     * // Then you can register the action like this:
     *
     * def artifactType = Attribute.of('artifactType', String)
     *
     * dependencies.registerTransform(MyTransform) {
     *     from.attribute(artifactType, "jar")
     *     to.attribute(artifactType, "java-classes-directory")
     *
     *     parameters {
     *         stringParameter.set("Some string")
     *         inputFiles.from("my-input-file")
     *     }
     * }
     * </pre>
     *
     * @see TransformAction
     * @since 5.3
     */
    <T extends TransformParameters> void registerTransform(Class<? extends TransformAction<T>> actionType, Action<? super TransformSpec<T>> registrationAction);

    /**
     * Declares a dependency on a platform. If the target coordinates represent multiple
     * potential components, the platform component will be selected, instead of the library.
     *
     * @param notation the coordinates of the platform
     *
     * @since 5.0
     */
    Dependency platform(Object notation);

    /**
     * Declares a dependency on a platform. If the target coordinates represent multiple
     * potential components, the platform component will be selected, instead of the library.
     *
     * @param notation the coordinates of the platform
     * @param configureAction the dependency configuration block
     *
     * @since 5.0
     */
    Dependency platform(Object notation, Action<? super Dependency> configureAction);

    /**
     * Declares a dependency on an enforced platform. If the target coordinates represent multiple
     * potential components, the platform component will be selected, instead of the library.
     * An enforced platform is a platform for which the direct dependencies are forced, meaning
     * that they would override any other version found in the graph.
     *
     * @param notation the coordinates of the platform
     *
     * @since 5.0
     */
    Dependency enforcedPlatform(Object notation);

    /**
     * Declares a dependency on an enforced platform. If the target coordinates represent multiple
     * potential components, the platform component will be selected, instead of the library.
     * An enforced platform is a platform for which the direct dependencies are forced, meaning
     * that they would override any other version found in the graph.
     *
     * @param notation the coordinates of the platform
     * @param configureAction the dependency configuration block
     *
     * @since 5.0
     */
    Dependency enforcedPlatform(Object notation, Action<? super Dependency> configureAction);

    /**
     * Declares a dependency on the test fixtures of a component.
     * @param notation the coordinates of the component to use test fixtures for
     *
     * @since 5.6
     */
    Dependency testFixtures(Object notation);

    /**
     * Declares a dependency on the test fixtures of a component and allows configuring
     * the resulting dependency.
     * @param notation the coordinates of the component to use test fixtures for
     *
     * @since 5.6
     */
    Dependency testFixtures(Object notation, Action<? super Dependency> configureAction);

    /**
     * Allows fine-tuning what variant to select for the target dependency. This can be used to
     * specify a classifier, for example.
     *
     * @param dependencyProvider the dependency provider
     * @param variantSpec the variant specification
     * @return a new dependency provider targeting the configured variant
     * @since 6.8
     */
    Provider<MinimalExternalModuleDependency> variantOf(Provider<MinimalExternalModuleDependency> dependencyProvider, Action<? super ExternalModuleDependencyVariantSpec> variantSpec);

    /**
     * Allows fine-tuning what variant to select for the target dependency. This can be used to
     * specify a classifier, for example.
     *
     * @param dependencyProviderConvertible the dependency provider convertible that returns the dependency provider
     * @param variantSpec the variant specification
     * @return a new dependency provider targeting the configured variant
     * @since 7.3
     */
    default Provider<MinimalExternalModuleDependency> variantOf(ProviderConvertible<MinimalExternalModuleDependency> dependencyProviderConvertible,
                                                                Action<? super ExternalModuleDependencyVariantSpec> variantSpec) {
        return variantOf(dependencyProviderConvertible.asProvider(), variantSpec);
    }

    /**
     * Configures this dependency provider to select the platform variant of the target component
     * @param dependencyProvider the dependency provider
     * @return a new dependency provider targeting the platform variant of the component
     * @since 6.8
     */
    default Provider<MinimalExternalModuleDependency> platform(Provider<MinimalExternalModuleDependency> dependencyProvider) {
        return variantOf(dependencyProvider, ExternalModuleDependencyVariantSpec::platform);
    }

    /**
     * Configures this dependency provider to select the platform variant of the target component
     * @param dependencyProviderConvertible the dependency provider convertible that returns the dependency provider
     * @return a new dependency provider targeting the platform variant of the component
     * @since 7.3
     */
    default Provider<MinimalExternalModuleDependency> platform(ProviderConvertible<MinimalExternalModuleDependency> dependencyProviderConvertible) {
        return platform(dependencyProviderConvertible.asProvider());
    }

    /**
     * Configures this dependency provider to select the enforced-platform variant of the target component
     * @param dependencyProvider the dependency provider
     * @return a new dependency provider targeting the enforced-platform variant of the component
     * @since 7.3
     */
    Provider<MinimalExternalModuleDependency> enforcedPlatform(Provider<MinimalExternalModuleDependency> dependencyProvider);

    /**
     * Configures this dependency provider to select the enforced-platform variant of the target component
     * @param dependencyProviderConvertible the dependency provider convertible that returns the dependency provider
     * @return a new dependency provider targeting the enforced-platform variant of the component
     * @since 7.3
     */
    default Provider<MinimalExternalModuleDependency> enforcedPlatform(ProviderConvertible<MinimalExternalModuleDependency> dependencyProviderConvertible) {
        return enforcedPlatform(dependencyProviderConvertible.asProvider());
    }

    /**
     * Configures this dependency provider to select the test fixtures of the target component
     * @param dependencyProvider the dependency provider
     * @return a new dependency provider targeting the test fixtures of the component
     * @since 6.8
     */
    default Provider<MinimalExternalModuleDependency> testFixtures(Provider<MinimalExternalModuleDependency> dependencyProvider) {
        return variantOf(dependencyProvider, ExternalModuleDependencyVariantSpec::testFixtures);
    }

    /**
     * Configures this dependency provider to select the test fixtures of the target component
     * @param dependencyProviderConvertible the dependency provider convertible that returns the dependency provider
     * @return a new dependency provider targeting the test fixtures of the component
     * @since 7.3
     */
    default Provider<MinimalExternalModuleDependency> testFixtures(ProviderConvertible<MinimalExternalModuleDependency> dependencyProviderConvertible) {
        return testFixtures(dependencyProviderConvertible.asProvider());
    }

}
