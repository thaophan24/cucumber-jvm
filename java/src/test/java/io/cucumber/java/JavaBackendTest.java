package io.cucumber.java;

import io.cucumber.core.backend.Glue;
import io.cucumber.core.backend.ObjectFactory;
import io.cucumber.core.backend.StepDefinition;
import io.cucumber.core.io.MultiLoader;
import io.cucumber.core.io.ResourceLoader;
import io.cucumber.java.steps.Steps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
public class JavaBackendTest {

    @Captor
    public ArgumentCaptor<StepDefinition> stepDefinition;

    @Mock
    private Glue glue;

    @Mock
    private ObjectFactory factory;

    private JavaBackend backend;

    @BeforeEach
    public void createBackend() {
        ClassLoader classLoader = currentThread().getContextClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        this.backend = new JavaBackend(factory, factory, resourceLoader);
    }

    @Test
    public void finds_step_definitions_by_classpath_url() {
        backend.loadGlue(glue, asList(URI.create("classpath:io/cucumber/java/steps")));
        backend.buildWorld();
        verify(factory).addClass(Steps.class);
    }

    @Test
    public void detects_subclassed_glue_and_throws_exception() {
        Executable testMethod = () -> backend.loadGlue(glue, asList(URI.create("classpath:io/cucumber/java/steps"), URI.create("classpath:io/cucumber/java/incorrectlysubclassedsteps")));
        InvalidMethodException expectedThrown = assertThrows(InvalidMethodException.class, testMethod);
        assertThat(expectedThrown.getMessage(), is(equalTo("You're not allowed to extend classes that define Step Definitions or hooks. class io.cucumber.java.incorrectlysubclassedsteps.SubclassesSteps extends class io.cucumber.java.steps.Steps")));
    }

    @Test
    public void detects_repeated_annotations() {
        backend.loadGlue(glue, asList(URI.create("classpath:io/cucumber/java/repeatable")));
        verify(glue, times(2)).addStepDefinition(stepDefinition.capture());

        List<String> patterns = stepDefinition.getAllValues()
            .stream()
            .map(StepDefinition::getPattern)
            .collect(toList());
        assertThat(patterns, equalTo(asList("test", "test again")));

    }

}
