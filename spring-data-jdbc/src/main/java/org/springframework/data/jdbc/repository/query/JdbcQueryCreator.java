/*
 * Copyright 2020-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.repository.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.QueryMapper;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.AggregatePath;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder;
import org.springframework.data.relational.core.sql.StatementBuilder;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.relational.repository.query.RelationalEntityMetadata;
import org.springframework.data.relational.repository.query.RelationalParameterAccessor;
import org.springframework.data.relational.repository.query.RelationalQueryCreator;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of {@link RelationalQueryCreator} that creates {@link ParametrizedQuery} from a {@link PartTree}.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Myeonghyeon Lee
 * @author Diego Krupitza
 * @since 2.0
 */
class JdbcQueryCreator extends RelationalQueryCreator<ParametrizedQuery> {

	private final RelationalMappingContext context;
	private final PartTree tree;
	private final RelationalParameterAccessor accessor;
	private final QueryMapper queryMapper;
	private final RelationalEntityMetadata<?> entityMetadata;
	private final RenderContextFactory renderContextFactory;
	private final boolean isSliceQuery;
	private final ReturnedType returnedType;
	private final Optional<Lock> lockMode;

	/**
	 * Creates new instance of this class with the given {@link PartTree}, {@link JdbcConverter}, {@link Dialect},
	 * {@link RelationalEntityMetadata} and {@link RelationalParameterAccessor}.
	 *
	 * @param context the mapping context. Must not be {@literal null}.
	 * @param tree part tree, must not be {@literal null}.
	 * @param converter must not be {@literal null}.
	 * @param dialect must not be {@literal null}.
	 * @param entityMetadata relational entity metadata, must not be {@literal null}.
	 * @param accessor parameter metadata provider, must not be {@literal null}.
	 * @param isSliceQuery flag denoting if the query returns a {@link org.springframework.data.domain.Slice}.
	 * @param returnedType the {@link ReturnedType} to be returned by the query. Must not be {@literal null}.
	 */
	JdbcQueryCreator(RelationalMappingContext context, PartTree tree, JdbcConverter converter, Dialect dialect,
			RelationalEntityMetadata<?> entityMetadata, RelationalParameterAccessor accessor, boolean isSliceQuery,
			ReturnedType returnedType, Optional<Lock> lockMode) {
		super(tree, accessor);

		Assert.notNull(converter, "JdbcConverter must not be null");
		Assert.notNull(dialect, "Dialect must not be null");
		Assert.notNull(entityMetadata, "Relational entity metadata must not be null");
		Assert.notNull(returnedType, "ReturnedType must not be null");

		this.context = context;
		this.tree = tree;
		this.accessor = accessor;

		this.entityMetadata = entityMetadata;
		this.queryMapper = new QueryMapper(converter);
		this.renderContextFactory = new RenderContextFactory(dialect);
		this.isSliceQuery = isSliceQuery;
		this.returnedType = returnedType;
		this.lockMode = lockMode;
	}

	/**
	 * Validate parameters for the derived query. Specifically checking that the query method defines scalar parameters
	 * and collection parameters where required and that invalid parameter declarations are rejected.
	 *
	 * @param tree the tree structure defining the predicate of the query.
	 * @param parameters parameters for the predicate.
	 */
	static void validate(PartTree tree, Parameters<?, ?> parameters, RelationalMappingContext context) {
		RelationalQueryCreator.validate(tree, parameters);
	}

	/**
	 * Creates {@link ParametrizedQuery} applying the given {@link Criteria} and {@link Sort} definition.
	 *
	 * @param criteria {@link Criteria} to be applied to query
	 * @param sort sort option to be applied to query, must not be {@literal null}.
	 * @return instance of {@link ParametrizedQuery}
	 */
	@Override
	protected ParametrizedQuery complete(@Nullable Criteria criteria, Sort sort) {

		RelationalPersistentEntity<?> entity = entityMetadata.getTableEntity();
		Table table = Table.create(entityMetadata.getTableName());
		MapSqlParameterSource parameterSource = new MapSqlParameterSource();

		SelectBuilder.SelectLimitOffset limitOffsetBuilder = createSelectClause(entity, table);
        SelectBuilder.SelectJoin joinBuilder = applyLimitAndOffset(limitOffsetBuilder);
        SelectBuilder.SelectOrdered selectOrderBuilder = applyCriteria(criteria, entity, table, parameterSource, joinBuilder);

        selectOrderBuilder = applyOrderBy(sort, entity, table, selectOrderBuilder);

		SelectBuilder.BuildSelect completedBuildSelect = selectOrderBuilder;
		if (this.lockMode.isPresent()) {
			completedBuildSelect = selectOrderBuilder.lock(this.lockMode.get().value());
		}

		Select select = completedBuildSelect.build();

		String sql = SqlRenderer.create(renderContextFactory.createRenderContext()).render(select);
		return new ParametrizedQuery(sql, parameterSource);
	}

