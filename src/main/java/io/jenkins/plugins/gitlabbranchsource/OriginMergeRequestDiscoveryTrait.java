package io.jenkins.plugins.gitlabbranchsource;

import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.api.trait.SCMSourceTrait;

import java.util.EnumSet;

public class OriginMergeRequestDiscoveryTrait extends SCMSourceTrait {
    public OriginMergeRequestDiscoveryTrait(EnumSet<ChangeRequestCheckoutStrategy> merge) {
    }
}
