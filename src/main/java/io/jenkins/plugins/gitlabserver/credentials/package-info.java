/**
 * GitLab specific {@link com.cloudbees.plugins.credentials.Credentials} interface and default implementation.
 * Note we use an interface so that {@link com.cloudbees.plugins.credentials.CredentialsProvider} implementations
 * that store credentials external from {@link jenkins.model.Jenkins} can use {@link java.lang.reflect.Proxy}
 * to lazily instantiate {@link hudson.util.Secret} properties on access.
 */

package io.jenkins.plugins.gitlabserver.credentials;
