package org.springframework.data.jdbc.repository;

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
    @Autowired
    RelatedEntityRepository relatedEntityRepository;

    @Test
    void getEntityByNestedProperty() {
        // given
        String contentString = "content";
        RelatedEntity related = new RelatedEntity(contentString);

        DummyEntity dummy = new DummyEntity();
        dummy.relatedEntities = new HashSet<>();
        dummy.relatedEntities.add(related);

        DummyEntity saved = repository.save(dummy);

        // when
        relatedEntityRepository.findAll();
        List<DummyEntity> actual = repository.findByRelatedEntitiesContent(contentString);

        // then
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).id).isEqualTo(saved.id);
        assertThat(actual.get(0).relatedEntities)
                .extracting(e -> e.content)
                .containsExactly(contentString);
    }

    interface DummyEntityRepository extends CrudRepository<DummyEntity, Long> {
        List<DummyEntity> findByRelatedEntitiesContent(String content);
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

        // Указываем имя колонки, которая соответствует внешнему ключу в таблице related_entity.
        @MappedCollection(idColumn = "dummy_entity_id")
        Set<RelatedEntity> relatedEntities;

        public DummyEntity() {
        }  // Конструктор без аргументов обязателен

        public DummyEntity(RelatedEntity relatedEntity) {
            this.relatedEntities = Set.of(relatedEntity);
        }
    }

    static class RelatedEntity {
        @Id
        Long id;
        String content;

        public RelatedEntity() {
        } // Конструктор без аргументов для Spring Data JDBC

        public RelatedEntity(String content) {
            this.content = content;
        }
    }
}
