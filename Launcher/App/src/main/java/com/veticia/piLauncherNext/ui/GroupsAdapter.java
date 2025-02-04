package com.veticia.piLauncherNext.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.veticia.piLauncherNext.MainActivity;
import com.veticia.piLauncherNext.R;
import com.veticia.piLauncherNext.SettingsProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupsAdapter extends BaseAdapter
{
    public static final int MAX_GROUPS = 12;
    public static final String HIDDEN_GROUP = "HIDDEN!";

    private final MainActivity mActivity;
    private final List<String> mItems;
    private final Set<String> mSelection;
    private final SettingsProvider mSettings;
    private final boolean mEditMode;

    /** Create new adapter */
    public GroupsAdapter(MainActivity activity, boolean editMode)
    {
        mActivity = activity;
        mEditMode = editMode;
        mSettings = SettingsProvider.getInstance(activity);

        SettingsProvider settings = SettingsProvider.getInstance(mActivity);
        mItems = settings.getAppGroupsSorted(false);
        if (editMode) {
            mItems.add(HIDDEN_GROUP);
            mItems.add("+ " + mActivity.getString(R.string.add_group));
        }
        mSelection = settings.getSelectedGroups();
    }

    public int getCount()
    {
        return mItems.size();
    }

    public String getItem(int position)
    {
        return mItems.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.lv_group, parent, false);

        if (position >= MAX_GROUPS - 1) {
            itemView.setVisibility(View.GONE);
        }

        // set menu action
        View menu = itemView.findViewById(R.id.menu);
        menu.setOnClickListener(view -> {

            final Map<String, String> apps = mSettings.getAppList();
            final Set<String> groups = mSettings.getAppGroups();
            final String oldName = mSettings.getAppGroupsSorted(false).get(position);

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setView(R.layout.dialog_group_details);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
            dialog.show();

            final EditText input = dialog.findViewById(R.id.group_name);
            input.setText(oldName);

            dialog.findViewById(R.id.ok).setOnClickListener(view1 -> {
                String newName = input.getText().toString();
                if (newName.length() > 0) {
                    groups.remove(oldName);
                    groups.add(newName);
                    Map<String, String> updatedApps = new HashMap<>();
                    for (String pkg : apps.keySet()) {
                        if (apps.get(pkg).compareTo(oldName) == 0) {
                            updatedApps.put(pkg, newName);
                        } else {
                            updatedApps.put(pkg, apps.get(pkg));
                        }
                    }
                    HashSet<String> selected = new HashSet<>();
                    selected.add(newName);
                    mSettings.setSelectedGroups(selected);
                    mSettings.setAppGroups(groups);
                    mSettings.setAppList(updatedApps);
                    mActivity.reloadUI();
                }
                dialog.dismiss();
            });

            dialog.findViewById(R.id.group_delete).setOnClickListener(view12 -> {
                HashMap<String, String> newApps = new HashMap<>();
                for (String pkg : apps.keySet()) {
                    if (apps.get(pkg).compareTo(oldName) == 0) {
                        newApps.put(pkg, HIDDEN_GROUP);
                    } else {
                        newApps.put(pkg, apps.get(pkg));
                    }
                }
                mSettings.setAppList(newApps);

                groups.remove(oldName);
                mSettings.setAppGroups(groups);

                Set<String> selectFirst = new HashSet<>();
                selectFirst.add(mSettings.getAppGroupsSorted(false).get(0));
                mSettings.setSelectedGroups(selectFirst);
                mActivity.reloadUI();
                dialog.dismiss();
            });
        });

        // set drag and drop
        itemView.setOnDragListener((view, event) -> {
            if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
                int[] colors = new int[] {Color.argb(192, 128, 128, 255), Color.TRANSPARENT};
                GradientDrawable.Orientation orientation = GradientDrawable.Orientation.LEFT_RIGHT;
                itemView.setBackground(new GradientDrawable(orientation, colors));
            } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {
                setLook(position, view, menu);
            } else if (event.getAction() == DragEvent.ACTION_DROP) {
                // add group or hidden group selection
                String name = mItems.get(position);
                List<String> groups = mSettings.getAppGroupsSorted(false);
                if (groups.size() + 1 == position) {
                    name = mSettings.addGroup();
                } else if (groups.size() == position) {
                    name = HIDDEN_GROUP;
                }

                // move app into group
                String pkg = mActivity.getSelectedPackage();
                Set<String> selected = mSettings.getSelectedGroups();
                Map<String, String> apps = mSettings.getAppList();
                apps.remove(pkg);
                apps.put(pkg, name);
                mSettings.setAppList(apps);

                // false to dragged icon fly back
                return !selected.contains(name);
            }
            return true;
        });

        // set value into textview
        TextView textView = itemView.findViewById(R.id.textLabel);
        if (mItems.get(position).compareTo(HIDDEN_GROUP) == 0) {
            String hiddenText = " -  " + mActivity.getString(R.string.apps_hidden);
            textView.setText(hiddenText);
        } else {
            textView.setText(mItems.get(position));
        }

        // set the look
        setLook(position, itemView, menu);

        return itemView;
    }

    private void setLook(int position, View itemView, View menu) {
        View filler = mActivity.findViewById(R.id.filler);
        View topBar = mActivity.findViewById(R.id.topBar);
        View pi = mActivity.findViewById(R.id.pi);
        View update = mActivity.findViewById(R.id.update);

        int gap = (topBar.getWidth()-pi.getWidth()-(update!=null?update.getWidth():0))%getCount();
        filler.setMinimumWidth(gap);
        if (mSelection.contains(mItems.get(position))) {
            int[] colors = new int[] {Color.argb(192, 255, 255, 255), Color.TRANSPARENT};
            GradientDrawable.Orientation orientation = GradientDrawable.Orientation.TOP_BOTTOM;
            itemView.setBackground(new GradientDrawable(orientation, colors));
            if(position==getCount()-1){
                filler.setBackground(new GradientDrawable(orientation, colors));
            }
            if (mEditMode && (position < getCount() - 2)) {
                menu.setVisibility(View.VISIBLE);
            } else {
                menu.setVisibility(View.GONE);
            }
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
            menu.setVisibility(View.GONE);
            if(position==getCount()-1){
                filler.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
}
