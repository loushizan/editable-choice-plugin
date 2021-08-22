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
import com.gargoylesoftware.htmlunit.html.HtmlFormUtil;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.javascript.host.event.KeyboardEvent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
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
    public final JenkinsRule j = new JenkinsRule();
    private final WebClient wc = j.createWebClient();

    @Before
    public void setupWebClient() throws Exception {
        wc.setThrowExceptionOnFailingStatusCode(false);
        wc.getOptions().setPrintContentOnFailingStatusCode(false);
    }

    private HtmlPage getBuildPage(final Job<?, ?> p) throws Exception {
        return wc.getPage(p, "build?delay=0sec");
    }

    /*
    // Unfortunatelly, htmlunit doesn't simulate browser behaviors for
    // typing "ENTER" and "TAB" exactly.
    // We'll test not browser behavior but result of `preventDefault()` instead.
    private void typeEnter(final HtmlElement e) throws Exception {
        // NOTICE: this doesn't simulate actual browser behavior.
        // This doesn't trigger form submittion in Htmlunit.
        e.type(KeyboardEvent.DOM_VK_RETURN);
        // This always submit form even if `preventDefault()` called.
        e.type('\n');
    }

    private void typeTab(final HtmlElement e) throws Exception {
        // NOTICE: this doesn't simulate actual browser behavior.
        // This doesn't move focus
        e.type(KeyboardEvent.DOM_VK_TAB);
        // This doesn't move focus
        e.type('\t');
        // This doesn't trigger keydown event
        e.getHtmlPageOrNull().tabToNextElement();
    }
    */

    private void assertKeydownNotInturrupted(final HtmlElement e, final int code) throws Exception {
        final KeyboardEvent evt = new KeyboardEvent(
            e,
            KeyboardEvent.TYPE_KEY_DOWN,
            code,
            false,
            false,
            false
        );
        e.fireEvent(evt);
        assertThat(
            evt.isDefaultPrevented(),
            is(false)
        );
    }

    private void assertKeydownInturrupted(final HtmlElement e, final int code) throws Exception {
        final KeyboardEvent evt = new KeyboardEvent(
            e,
            KeyboardEvent.TYPE_KEY_DOWN,
            code,
            false,
            false,
            false
        );
        e.fireEvent(evt);
        assertThat(
            evt.isDefaultPrevented(),
            is(true)
        );
    }

    private void clickSubmit(final HtmlPage page) throws Exception {
        // j.submit() triggers submit twice.
        HtmlFormUtil.getSubmitButton(page.getFormByName("parameters")).click();
    }

    private HtmlElement getSuggestInputContainer(final HtmlPage page, final String paramName) throws Exception {
        final HtmlElement paramBlock = page.querySelector(
            String.format("[data-parameter='%s']", paramName)
        );
        return paramBlock.querySelector(".editable-choice-suggest");
    }

    private HtmlElement getSuggestInputChoicesBlock(final HtmlPage page, final String paramName) throws Exception {
        final HtmlElement paramBlock = page.querySelector(
            String.format("[data-parameter='%s']", paramName)
        );
        return paramBlock.querySelector(".editable-choice-suggest-choices");
    }

    private HtmlTextInput getSuggestInputTextbox(final HtmlPage page, final String paramName) throws Exception {
        final HtmlElement paramBlock = page.querySelector(
            String.format("[data-parameter='%s']", paramName)
        );
        return paramBlock.getOneHtmlElementByAttribute("input", "name", "value");
    }

    private HtmlElement getChoice(final HtmlPage page, final String paramName, final String value) throws Exception {
        final HtmlElement choicesBlock = getSuggestInputChoicesBlock(page, paramName);
        return choicesBlock.querySelector(String.format("[data-value='%s']", value));
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
        assertThat(
            e.isDisplayed(),
            is(true)
        );
    }

    private void assertNotDisplays(final HtmlElement e) {
        assertThat(
            e.isDisplayed(),
            is(false)
        );
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
    public void testCloseSuggestion() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_ESCAPE);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
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

    @Test
    public void testUpCursorInNonSuggestion() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_ESCAPE);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_UP);
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Orange"))
        );
    }

    @Test
    public void testDownCursorInSuggestion() throws Exception {
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

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_DOWN);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Apple"))
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_DOWN);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Grape"))
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_DOWN);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Orange"))
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_DOWN);
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Apple"))
        );
    }

    @Test
    public void testDownCursorInNonSuggestion() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_ESCAPE);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_DOWN);
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Apple"))
        );
    }

    @Test
    public void testEnterInSuggestionNotSelected() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );

        assertKeydownInturrupted(getSuggestInputTextbox(page, "PARAM1"), KeyboardEvent.DOM_VK_RETURN);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );
    }

    @Test
    public void testEnterInSuggestionSelected() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );

        // `mouseOver()` seems not fire `mouseenter` event
        // getChoice(page, "PARAM1", "Grape").mouseOver();
        getChoice(page, "PARAM1", "Grape").fireEvent("mouseenter");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Grape"))
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );

        assertKeydownInturrupted(getSuggestInputTextbox(page, "PARAM1"), KeyboardEvent.DOM_VK_RETURN);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grape"))
        );
    }

    @Test
    public void testEnterInNonSuggestion() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        getSuggestInputTextbox(page, "PARAM1").type("Grapefruit");
        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_ESCAPE);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));

        assertKeydownNotInturrupted(getSuggestInputTextbox(page, "PARAM1"), KeyboardEvent.DOM_VK_RETURN);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grapefruit"))
        );
    }

    @Test
    public void testTabInSuggestionNotSelected() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );

        assertKeydownInturrupted(getSuggestInputTextbox(page, "PARAM1"), KeyboardEvent.DOM_VK_TAB);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );
    }

    @Test
    public void testTabInSuggestionSelected() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );

        // `mouseOver()` seems not fire `mouseenter` event
        // getChoice(page, "PARAM1", "Grape").mouseOver();
        getChoice(page, "PARAM1", "Grape").fireEvent("mouseenter");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Grape"))
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );

        assertKeydownInturrupted(getSuggestInputTextbox(page, "PARAM1"), KeyboardEvent.DOM_VK_TAB);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grape"))
        );
    }

    @Test
    public void testTagInNonSuggestion() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        getSuggestInputTextbox(page, "PARAM1").type("Grapefruit");
        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_ESCAPE);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));

        assertKeydownNotInturrupted(getSuggestInputTextbox(page, "PARAM1"), KeyboardEvent.DOM_VK_TAB);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grapefruit"))
        );
    }

    @Test
    public void testClickChoice() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo(""))
        );

        getChoice(page, "PARAM1", "Grape").click();
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grape"))
        );
    }

    @Test
    public void testFormSubmissionInSuggesting() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        getSuggestInputTextbox(page, "PARAM1").type("Grapefruit");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grapefruit"))
        );

        clickSubmit(page);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grapefruit"))
        );

        j.waitUntilNoActivity();
        assertThat(
            p.getLastBuild(),
            is(nullValue())
        );
    }

    @Test
    public void testFormSubmissionInNotSuggesting() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(ceb);
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        getSuggestInputTextbox(page, "PARAM1").type("Grapefruit");
        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_ESCAPE);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grapefruit"))
        );

        clickSubmit(page);

        j.waitUntilNoActivity();
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertThat(
            ceb.getEnvVars().get("PARAM1"),
            is(equalTo("Grapefruit"))
        );
    }

    @Test
    public void testInputInSuggestingSelect() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type("Grap");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grap"))
        );
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type("e");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grape"))
        );
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Grape"))
        );
    }

    @Test
    public void testInputInSuggestingDeselect() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type("Grape");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grape"))
        );
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Grape"))
        );

        getSuggestInputTextbox(page, "PARAM1").type("f");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getSuggestInputTextbox(page, "PARAM1").getValueAttribute(),
            is(equalTo("Grapef"))
        );
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );
    }

    @Test
    public void testInputInNotSuggesting() throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange"))
                .withDefaultValue("")
        ));
        final HtmlPage page = getBuildPage(p);

        getSuggestInputTextbox(page, "PARAM1").focus();
        getSuggestInputTextbox(page, "PARAM1").type(KeyboardEvent.DOM_VK_ESCAPE);
        assertNotHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertNotDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(nullValue())
        );

        getSuggestInputTextbox(page, "PARAM1").type("Grape");
        assertHasClass(getSuggestInputContainer(page, "PARAM1"), "suggesting");
        assertDisplays(getSuggestInputChoicesBlock(page, "PARAM1"));
        assertThat(
            getCurrentSelected(page, "PARAM1"),
            is(equalTo("Grape"))
        );
    }
}
