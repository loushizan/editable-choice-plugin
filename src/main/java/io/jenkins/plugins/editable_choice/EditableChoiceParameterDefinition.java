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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.RelativePath;
import hudson.Util;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.util.ComboBoxModel;
import net.sf.json.JSONObject;

/**
 * EditableChoiceParameterDefinition provides a string parameter with candidates.
 */
public class EditableChoiceParameterDefinition extends SimpleParameterDefinition {
    private static final long serialVersionUID = 4977304129086062036L;

    @NonNull
    private List<String> choices = new ArrayList<>();
    @CheckForNull
    private String defaultValue = null;
    private boolean restrict = false;
    @CheckForNull
    private FilterConfig filterConfig = null;

    /**
     * ctor.
     *
     * @param name the name of the parameter
     * @param description the description of the parameter
     */
    @DataBoundConstructor
    public EditableChoiceParameterDefinition(@NonNull final String name, @CheckForNull final String description) {
        super(name, description);
    }

    /**
     * ctor.
     *
     * @param name the name of the parameter
     */
    public EditableChoiceParameterDefinition(@NonNull final String name) {
        this(name, null);
    }

    /**
     * @param choices choices used as candidates
     */
    @DataBoundSetter
    public void setChoices(@NonNull final List<String> choices) {
        this.choices = choices;
    }

    /**
     * @return choices
     */
    @Exported
    public List<String> getChoices() {
        return choices;
    }

    /**
     * @param text choices delimited with new lines
     * @return choices
     */
    @NonNull
    public static List<String> choicesFromText(@NonNull final String text) {
        final List<String> stringList = Arrays.asList(text.split("\\r?\\n", -1));
        if (stringList.isEmpty() || !StringUtils.isEmpty(stringList.get(stringList.size() - 1))) {
            return stringList;
        }

        // Ignore the last empty line
        final List<String> newList = new ArrayList<>();
        for (int i = 0; i < stringList.size() - 1; ++i) {
            newList.add(stringList.get(i));
        }
        return newList;
    }

    /**
     * @param choices choices
     * @return choices with delimited with new lines
     */
    public static String textFromChoices(@NonNull final List<String> choices) {
        final StringBuffer sb = new StringBuffer();
        for (final String s : choices) {
            sb.append(s);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * @param choicesWithText choices with delimited with new lines
     */
    @DataBoundSetter
    public void setChoicesWithText(@NonNull final String choicesWithText) {
        setChoices(choicesFromText(choicesWithText));
    }

    /**
     * @return choices with delimited with new lines
     */
    @NonNull
    public String getChoicesWithText() {
        return textFromChoices(getChoices());
    }

    /**
     * @param choices choices
     * @return this instance
     */
    public EditableChoiceParameterDefinition withChoices(final List<String> choices) {
        setChoices(choices);
        return this;
    }

    /**
     * @param defaultValue the default value. The top choice is used if
     *                     {@code null}.
     */
    @DataBoundSetter
    public void setDefaultValue(@CheckForNull final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * @return the default value. The top choice is used if {@code null}.
     */
    @Exported
    @CheckForNull
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Set the default value with {@link DefaultValue}.
     *
     * Only for used with &lt;f:optionalBlock&gt;.
     * Use {@link #setDefaultValue()} instead.
     *
     * @param defaultValue the default value with {@link DefaultValue}
     */
    @Restricted(NoExternalUse.class)
    @DataBoundSetter
    public void setWithDefaultValue(@NonNull final DefaultValue defaultValue) {
        this.defaultValue = defaultValue.getDefaultValue();
    }

    /**
     * Return the default value with {@link DefaultValue}.
     *
     * Only for used with &lt;f:optionalBlock&gt;.
     * Use {@link #getDefaultValue()} instead.
     *
     * @return the default value with {@link DefaultValue}
     */
    @Restricted(NoExternalUse.class)
    @CheckForNull
    public DefaultValue getWithDefaultValue() {
        final String value = getDefaultValue();
        return (value != null) ? new DefaultValue(value) : null;
    }

    /**
     * @param defaultValue the default value
     * @return this instance
     */
    public EditableChoiceParameterDefinition withDefaultValue(@CheckForNull final String defaultValue) {
        setDefaultValue(defaultValue);
        return this;
    }

    /**
     * @param restrict whether restrict value in choices
     */
    @DataBoundSetter
    public void setRestrict(final boolean restrict) {
        this.restrict = restrict;
    }

    /**
     * @return whether restrict value in choices
     */
    @Exported
    public boolean isRestrict() {
        return restrict;
    }

    /**
     * @param restrict whether restrict value in choices
     * @return this instance
     */
    public EditableChoiceParameterDefinition withRestrict(final boolean restrict) {
        setRestrict(restrict);
        return this;
    }

    /**
     * @param filterConfig how to filter values for input. {@code null} not to filter.
     */
    @DataBoundSetter
    public void setFilterConfig(@CheckForNull final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
     * @return how to filter values for input. {@code null} not to filter.
     */
    @CheckForNull
    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    /**
     * @param filterConfig how to filter values for input
     * @return this instance
     */
    public EditableChoiceParameterDefinition withFilterConfig(@CheckForNull final FilterConfig filterConfig) {
        setFilterConfig(filterConfig);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @CheckForNull
    public ParameterValue getDefaultParameterValue() {
        final String defaultValue = getDefaultValue();
        if (defaultValue != null) {
            return new StringParameterValue(getName(), defaultValue, getDescription());
        }
        final List<String> choices = getChoices();
        if (choices.size() <= 0) {
            return null;
        }
        return new StringParameterValue(getName(), choices.get(0), getDescription());
    }

    /**
     * @param value candidate input
     * @return whether the value is allowed (e.g. value in choices)
     */
    protected boolean checkValue(@NonNull final String value) {
        if (!isRestrict()) {
            return true;
        }
        return getChoices().contains(value);
    }

    /**
     * @param value the user input
     * @return the value of this parameter.
     * @throws IllegalArgumentException The value is not in choices even not
     *                                  editable.
     */
    protected ParameterValue createValueCommon(final StringParameterValue value) throws IllegalArgumentException {
        if (!checkValue(value.getValue())) {
            throw new IllegalArgumentException(
                    Messages.EditableChoiceParameterDefinition_IllegalChoice(value.getValue(), value.getName()));
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException The value is not in choices when restricted.
     */
    @Override
    public ParameterValue createValue(final StaplerRequest request, final JSONObject jo)
            throws IllegalArgumentException {
        final StringParameterValue value = request.bindJSON(StringParameterValue.class, jo);
        value.setDescription(getDescription());

        return createValueCommon(value);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException The value is not in choices when restricted.
     */
    @Override
    public ParameterValue createValue(final String value) throws IllegalArgumentException {
        return createValueCommon(
            new StringParameterValue(getName(), Util.fixNull(value), getDescription())
        );
    }

    /**
     * Descriptor for {@link EditableChoiceParameterDefinition}.
     */
    @Extension
    @Symbol("editableChoice")
    public static class DescriptorImpl extends ParameterDescriptor {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.EditableChoiceParameterDefinition_DisplayName();
        }

        /**
         * @param choicesWithText choices that the user inputing
         * @return choices
         */
        public ComboBoxModel doFillDefaultValueItems(
            @CheckForNull @RelativePath("..") @QueryParameter("choicesWithText") final String choicesWithText
        ) {
            final ComboBoxModel ret = new ComboBoxModel();
            if (choicesWithText == null) {
                return ret;
            }
            ret.addAll(choicesFromText(choicesWithText));
            return ret;
        }
    }
}
