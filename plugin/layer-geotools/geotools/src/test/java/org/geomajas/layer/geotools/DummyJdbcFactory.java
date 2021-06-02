package org.geomajas.layer.geotools;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.opengis.feature.type.Name;

public class DummyJdbcFactory extends ContentDataStore implements org.geotools.data.DataStoreFactorySpi {

	public class DummyJdbcDataStore extends ContentDataStore {
		@Override
        protected List<Name> createTypeNames() throws IOException {
			return null;
		}
		@Override
        protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
			return null;
		}
	}

	@Override
    public String getDescription() {
		return "DummyJdbcFactory";
	}

	@Override
    public Param[] getParametersInfo() {
		return new Param[] { new Param("testScope", Boolean.class, "Set to true for unit testing", true) };
	}

	@Override
    public boolean canProcess(Map params) {
	    if (!DataUtilities.canProcess(params, getParametersInfo())) {
            return false;
        }
		if (!(((String) params.get("testScope")).equalsIgnoreCase("true"))) {
			return (false);
		} else {
			return (true);
		}
	}

	@Override
    public DataStore createDataStore(Map<String, ?> params) throws IOException {
		return new DummyJdbcDataStore();
	}

	@Override
    public DataStore createNewDataStore(Map<String, ?> params) throws IOException {
		return new DummyJdbcDataStore();
	}

    @Override
    public String getDisplayName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAvailable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected ContentFeatureSource createFeatureSource(ContentEntry arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<Name> createTypeNames() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
}
