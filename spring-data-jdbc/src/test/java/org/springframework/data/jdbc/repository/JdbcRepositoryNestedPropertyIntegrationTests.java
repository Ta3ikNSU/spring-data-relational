package org.springframework.data.jdbc.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory;
import org.springframework.data.jdbc.testing.DatabaseType;
import org.springframework.data.jdbc.testing.EnabledOnDatabase;
import org.springframework.data.jdbc.testing.IntegrationTest;
import org.springframework.data.jdbc.testing.TestConfiguration;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.repository.CrudRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for verifying querying by nested properties in Spring Data JDBC repositories.
 *
 * @author Dmitriy Korolyov
 */
@IntegrationTest
@EnabledOnDatabase(DatabaseType.POSTGRES)
public class JdbcRepositoryNestedPropertyIntegrationTests {
    @Autowired
    DummyEntityRepository repository;

    @Test
    void getEntityByNestedProperty() {
        // given
        String contentString = "content";
        IntermediateEntity related = new IntermediateEntity(contentString);

        DummyEntity dummy = new DummyEntity();
        dummy.intermediateEntities = new HashSet<>();
        dummy.intermediateEntities.add(related);

        DummyEntity saved = repository.save(dummy);

        // when
        List<DummyEntity> actual = repository.findByIntermediateEntitiesContent(contentString);

        // then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).id).isEqualTo(saved.id);
        assertThat(actual.get(0).intermediateEntities)
                .extracting(e -> e.content)
                .containsExactly(contentString);
    }

    @Test
    void getEntityByDoubleNestedProperty() {
        // given
        String contentString = "content";
        String intermediateContent = "intermediateContent";
        String relatedContent = "relatedContent";

        // Create RelatedEntity with content
        RelatedEntity related = new RelatedEntity(relatedContent);

        // Create IntermediateEntity with the nested RelatedEntity
        IntermediateEntity intermediate = new IntermediateEntity(intermediateContent, related);

        // Create DummyEntity with the nested IntermediateEntity
        DummyEntity dummy = new DummyEntity();
        dummy.intermediateEntities = new HashSet<>();
        dummy.intermediateEntities.add(intermediate);

        // Save DummyEntity
        DummyEntity saved = repository.save(dummy);

        // when
        List<DummyEntity> actual = repository.findByIntermediateEntitiesRelatedEntitiesContent(relatedContent);

        // then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).id).isEqualTo(saved.id);
        assertThat(actual.get(0).intermediateEntities)
                .extracting(e -> e.relatedEntities)  // Extract relatedEntities
                .flatExtracting(relatedEntities -> relatedEntities)  // Flatten the Set
                .extracting(e -> e.content)  // Extract content of RelatedEntity
                .containsExactly(relatedContent);  // Check if the content matches
    }


    interface DummyEntityRepository extends CrudRepository<DummyEntity, Long> {
        List<DummyEntity> findByIntermediateEntitiesContent(String content);

        List<DummyEntity> findByIntermediateEntitiesRelatedEntitiesContent(String content);
    }

    @Configuration
    @Import(TestConfiguration.class)
    static class Config {

        @Bean
        DummyEntityRepository dummyEntityRepository(JdbcRepositoryFactory factory) {
            return factory.getRepository(DummyEntityRepository.class);
        }
    }

    static class DummyEntity {
        @Id
        Long id;

        @MappedCollection(idColumn = "dummy_entity_id")
        Set<IntermediateEntity> intermediateEntities;

        public DummyEntity() {
        }

        public DummyEntity(IntermediateEntity intermediateEntities) {
            this.intermediateEntities = Set.of(intermediateEntities);
        }
    }

    static class IntermediateEntity {
        @Id
        Long id;
        String content;

        @MappedCollection(idColumn = "intermediate_entity_id")
        Set<RelatedEntity> relatedEntities;

        public IntermediateEntity(String content, RelatedEntity relatedEntity) {
            this.content = content;
            this.relatedEntities = Set.of(relatedEntity);
        }

        public IntermediateEntity(String content) {
            this.content = content;
            this.relatedEntities = Set.of();
        }

        public IntermediateEntity() {
        }
    }

    static class RelatedEntity {
        @Id
        Long id;
        String content;

        public RelatedEntity() {
        }

        public RelatedEntity(String content) {
            this.content = content;
        }
    }
}
