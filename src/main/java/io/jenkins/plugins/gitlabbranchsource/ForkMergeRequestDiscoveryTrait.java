package io.jenkins.plugins.gitlabbranchsource;

import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceTrait;

import java.util.EnumSet;

public class ForkMergeRequestDiscoveryTrait extends SCMSourceTrait {
    public ForkMergeRequestDiscoveryTrait(EnumSet<ChangeRequestCheckoutStrategy> merge,
                                          ForkMergeRequestDiscoveryTrait.TrustContributors trustContributors) {
    }
}
