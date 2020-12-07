package org.synyx.urlaubsverwaltung.application.dao;

import org.springframework.data.repository.CrudRepository;
import org.synyx.urlaubsverwaltung.application.domain.Application;
import org.synyx.urlaubsverwaltung.application.domain.ApplicationComment;

import java.util.List;


/**
 * Repository for {@link ApplicationComment} entities.
 */
public interface ApplicationCommentRepository extends CrudRepository<ApplicationComment, Integer> {

    List<ApplicationComment> findByApplication(Application application);
}
