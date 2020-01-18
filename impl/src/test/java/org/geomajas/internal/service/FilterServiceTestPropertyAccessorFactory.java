package org.geomajas.internal.service;

import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.util.factory.Hints;

/**
 * PropertyAccessorFactory for testing. Works for all features that implement TestPropertyAccess.
 *
 * @author Jan De Moerloose
 *
 */
public class FilterServiceTestPropertyAccessorFactory implements PropertyAccessorFactory {

	public class FilterServicePropertyAccessor implements PropertyAccessor {

		@Override
        public boolean canHandle(Object object, String xpath, Class target) {
			return object instanceof TestPropertyAccess;
		}

		@Override
        public Object get(Object object, String xpath, Class target) throws IllegalArgumentException {
			return ((TestPropertyAccess) object).get(xpath, target);
		}

		@Override
        public void set(Object object, String xpath, Object value, Class target) throws IllegalArgumentException {
		}

	}

	@Override
    public PropertyAccessor createPropertyAccessor(Class type, String xpath, Class target, Hints hints) {
		return new FilterServicePropertyAccessor();
	}

}
