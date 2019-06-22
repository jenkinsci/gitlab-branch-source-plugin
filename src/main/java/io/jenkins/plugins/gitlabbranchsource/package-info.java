/**
 * <h1>Jenkins SCM API implementation for <a href="https://gitea.io/">GitLab</a></h1>.
 *
 * The primary entry points for the implementation are:
 *
 * <ul>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMSource} - the {@link jenkins.scm.api.SCMSource} implementation</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMNavigator} - the {@link jenkins.scm.api.SCMNavigator}
 * implementation</li>
 * </ul>
 *
 * These implementations are {@link jenkins.scm.api.trait.SCMTrait} based and accept the traits for
 * {@link jenkins.plugins.git.AbstractGitSCMSource} as well as the GitLab specific traits:
 * <ul>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.BranchDiscoveryTrait}</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.ForkMergeRequestDiscoveryTrait}</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.OriginMergeRequestDiscoveryTrait}</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.SSHCheckoutTrait}</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.WebhookRegistrationTrait}</li>
 * </ul>
 *
 * Extension plugins wanting to add GitLab-specific traits should target at least one of:
 * <ul>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMNavigatorContext} for
 * {@linkplain jenkins.scm.api.trait.SCMNavigatorTrait}s</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMNavigatorRequest} for
 * {@linkplain jenkins.scm.api.trait.SCMNavigatorTrait}s</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceBuilder} for
 * {@linkplain jenkins.scm.api.trait.SCMNavigatorTrait}s</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceContext} for
 * {@linkplain jenkins.scm.api.trait.SCMSourceTrait}s</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMSourceRequest} for
 * {@linkplain jenkins.scm.api.trait.SCMSourceTrait}s</li>
 * <li>{@link io.jenkins.plugins.gitlabbranchsource.GitLabSCMBuilder} for
 * {@linkplain jenkins.scm.api.trait.SCMSourceTrait}s</li>
 * </ul>
 */

package io.jenkins.plugins.gitlabbranchsource;
