/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.data.relational.core.sqlgeneration;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.SelectItem;

/**
 * A {@link SelectItemPattern} that matches a specific type of expression
 *
 * @param <T> the type of the expression that is matched by this pattern.
 */
abstract class TypedExpressionPattern<T> implements SelectItemPattern, ExpressionPattern {

	private final Class<T> type;

	TypedExpressionPattern(Class<T> type) {

		this.type = type;
	}

	@Override
	public boolean matches(SelectItem selectItem) {

		Expression expression = selectItem.getExpression();
		return matches(expression);
	}

	@Override
	public boolean matches(Expression expression) {

		if (type.isInstance(expression)) {
			return matches((T) expression);
		}
		return false;
	}

	abstract boolean matches(T expression);
}
