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

import java.util.Arrays;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.ParametersDefinitionProperty;

public class EditableChoiceParameterDefinitionWorkflowTests {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void decralativePipeline() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "test");
        p.setDefinition(new CpsFlowDefinition(
            String.join(
                "\n",
                new String[] {
                    "pipeline {",
                    "  agent any",
                    "  parameters {",
                    "    editableChoice(",
                    "      name: 'PARAM1',",
                    "      choices: ['Apple', 'Grape', 'Orange'],",
                    "    )",
                    "  }",
                    "  stages {",
                    "    stage('build') {",
                    "      steps {",
                    "        echo \"PARAM1=${params.PARAM1}\"",
                    "       }",
                    "    }",
                    "  }",
                    "}",
                }
            ),
            true
        ));
        WorkflowRun r = j.buildAndAssertSuccess(p);
        j.assertLogContains("PARAM1=Apple", r);
        j.assertEqualDataBoundBeans(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange")),
            p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("PARAM1")
        );
    }

    @Test
    public void scriptedPipeline() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "test");
        p.setDefinition(new CpsFlowDefinition(
            String.join(
                "\n",
                new String[] {
                    "properties([",
                    "  parameters([",
                    "    editableChoice(",
                    "      name: 'PARAM1',",
                    "      choices: ['Apple', 'Grape', 'Orange'],",
                    "    ),",
                    "  ]),",
                    "])",
                    "node {",
                    "  echo \"PARAM1=${params.PARAM1}\"",
                    "}",
                }
            ),
            true
        ));
        WorkflowRun r = j.buildAndAssertSuccess(p);
        j.assertLogContains("PARAM1=Apple", r);
        j.assertEqualDataBoundBeans(
            new EditableChoiceParameterDefinition("PARAM1")
                .withChoices(Arrays.asList("Apple", "Grape", "Orange")),
            p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("PARAM1")
        );
    }
}
