package io.jenkins.plugins.gitlabserverconfig.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;

public class GroupAccessTokenImplTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void configRoundtrip() throws Exception {
        GroupAccessTokenImpl expected =
                new GroupAccessTokenImpl(CredentialsScope.GLOBAL, "magic-id", "configRoundtrip", "sAf_Xasnou47yxoAsC");
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
            public String getDisplayName() {
                return "CredentialsBuilder";
            }

            @SuppressWarnings("rawtypes")
            @Override
            public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                return true;
            }
        }
    }
}
