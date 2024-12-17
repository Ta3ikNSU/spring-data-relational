package org.springframework.data.jdbc.repository;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory;
import org.springframework.data.jdbc.testing.IntegrationTest;
import org.springframework.data.jdbc.testing.TestConfiguration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Integration tests for verifying querying by nested properties in Spring Data JDBC repositories.
 *
 * @author Dmitriy Korolyov
 */
@IntegrationTest
public class JdbcRepositoryNestedPropertyIntegrationTests {

    @Autowired
    NamedParameterJdbcTemplate template;
    @Autowired
    DummyEntityRepository repository;
    @Autowired
    RelatedEntityRepository relatedEntityRepository;

    @Test
    public void getEntityByNestedProperty() {
        DummyEntity entity = repository.save(createEntity());
    }

    private DummyEntity createEntity() {
        RelatedEntity relatedEntity = relatedEntityRepository.save(new RelatedEntity("some text"));
        return repository.save(new DummyEntity(relatedEntity));
    }

    interface DummyEntityRepository extends CrudRepository<DummyEntity, Long> {
        List<DummyEntity> findByRelatedEntityContent(String content);
    }

    interface RelatedEntityRepository extends CrudRepository<RelatedEntity, Long> {
    }

    @Configuration
    @Import(TestConfiguration.class)
    static class Config {

        @Bean
        DummyEntityRepository dummyEntityRepository(JdbcRepositoryFactory factory) {
            return factory.getRepository(DummyEntityRepository.class);
        }

        @Bean
        RelatedEntityRepository relatedEntityRepository(JdbcRepositoryFactory factory) {
            return factory.getRepository(RelatedEntityRepository.class);
        }
    }

    static class DummyEntity {
        @Id
        Long id;
        RelatedEntity relatedEntity;

        public DummyEntity(RelatedEntity relatedEntity) {
            this.relatedEntity = relatedEntity;
        }
    }

    static class RelatedEntity {
        @Id
        Long id;
        String content;

        public RelatedEntity(String content) {
            this.content = content;
        }
    }
}
