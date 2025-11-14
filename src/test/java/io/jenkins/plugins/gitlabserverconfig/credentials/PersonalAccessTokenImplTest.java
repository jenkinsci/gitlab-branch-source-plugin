package io.jenkins.plugins.gitlabserverconfig.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.DataBoundConstructor;

@WithJenkins
class PersonalAccessTokenImplTest {

    private static JenkinsRule j;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void configRoundtrip() throws Exception {
        PersonalAccessTokenImpl expected = new PersonalAccessTokenImpl(
                CredentialsScope.GLOBAL, "magic-id", "configRoundtrip", "sAf_Xasnou47yxoAsC");
        CredentialsBuilder builder = new CredentialsBuilder(expected);
        j.configRoundtrip(builder);
        j.assertEqualDataBoundBeans(expected, builder.credentials);
    }

    /**
     * Helper for {@link #configRoundtrip()}.
     */
    public static class CredentialsBuilder extends Builder {

        public final Credentials credentials;

        @DataBoundConstructor
        public CredentialsBuilder(Credentials credentials) {
            this.credentials = credentials;
        }

        @TestExtension
        public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

            @Override
            public @NotNull String getDisplayName() {
                return "CredentialsBuilder";
            }

            @Override
            public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                return true;
            }
        }
    }
}
