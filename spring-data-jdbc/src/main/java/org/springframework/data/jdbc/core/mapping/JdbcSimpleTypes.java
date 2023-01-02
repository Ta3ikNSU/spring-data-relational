/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.data.jdbc.core.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Simple constant holder for a {@link SimpleTypeHolder} enriched with specific simple types for relational database
 * access.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 */
public abstract class JdbcSimpleTypes {

	public static final Set<Class<?>> AUTOGENERATED_ID_TYPES;

	static {

		Set<Class<?>> classes = new HashSet<>();
		classes.add(Long.class);
		classes.add(String.class);
		classes.add(BigInteger.class);
		classes.add(BigDecimal.class);
		classes.add(UUID.class);
		AUTOGENERATED_ID_TYPES = Collections.unmodifiableSet(classes);

		Set<Class<?>> simpleTypes = new HashSet<>();
		simpleTypes.add(BigDecimal.class);
		simpleTypes.add(BigInteger.class);
		simpleTypes.add(Array.class);
		simpleTypes.add(Clob.class);
		simpleTypes.add(Blob.class);
		simpleTypes.add(java.sql.Date.class);
		simpleTypes.add(NClob.class);
		simpleTypes.add(Ref.class);
		simpleTypes.add(RowId.class);
		simpleTypes.add(Struct.class);
		simpleTypes.add(Time.class);
		simpleTypes.add(Timestamp.class);
		simpleTypes.add(UUID.class);
		simpleTypes.add(JdbcValue.class);

		JDBC_SIMPLE_TYPES = Collections.unmodifiableSet(simpleTypes);
	}

	private static final Set<Class<?>> JDBC_SIMPLE_TYPES;
	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(JDBC_SIMPLE_TYPES, true);

	private JdbcSimpleTypes() {}
}