	SelectBuilder.SelectOrdered applyCriteria(@Nullable Criteria criteria,
											  RelationalPersistentEntity<?> rootEntity,
											  Table rootTable,
											  MapSqlParameterSource parameterSource,
											  SelectBuilder.SelectJoin joinBuilder) {
		if (criteria == null) {
			return ((SelectBuilder.SelectWhere) joinBuilder);
		}

		// Extract property path (e.g. ["intermediateEntities", "relatedEntities", "content"])
		String[] propertySegments = extractPropertyPath(criteria);

		// Join only on entities (i.e., all segments except the last one)
		String[] pathToLastEntity = excludeLastSegment(propertySegments);

		// The final property name (e.g., "content")
		String lastProperty = propertySegments[propertySegments.length - 1];

		JoinContext joinContext = applyJoins(pathToLastEntity, rootEntity, rootTable, joinBuilder);

		// Build new Criteria using only the actual field name (no path)
		Criteria adjustedCriteria = Criteria.where(lastProperty).is(criteria.getValue());

		return ((SelectBuilder.SelectWhere) joinContext.joinBuilder).where(
				queryMapper.getMappedObject(parameterSource, adjustedCriteria, joinContext.table, joinContext.entity)
		);
	}

	private String[] extractPropertyPath(Criteria criteria) {
		return criteria.getColumn().getReference().split("\\.");
	}

	private String[] excludeLastSegment(String[] segments) {
		if (segments.length == 0) return new String[0];
		return java.util.Arrays.copyOf(segments, segments.length - 1);
	}

	private JoinContext applyJoins(String[] path,
								   RelationalPersistentEntity<?> rootEntity,
								   Table rootTable,
								   SelectBuilder.SelectJoin joinBuilder) {

		RelationalPersistentEntity<?> currentEntity = rootEntity;
		Table currentTable = rootTable;
		SelectBuilder.SelectJoin currentJoinBuilder = joinBuilder;

		for (String nestedPropertyName : path) {
			RelationalPersistentProperty nestedProperty = currentEntity.getPersistentProperty(nestedPropertyName);
			if (nestedProperty == null) {
				throw new IllegalArgumentException("No property '" + nestedPropertyName + "' found in " + currentEntity.getName());
			}

			Class<?> nestedType = nestedProperty.getActualType();
			RelationalPersistentEntity<?> nestedEntity = context.getRequiredPersistentEntity(nestedType);
			Table relatedTable = Table.create(nestedEntity.getTableName());

			String fkColumn = Optional.ofNullable(nestedProperty.findAnnotation(MappedCollection.class))
					.map(MappedCollection::idColumn)
					.orElseThrow(() -> new IllegalStateException(
							"Property '" + nestedPropertyName + "' is not annotated with @MappedCollection"
					));

			currentJoinBuilder = currentJoinBuilder
					.join(relatedTable)
					.on(relatedTable.column(fkColumn))
					.equals(currentTable.column(currentEntity.getIdColumn()));

			currentEntity = nestedEntity;
			currentTable = relatedTable;
		}

		return new JoinContext(currentJoinBuilder, currentEntity, currentTable);
	}

	private record JoinContext(
			SelectBuilder.SelectJoin joinBuilder,
			RelationalPersistentEntity<?> entity,
							   Table table
	) {
	}

	SelectBuilder.SelectOrdered applyOrderBy(Sort sort, RelationalPersistentEntity<?> entity, Table table,
			SelectBuilder.SelectOrdered selectOrdered) {

		return sort.isSorted() ? //
				selectOrdered.orderBy(queryMapper.getMappedSort(table, sort, entity)) //
				: selectOrdered;
	}

    SelectBuilder.SelectJoin applyLimitAndOffset(SelectBuilder.SelectLimitOffset limitOffsetBuilder) {

		if (tree.isExistsProjection()) {
			limitOffsetBuilder = limitOffsetBuilder.limit(1);
		} else if (tree.isLimiting()) {
			limitOffsetBuilder = limitOffsetBuilder.limit(tree.getMaxResults());
		}

		Pageable pageable = accessor.getPageable();
		if (pageable.isPaged()) {
			limitOffsetBuilder = limitOffsetBuilder.limit(isSliceQuery ? pageable.getPageSize() + 1 : pageable.getPageSize())
					.offset(pageable.getOffset());
		}

        return (SelectBuilder.SelectJoin) limitOffsetBuilder;
	}

