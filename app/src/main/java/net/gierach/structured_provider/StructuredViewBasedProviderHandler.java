
package net.gierach.structured_provider;

import android.net.Uri;

public abstract class StructuredViewBasedProviderHandler extends ViewBasedProviderHandler {

    public interface SelectionField {

    }

    public static class SimpleSelectionField implements SelectionField {

        public final String functionName;
        public final boolean distinct;
        public final String qualifier;
        public final String fieldName;
        public final String alias;

        public SimpleSelectionField(String qualifier, String fieldName) {
            this(qualifier, fieldName, null);
        }

        public SimpleSelectionField(String qualifier, String fieldName, String alias) {
            this.fieldName = fieldName;
            this.alias = alias;
            this.functionName = null;
            this.qualifier = qualifier;
            this.distinct = false;
        }

        public SimpleSelectionField(String qualifier, String fieldName, String alias, String functionName, boolean distinct) {
            this.fieldName = fieldName;
            this.alias = alias;
            this.functionName = functionName;
            this.qualifier = qualifier;
            this.distinct = distinct;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if (this.functionName != null) {
                sb.append(this.functionName).append('(');
            }
            if (this.distinct) {
                sb.append("DISTINCT ");
            }
            if (this.qualifier != null) {
                sb.append(this.qualifier).append('.');
            }
            sb.append(this.fieldName);
            if (this.functionName != null) {
                sb.append(')');
            }
            if (this.alias != null) {
                sb.append(" AS ").append(this.alias);
            }

            return sb.toString();
        }
    }

    public static class CoalesceField implements SelectionField {

        final String mStringValue;

        public CoalesceField(String alias, SelectionField... fields) {
            StringBuilder sb = new StringBuilder("COALESCE(");
            for (int i = 0; i < fields.length; ++i) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(fields[i]);
            }

            sb.append(')');
            if (alias != null) {
                sb.append(" AS ").append(alias);
            }

            this.mStringValue = sb.toString();
        }

        @Override
        public String toString() {
            return this.mStringValue;
        }
    }

    protected StructuredViewBasedProviderHandler(String viewName, String contentType, String entryContentType, Uri contentUri) {
        super(viewName, contentType, entryContentType, contentUri);

    }

    protected abstract SelectionField[] getSelectionFields(int version);

    protected abstract String getFromClause(int version);

    protected String getWhereClause(int version) {
        return null;
    }

    protected String getGroupByClause(int version) {
        return null;
    }

    protected String getHavingClause(int version) {
        return null;
    }

    @Override
    public String createViewSQL(int version) {
        SelectionField[] fields = getSelectionFields(version);
        StringBuilder sb = new StringBuilder(256);

        sb.append("CREATE VIEW ").append(getName()).append(" AS SELECT ");
        for (int i = 0; i < fields.length; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(fields[i]);
        }

        sb.append(" FROM ");
        sb.append(getFromClause(version));
        String where = getWhereClause(version);
        if (where != null) {
            sb.append(" WHERE ").append(where);
        }
        String groupBy = getGroupByClause(version);
        if (groupBy != null) {
            sb.append(" GROUP BY ").append(groupBy);
            String having = getHavingClause(version);
            if (having != null) {
                sb.append(" HAVING ").append(having);
            }

        }

        sb.append(';');

        return sb.toString();
    }

    protected static String[] substituteProjectionFieldDefinitions(String[] projection, String[] fieldNames, String[] fieldDefs) {
        if (projection == null) {
            projection = new String[fieldDefs.length + 1];
            projection[0] = "*";
            System.arraycopy(fieldDefs, 0, projection, 1, fieldDefs.length);
        } else {
            projection = projection.clone();

            boolean[] foundFields = new boolean[fieldNames.length];
            int fieldsToAdd = 0;

            for (int i = 0; i < fieldNames.length; ++i) {
                int foundIndex = indexInArray(projection, fieldNames[i]);
                if (foundIndex >= 0) {
                    projection[foundIndex] = fieldDefs[i];
                    foundFields[i] = true;
                } else {
                    fieldsToAdd++;
                }
            }

            if (indexInArray(projection, "*") >= 0 && fieldsToAdd > 0) {
                String[] newProjection = new String[projection.length + fieldsToAdd];

                System.arraycopy(projection, 0, newProjection, 0, projection.length);

                int writePos = projection.length;

                for (int i = 0; i < foundFields.length; ++i) {
                    if (!foundFields[i]) {
                        newProjection[writePos++] = fieldDefs[i];
                    }
                }

                projection = newProjection;
            }
        }


        return projection;
    }

    private static int indexInArray(String[] array, String string) {
        int result = -1;

        if (array != null) {
            int len = array.length;
            for (int i = 0; i < len; ++i) {
                if (array[i] != null && array[i].equals(string)) {
                    result = i;
                    break;
                }
            }
        }

        return result;
    }
}
