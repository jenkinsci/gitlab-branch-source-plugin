import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiClient;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;

import java.util.List;

public class Foo {

    public static void main(String[] args) throws GitLabApiException {
        GitLabApi gitLabApi = new GitLabApi("https://www.gitlab.com/api/v4/", "C1n738p7EQxgahE5KGXh");
        List<Project> projects = gitLabApi.getProjectApi().getProjects();
        projects.forEach((project ->
            System.out.println(project.getWebUrl())
        ));

//        User v = gitLabApi.getUserApi().getUser("baymac");
//        System.out.println(v);
//        System.out.println(v.getName());
    }
}
