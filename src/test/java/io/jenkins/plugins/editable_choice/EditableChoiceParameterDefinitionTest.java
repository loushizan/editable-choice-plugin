/*
 * The MIT License
 *
 * Copyright (c) 2021 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.editable_choice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterValue;

public class EditableChoiceParameterDefinitionTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Run a build from view with value
     *
     * @param p          job to run
     * @param paramName  parameter name to set
     * @param paramValue parameter value to set
     * @throws Exception any exceptions
     */
    private void runBuildFromView(Job<?, ?> p, String paramName, String paramValue) throws Exception {
        WebClient wc = j.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(p, "build?delay=0sec");
        HtmlElement paramBlock = page.getFirstByXPath(String.format("//*[@data-parameter='%s']", paramName));
        HtmlTextInput input = paramBlock.getOneHtmlElementByAttribute("input", "name", "value");
        input.setValueAttribute(paramValue);
        j.submit(page.getFormByName("parameters"));
        j.waitUntilNoActivity();
    }

    @Test
    public void configSimple() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
        ));
        j.configRoundtrip(p);
    }

    @Test
    public void configFull1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1", "description1\ndescription2")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape").withRestrict(true)
                .withFilterConfig(
                    new FilterConfig()
                        .withPrefix(false)
                        .withCaseInsensitive(true)
                )
            )
        );
        j.configRoundtrip(p);
    }

    @Test
    public void configFull2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1", "description1\ndescription2")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
                .withRestrict(false)
                .withFilterConfig(
                    new FilterConfig()
                        .withPrefix(true)
                        .withCaseInsensitive(false)
                )
            )
        );
        j.configRoundtrip(p);
    }

    @Test
    public void useTopMost() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        j.buildAndAssertSuccess(p);
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("Apple")));
    }

    @Test
    public void useDefault() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        j.buildAndAssertSuccess(p);
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("Grape")));
    }

    @Test
    public void useEmptyDefault() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        j.buildAndAssertSuccess(p);
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("")));
    }

    @Test
    public void useDefaultNotInChoice() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grapefruit")
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        j.buildAndAssertSuccess(p);
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("Grapefruit")));
    }

    @Test
    public void specifyValue() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        runBuildFromView(p, "PARAM1", "Orange");
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("Orange")));
    }

    @Test
    public void specifyValueNotInChoices() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        runBuildFromView(p, "PARAM1", "Grapefruit");
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("Grapefruit")));
    }

    @Test
    public void specifyValueEmpty() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        runBuildFromView(p, "PARAM1", "");
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("")));
    }

    @Test
    public void restrictWithDefault() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
                .withRestrict(true)
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        j.buildAndAssertSuccess(p);
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("Grape")));
    }

    @Test(expected=IllegalArgumentException.class)
    public void restrictWithDefaultNotInChoice() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grapefruit")
                .withRestrict(true)
        ));
        p.scheduleBuild2(0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void restrictWithDefaultEmpty() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
                .withRestrict(true)
        ));
        p.scheduleBuild2(0);
    }

    @Test
    public void restrictSpecifyValue() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
                .withRestrict(true)
        ));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        runBuildFromView(p, "PARAM1", "Orange");
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertThat(ceb.getEnvVars().get("PARAM1"), is(equalTo("Orange")));
    }

    @Test
    public void restrictSpecifyValueNotInChoices() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
                .withRestrict(true)
        ));
        runBuildFromView(p, "PARAM1", "Grapefruit");
        // prevents submitting form.
        assertThat(p.getLastBuild(), is(nullValue()));
    }

    @Test
    public void restrictSpecifyValueEmpty() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Grape")
                .withRestrict(true)
        ));
        runBuildFromView(p, "PARAM1", "");
        // prevents submitting form.
        assertThat(p.getLastBuild(), is(nullValue()));
    }
}
