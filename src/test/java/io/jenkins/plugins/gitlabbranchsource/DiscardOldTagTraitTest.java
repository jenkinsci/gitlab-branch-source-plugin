package io.jenkins.plugins.gitlabbranchsource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.gitlabbranchsource.DiscardOldTagTrait.ExcludeOldSCMTag;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.trait.SCMHeadPrefilter;
import jenkins.scm.impl.NullSCMSource;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DiscardOldTagTraitTest {

    static Stream<Arguments> tagSCMHeadProvider() {
        return Stream.of(
                Arguments.argumentSet(
                        "expired",
                        new GitLabTagSCMHead(
                                "tag/1234", DateUtils.addDays(new Date(), -6).getTime()),
                        true),
                Arguments.argumentSet(
                        "too_recent",
                        new GitLabTagSCMHead(
                                "tag/1234", DateUtils.addDays(new Date(), -4).getTime()),
                        false),
                Arguments.argumentSet("no_timestamp", new GitLabTagSCMHead("tag/zer0", 0L), false),
                Arguments.argumentSet("not_a_tag", new BranchSCMHead("someBranch"), false));
    }

    @ParameterizedTest
    @MethodSource("tagSCMHeadProvider")
    void verify_tag_filtering(SCMHead head, boolean expectedResult) {
        DiscardOldTagTrait uut = new DiscardOldTagTrait(5);
        GitLabSCMSourceContext context = new GitLabSCMSourceContext(null, SCMHeadObserver.none());
        uut.decorateContext(context);

        Optional<SCMHeadPrefilter> optFilter = context.prefilters().stream()
                .filter(it -> ExcludeOldSCMTag.class.equals(it.getClass()))
                .findFirst();
        assertTrue(optFilter.isPresent());

        SCMHeadPrefilter filter = optFilter.get();

        assertEquals(filter.isExcluded(new NullSCMSource(), head), expectedResult);
    }
}
