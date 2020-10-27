package uk.ac.napier.soc.ssd.coursework.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import uk.ac.napier.soc.ssd.coursework.abac.security.spring.ContextAwarePolicyEnforcement;
import uk.ac.napier.soc.ssd.coursework.domain.Enrollment;
import uk.ac.napier.soc.ssd.coursework.repository.EnrollmentRepository;
import uk.ac.napier.soc.ssd.coursework.repository.HibernateUtil;
import uk.ac.napier.soc.ssd.coursework.repository.search.EnrollmentSearchRepository;
import uk.ac.napier.soc.ssd.coursework.web.rest.errors.BadRequestAlertException;
import uk.ac.napier.soc.ssd.coursework.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing Enrollment.
 */
@RestController
@RequestMapping("/api")
public class EnrollmentResource {

    private final Logger log = LoggerFactory.getLogger(EnrollmentResource.class);

    private static final String ENTITY_NAME = "enrollment";

    private final EnrollmentRepository enrollmentRepository;

    private final EnrollmentSearchRepository enrollmentSearchRepository;

    @Autowired
    private ContextAwarePolicyEnforcement policy;

    public EnrollmentResource(EnrollmentRepository enrollmentRepository, EnrollmentSearchRepository enrollmentSearchRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSearchRepository = enrollmentSearchRepository;
    }

    /**
     * POST  /enrollments : Create a new enrollment.
     *
     * @param enrollment the enrollment to create
     * @return the ResponseEntity with status 201 (Created) and with body the new enrollment, or with status 400 (Bad Request) if the enrollment has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/enrollments")
    @Timed
    @PreAuthorize("hasPermission(#enrollment, 'CREATE_ENROLLMENT')")
    public ResponseEntity<Enrollment> createEnrollment(@RequestBody Enrollment enrollment) throws URISyntaxException {
        log.debug("REST request to save Enrollment : {}", enrollment);
        if (enrollment.getId() != null) {
            throw new BadRequestAlertException("A new enrollment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Enrollment result = enrollmentRepository.save(enrollment);

        return ResponseEntity.created(new URI("/api/enrollments/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /enrollments : Updates an existing enrollment.
     *
     * @param enrollment the enrollment to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated enrollment,
     * or with status 400 (Bad Request) if the enrollment is not valid,
     * or with status 500 (Internal Server Error) if the enrollment couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/enrollments")
    @Timed
    @PreAuthorize("hasPermission(#enrollment, 'UPDATE_ENROLLMENT')")
    public ResponseEntity<Enrollment> updateEnrollment(@RequestBody Enrollment enrollment) throws URISyntaxException {
        log.debug("REST request to update Enrollment : {}", enrollment);
        if (enrollment.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Enrollment result = enrollmentRepository.save(enrollment);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, enrollment.getId().toString()))
            .body(result);
    }

    /**
     * GET  /enrollments : get all the enrollments.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many)
     * @return the ResponseEntity with status 200 (OK) and the list of enrollments in body
     */
    @GetMapping("/enrollments")
    @Timed
    @PostFilter("hasPermission(filterObject, 'VIEW_ENROLLMENTS')")
    public List<Enrollment> getAllEnrollments(@RequestParam(required = false, defaultValue = "false") boolean eagerload) {
        log.debug("REST request to get all Enrollments");
        return enrollmentRepository.findAllWithEagerRelationships();
    }

    /**
     * GET  /enrollments/:id : get the "id" enrollment.
     *
     * @param id the id of the enrollment to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the enrollment, or with status 404 (Not Found)
     */
    @GetMapping("/enrollments/{id}")
    @Timed
    public ResponseEntity<Enrollment> getEnrollment(@PathVariable Long id) {
        log.debug("REST request to get Enrollment : {}", id);
        Optional<Enrollment> enrollment = enrollmentRepository.findOneWithEagerRelationships(id);
        policy.checkPermission(enrollment.get(), "VIEW_ENROLLMENT");
        return ResponseUtil.wrapOrNotFound(enrollment);
    }

    /**
     * DELETE  /enrollments/:id : delete the "id" enrollment.
     *
     * @param id the id of the enrollment to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/enrollments/{id}")
    @Timed
    public ResponseEntity<Void> deleteEnrollment(@PathVariable Long id) {
        log.debug("REST request to delete Enrollment : {}", id);
        policy.checkPermission(enrollmentRepository.findOneWithEagerRelationships(id),"DELETE_ENROLLMENT");
        enrollmentRepository.deleteById(id);
        enrollmentSearchRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/enrollments?query=:query : search for the enrollment corresponding
     * to the query.
     *
     * @param query the query of the enrollment search
     * @return the result of the search
     */
    @GetMapping("/_search/enrollments")
    @Timed
    @PostFilter("hasPermission(filterObject, 'SEARCH_ENROLLMENTS')")
    public List<Enrollment> searchEnrollments(@RequestParam String query) {
        log.debug("REST request to search Enrollments for query {}", query);
        Session session = HibernateUtil.getSession();
        Query q = session.createQuery("select enrollment from Enrollment enrollment where enrollment.comments like '%" + query + "%'");
        return q.list();
    }

}
