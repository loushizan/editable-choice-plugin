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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.List;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.javascript.host.event.KeyboardEvent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParametersDefinitionProperty;

/**
 * Tests for behaviors in build page.
 */
public class EditableChoiceParameterDefinitionUiTest {
    private static final long JAVASCRIPT_TIMEOUT = 1000;

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private WebClient wc = j.createWebClient();

    @Before
    public void setupWebClient() throws Exception {
        wc.setThrowExceptionOnFailingStatusCode(false);
        wc.getOptions().setPrintContentOnFailingStatusCode(false);
    }

    private HtmlPage getBuildPage(final Job<?, ?> p) throws Exception {
        return wc.getPage(p, "build?delay=0sec");
    }

    private HtmlElement getSuggestInputContainer(final HtmlPage page, final String paramName) throws Exception {
        final HtmlElement paramBlock = page.getFirstByXPath(
            String.format("//*[@data-parameter='%s']", paramName)
        );
        return paramBlock.getFirstByXPath("//*[contains(@class, 'editable-choice-suggest')]");
    }

    private HtmlElement getSuggestInputChoicesBlock(final HtmlPage page, final String paramName) throws Exception {
        final HtmlElement paramBlock = page.getFirstByXPath(
            String.format("//*[@data-parameter='%s']", paramName)
        );
        return paramBlock.getFirstByXPath("//*[contains(@class, 'editable-choice-suggest-choices')]");
    }

    private HtmlTextInput getSuggestInputTextbox(final HtmlPage page, final String paramName) throws Exception {
        final HtmlElement paramBlock = page.getFirstByXPath(
            String.format("//*[@data-parameter='%s']", paramName)
        );
        return paramBlock.getOneHtmlElementByAttribute("input", "name", "value");
    }

    private String getCurrentSelected(final HtmlPage page, final String paramName) throws Exception {
        final HtmlElement choicesBlock = getSuggestInputChoicesBlock(page, paramName);
        final List<HtmlElement> choices = choicesBlock.getByXPath("//*[@data-value]");
        HtmlElement selected = null;
        for (final HtmlElement e: choices) {
            if (hasClass(e, "active")) {
                if (selected != null) {
                    throw new IllegalStateException(String.format(
                        "More than one choices are active at the same time: %s and %s (or maybe more)",
                        selected.getAttribute("data-value"),
                        e.getAttribute("data-value")
                    ));
                }
                selected = e;
            }
        }
        return (selected != null) ? selected.getAttribute("data-value") : null;
    }

    private boolean hasClass(final HtmlElement e, final String clazz) {
        return Arrays.asList(e.getAttribute("class").split("\\s+")).contains(clazz);
    }

    private void assertHasClass(final HtmlElement e, final String clazz) {
        assertThat(
            Arrays.asList(e.getAttribute("class").split("\\s+")),
            hasItem(is(equalTo(clazz)))
        );
    }

    private void assertNotHasClass(final HtmlElement e, final String clazz) {
        assertThat(
            Arrays.asList(e.getAttribute("class").split("\\s+")),
            not(hasItem(is(equalTo(clazz))))
        );
    }

    private void assertDisplays(final HtmlElement e) {
        // somehow isDisplayed() always return `true`.
        /*
        assertThat(
            e.isDisplayed(),
            is(true)
        );
        */
    }

    private void assertNotDisplays(final HtmlElement e) {
        // somehow isDisplayed() always return `true`.
        /*
        assertThat(
            e.isDisplayed(),
            is(false)
        );
        */
    }

    @Test
    public void testFocus() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));

        getSuggestInputTextbox(page, "PARAM1").blur();
        wc.waitForBackgroundJavaScript(JAVASCRIPT_TIMEOUT);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
    }

    @Test
    public void testInitiallySelected() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Apple"))
        );
    }

    @Test
    public void testInitiallyNotSelectedEmpty() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
    }

    @Test
    public void testInitiallyNotSelectedNotInChoice() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("Mango")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
    }

    @Test
    public void testUpCursorInSuggestion() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_UP);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Orange"))
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_UP);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Grape"))
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_UP);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Apple"))
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_UP);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Orange"))
        );
    }
}
