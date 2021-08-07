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

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;

/**
 * EditableChoiceParameterDefinition provides a string parameter with candidates.
 */
public class EditableChoiceParameterDefinition extends SimpleParameterDefinition {
    private static final long serialVersionUID = 4977304129086062036L;

    @NonNull
    private List<String> choices = new ArrayList<>();
    private boolean editable = true;
    @CheckForNull
    private String defaultValue = null;

    /**
     * ctor
     *
     * @param name the name of the parameter
     */
    @DataBoundConstructor
    public EditableChoiceParameterDefinition(@NonNull String name) {
        super(name);
    }

    /**
     * @param choices choices used as candidates
     */
    @DataBoundSetter
    public void setChoices(@NonNull List<String> choices) {
        this.choices = choices;
    }

    /**
     * 
     * @return
     */
    @Exported
    public List<String> getChoices() {
        return choices;
    }

    /**
     * @param choicesWithText choices with delimited with new lines
     */
    @DataBoundSetter
    public void setChoicesWithText(@NonNull String choicesWithText) {
        setChoices(Arrays.asList(choicesWithText.split("\\r?\\n", -1)));
    }

    /**
     * @param editable whether the input is editable
     */
    @DataBoundSetter
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Whether the input is editable and can be a value not in choices.
     * If <code>false</code>, this works just same as the built in choice parameter.
     * Defaults to <code>true</code>.
     *
     * @return whether the input is editable
     */
    @Exported
    public boolean isEditable() {
        return editable;
    }

    /**
     * @param defaultValue the default value. The top choice is used if {@code null}.
     */
    @DataBoundSetter
    public void setDefaultValue(@CheckForNull String defaultValue) {
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
     * {@inheritDoc}
     */
    @CheckForNull
    public ParameterValue getDefaultParameterValue() {
        String defaultValue = getDefaultValue();
        if (defaultValue != null) {
            return new StringParameterValue(getName(), defaultValue, getDescription());
        }
        List<String> choices = getChoices();
        if (choices.size() <= 0) {
            return null;
        }
        return new StringParameterValue(getName(), choices.get(0), getDescription());
    }

    /**
     * @param value candidate input
     * @return whether the value is allowed (e.g. value in choices)
     */
    protected boolean checkValue(@NonNull String value) {
        if (isEditable()) {
            return true;
        }
        return getChoices().contains(value);
    }

    /**
     * @param value the user input
     * @return the value of this parameter.
     * @throws IllegalArgumentException The value is not in choices even not editable.
     */
    protected ParameterValue createValueCommon(StringParameterValue value) throws IllegalArgumentException {
        if(!checkValue(value.getValue())) {
            throw new IllegalArgumentException(
                Messages.EditableChoiceParameterDefinition_IllegalChoice(
                    value.getValue(),
                    value.getName()
                )
            );
        }
        return value;
    }

    @Override
    public ParameterValue createValue(StaplerRequest request, JSONObject jo) throws IllegalArgumentException {
        StringParameterValue value = request.bindJSON(StringParameterValue.class, jo);
        value.setDescription(getDescription());

        return createValueCommon(value);
    }

    /**
     * @param value the user input
     * @return the value of this parameter.
     * @throws IllegalArgumentException The value is not in choices even not editable.
     */
    @Override
    public ParameterValue createValue(String value) throws IllegalArgumentException {
        value = Util.fixNull(value);
        return createValueCommon(new StringParameterValue(getName(), value, getDescription()));
    }

    @Extension
    @Symbol("choice")
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.EditableChoiceParameterDefinition_DisplayName();
        }
    }
}
