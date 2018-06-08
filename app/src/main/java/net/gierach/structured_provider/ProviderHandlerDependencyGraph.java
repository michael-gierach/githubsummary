package net.gierach.structured_provider;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class ProviderHandlerDependencyGraph {

    private static final String TAG = "DependencyGraph";

    ArrayList<TableBasedProviderHandler> mTables = new ArrayList<>();
    HashMap<String, ArrayList<String>> mTableGraph = new HashMap<>();
    HashMap<String, ViewBasedProviderHandler> mViews = new HashMap<>();
    HashMap<String, ArrayList<String>> mViewGraph = new HashMap<>();

    private boolean mDebugPrintSql = false;

    public void addTableHandler(TableBasedProviderHandler table) {

        if (!this.mTables.contains(table)) {
            this.mTables.add(table);
        }
    }

    public void addViewHandler(ViewBasedProviderHandler view) {
        String viewName = view.getName();

        if (!this.mViews.containsKey(viewName)) {
            this.mViews.put(viewName, view);

            String[] tableNames = view.getDependentTables();
            if (tableNames != null) {
                for (String table : tableNames) {
                    ArrayList<String> list = this.mTableGraph.get(table);
                    if (list == null) {
                        list = new ArrayList<>();
                        this.mTableGraph.put(table, list);
                    }
                    if (!list.contains(viewName)) {
                        list.add(viewName);
                    }
                }
            }

            String[] viewNames = view.getDependentViews();
            if (viewNames != null) {
                for (String dependView : viewNames) {
                    ArrayList<String> list = this.mViewGraph.get(dependView);
                    if (list == null) {
                        list = new ArrayList<>();
                        this.mViewGraph.put(dependView, list);
                    }
                    if (!list.contains(viewName)) {
                        list.add(viewName);
                    }
                }
            }
        }
    }

    public void markTableChanged(String tableName, ArrayList<String> viewChangeList) {
        ArrayList<String> dependents = this.mTableGraph.get(tableName);
        if (dependents != null) {
            for (int i = 0; i < dependents.size(); ++i) {
                markViewChanged(dependents.get(i), viewChangeList);
            }
        }
    }

    public int markViewChanged(String viewName, ArrayList<String> viewChangeList) {
        int pos = viewChangeList.indexOf(viewName);
        if (pos < 0) {
            pos = Integer.MAX_VALUE;

            ArrayList<String> dependents = this.mViewGraph.get(viewName);
            if (dependents != null) {
                for (int i = 0; i < dependents.size(); ++i) {
                    int tempPos = markViewChanged(dependents.get(i), viewChangeList);
                    pos = Math.min(pos, tempPos);
                }
            }

            pos = Math.min(viewChangeList.size(), pos);

            viewChangeList.add(pos, viewName);
        }

        return pos;
    }

    public void onCreate(SQLiteDatabase db, int newVersion) throws SQLiteException {
        onUpgrade(db, 0, newVersion);
    }

    public void resetDatabase(SQLiteDatabase db) throws SQLiteException {
        ArrayList<String> viewChangeList = new ArrayList<>();
        for (ViewBasedProviderHandler view : this.mViews.values()) {
            markViewChanged(view.getName(), viewChangeList);
        }
        for (int i = viewChangeList.size() - 1; i >= 0; --i) {
            executeAndLogSQL(db, "DROP VIEW IF EXISTS " + viewChangeList.get(i) + ';');
        }

        for (int i = this.mTables.size() - 1; i >= 0; --i) {
            executeAndLogSQL(db, "DROP TABLE IF EXISTS " + this.mTables.get(i).getName() + ';');
        }

    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) throws SQLiteException {
        ArrayList<String> viewChangeList = new ArrayList<>();

        for (int i = 0; i < this.mTables.size(); ++i) {
            TableBasedProviderHandler table = this.mTables.get(i);

            String[] upgradeCommands = table.getUpgradeSQLCommands(oldVersion, newVersion);
            if (upgradeCommands != null && upgradeCommands.length > 0) {
                markTableChanged(table.getName(), viewChangeList);

                for (String command : upgradeCommands) {
                    executeAndLogSQL(db, command);
                }
            }
        }

        for (int i = 0; i < this.mTables.size(); ++i) {
            TableBasedProviderHandler table = this.mTables.get(i);

            String[] indexCommands = table.getUpgradeIndexCommands(oldVersion, newVersion);
            if (indexCommands != null) {
                for (String command : indexCommands) {
                    executeAndLogSQL(db, command);
                }
            }
        }

        for (int i = 0; i < this.mTables.size(); ++i) {
            TableBasedProviderHandler table = this.mTables.get(i);

            String[] triggerCommands = table.getUpgradeTriggerCommands(oldVersion, newVersion);
            if (triggerCommands != null) {
                for (String command : triggerCommands) {
                    executeAndLogSQL(db, command);
                }
            }
        }

        if (oldVersion > 0) {
            for (int i = 0; i < this.mTables.size(); ++i) {
                TableBasedProviderHandler table = this.mTables.get(i);

                String[] dataCommands = table.getUpgradeDataCommands(oldVersion, newVersion, db);
                if (dataCommands != null) {
                    for (String command : dataCommands) {
                        executeAndLogSQL(db, command);
                    }
                }
            }
        }

        for (ViewBasedProviderHandler view : this.mViews.values()) {
            if (view.hasChangesOnUpgrade(oldVersion, newVersion)) {
                markViewChanged(view.getName(), viewChangeList);
            }
        }

        if (viewChangeList.size() > 0) {
            ViewBasedProviderHandler[] viewList = new ViewBasedProviderHandler[viewChangeList.size()];
            for (int i = viewList.length - 1; i >= 0; --i) {
                executeAndLogSQL(db, "DROP VIEW IF EXISTS " + viewChangeList.get(i) + ';');
                viewList[i] = this.mViews.get(viewChangeList.get(i));
            }

            for (ViewBasedProviderHandler view : viewList) {
                executeAndLogSQL(db, view.createViewSQL(newVersion));
            }

        }

    }

    public ViewBasedProviderHandler getViewHandler(String viewName) {
        return this.mViews.get(viewName);
    }

    public TableBasedProviderHandler getTableHandler(String tableName) {
        TableBasedProviderHandler result = null;
        for (TableBasedProviderHandler table : this.mTables) {
            if (table.getName().equals(tableName)) {
                result = table;
                break;
            }
        }

        return result;
    }

    public void setDebugPrintSql(boolean debugPrintSql) {
        this.mDebugPrintSql = debugPrintSql;
    }

    public void executeAndLogSQL(SQLiteDatabase db, String command) throws SQLiteException {
        if (this.mDebugPrintSql) {
            Log.i(TAG, command);
        }
        db.execSQL(command);
    }
}
