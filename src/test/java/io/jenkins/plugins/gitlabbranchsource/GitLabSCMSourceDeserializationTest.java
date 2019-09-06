package io.jenkins.plugins.gitlabbranchsource;

import com.thoughtworks.xstream.XStream;
import java.lang.reflect.Field;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class GitLabSCMSourceDeserializationTest {

    @Test
    public void afterDeserializationWithXStreamTransientFieldsAreNotNull() throws Exception {
        XStream xs = new XStream();
        GitLabSCMSource source = (GitLabSCMSource) xs.fromXML(xs.toXML(new GitLabSCMSource("Test Server", "Test Owner", "test-path")));
        Field mergeRequestContributorCache = source.getClass().getDeclaredField("mergeRequestContributorCache");
        mergeRequestContributorCache.setAccessible(true);
        Field mergeRequestMetadataCache = source.getClass().getDeclaredField("mergeRequestMetadataCache");
        mergeRequestMetadataCache.setAccessible(true);
        assertNotNull(mergeRequestMetadataCache.get(source));
        assertNotNull(mergeRequestContributorCache.get(source));
    }

}