	SelectBuilder.SelectLimitOffset createSelectClause(RelationalPersistentEntity<?> entity, Table table) {

		SelectBuilder.SelectJoin builder;
		if (tree.isExistsProjection()) {

			Column idColumn = table.column(entity.getIdColumn());
			builder = Select.builder().select(idColumn).from(table);
		} else if (tree.isCountProjection()) {
			builder = Select.builder().select(Functions.count(Expressions.asterisk())).from(table);
		} else {
			builder = selectBuilder(table);
		}

		return (SelectBuilder.SelectLimitOffset) builder;
	}

	private SelectBuilder.SelectJoin selectBuilder(Table table) {

		List<Expression> columnExpressions = new ArrayList<>();
		RelationalPersistentEntity<?> entity = entityMetadata.getTableEntity();
		SqlContext sqlContext = new SqlContext(entity);

		List<Join> joinTables = new ArrayList<>();
		for (PersistentPropertyPath<RelationalPersistentProperty> path : context
				.findPersistentPropertyPaths(entity.getType(), p -> true)) {

			AggregatePath aggregatePath = context.getAggregatePath(path);

			if (returnedType.needsCustomConstruction()) {
				if (!returnedType.getInputProperties().contains(aggregatePath.getRequiredBaseProperty().getName())) {
					continue;
				}
			}

			// add a join if necessary
			Join join = getJoin(sqlContext, aggregatePath);
			if (join != null) {
				joinTables.add(join);
			}

			Column column = getColumn(sqlContext, aggregatePath);
			if (column != null) {
				columnExpressions.add(column);
			}
		}

		SelectBuilder.SelectAndFrom selectBuilder = StatementBuilder.select(columnExpressions);
		SelectBuilder.SelectJoin baseSelect = selectBuilder.from(table);

		for (Join join : joinTables) {
			baseSelect = baseSelect.leftOuterJoin(join.joinTable).on(join.joinColumn).equals(join.parentId);
		}

		return baseSelect;
	}

	/**
	 * Create a {@link Column} for {@link AggregatePath}.
	 *
	 * @param sqlContext
	 * @param path the path to the column in question.
	 * @return the statement as a {@link String}. Guaranteed to be not {@literal null}.
	 */
	@Nullable
	private Column getColumn(SqlContext sqlContext, AggregatePath path) {

		// an embedded itself doesn't give an column, its members will though.
		// if there is a collection or map on the path it won't get selected at all, but it will get loaded with a separate
		// select
		// only the parent path is considered in order to handle arrays that get stored as BINARY properly
		if (path.isEmbedded() || path.getParentPath().isMultiValued()) {
			return null;
		}

		if (path.isEntity()) {

			// Simple entities without id include there backreference as an synthetic id in order to distinguish null entities
			// from entities with only null values.

			if (path.isQualified() //
					|| path.isCollectionLike() //
					|| path.hasIdProperty() //
			) {
				return null;
			}

			return sqlContext.getReverseColumn(path);
		}

		return sqlContext.getColumn(path);
	}

	@Nullable
	Join getJoin(SqlContext sqlContext, AggregatePath path) {

		if (!path.isEntity() || path.isEmbedded() || path.isMultiValued()) {
			return null;
		}

		Table currentTable = sqlContext.getTable(path);

		AggregatePath idDefiningParentPath = path.getIdDefiningParentPath();
		Table parentTable = sqlContext.getTable(idDefiningParentPath);

		return new Join( //
				currentTable, //
				currentTable.column(path.getTableInfo().reverseColumnInfo().name()), //
				parentTable.column(idDefiningParentPath.getTableInfo().idColumnName()) //
		);
	}

	/**
	 * Value object representing a {@code JOIN} association.
	 */
	static private final class Join {

		private final Table joinTable;
		private final Column joinColumn;
		private final Column parentId;

		Join(Table joinTable, Column joinColumn, Column parentId) {

			Assert.notNull(joinTable, "JoinTable must not be null");
			Assert.notNull(joinColumn, "JoinColumn must not be null");
			Assert.notNull(parentId, "ParentId must not be null");

			this.joinTable = joinTable;
			this.joinColumn = joinColumn;
			this.parentId = parentId;
		}

		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Join join = (Join) o;
			return joinTable.equals(join.joinTable) && joinColumn.equals(join.joinColumn) && parentId.equals(join.parentId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(joinTable, joinColumn, parentId);
		}

		@Override
		public String toString() {

			return "Join{" + "joinTable=" + joinTable + ", joinColumn=" + joinColumn + ", parentId=" + parentId + '}';
		}
	}
}
