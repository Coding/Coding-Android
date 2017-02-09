package net.coding.program.project.detail;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;

import net.coding.program.MyApp;
import net.coding.program.R;
import net.coding.program.common.BlankViewDisplay;
import net.coding.program.common.Global;
import net.coding.program.common.ListModify;
import net.coding.program.common.PinyinComparator;
import net.coding.program.common.SaveFragmentPagerAdapter;
import net.coding.program.message.JSONUtils;
import net.coding.program.model.AccountInfo;
import net.coding.program.model.ProjectObject;
import net.coding.program.model.ProjectTaskCountModel;
import net.coding.program.model.ProjectTaskUserCountModel;
import net.coding.program.model.TaskLabelModel;
import net.coding.program.model.TaskObject;
import net.coding.program.model.TaskProjectCountModel;
import net.coding.program.model.UserObject;
import net.coding.program.task.TaskListParentUpdate;
import net.coding.program.task.TaskListUpdate;
import net.coding.program.task.add.TaskAddActivity;
import net.coding.program.task.add.TaskAddActivity_;
import net.coding.program.third.MyPagerSlidingTabStrip;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EFragment(R.layout.fragment_project_task_filter)
@OptionsMenu(R.menu.fragment_project_task)
public class ProjectTaskFragment extends TaskFilterFragment implements TaskListParentUpdate {

    final String HOST_MEMBERS = Global.HOST_API + "/project/%d/members?pageSize=1000";

    @FragmentArg
    ProjectObject mProjectObject;

    @ViewById
    MyPagerSlidingTabStrip tabs;
    @ViewById(R.id.pagerProjectTask)
    ViewPager pager;
    @ViewById
    View blankLayout, actionDivideLine;
    @ViewById
    FloatingActionButton floatButton;

    ArrayList<TaskObject.Members> mUsersInfo = new ArrayList<>();
    ArrayList<TaskObject.Members> mMembersAll = new ArrayList<>();
    ArrayList<TaskObject.Members> mMembersAllAll = new ArrayList<>();
    String HOST_TASK_MEMBER = Global.HOST_API + "/project/%d/task/user/count";
    View.OnClickListener onClickRetry = v -> refresh();

    MemberTaskCount mMemberTask = new MemberTaskCount();
    private MyPagerAdapter adapter;
    private DrawerLayout drawer;
    private UserObject account;

    @AfterViews
    protected final void initProjectTaskFragment() {
        showDialogLoading();

        setActionBarShadow(0);

        tabs.setLayoutInflater(mInflater);
        tabs.setVisibility(View.INVISIBLE);

        account = AccountInfo.loadAccount(getActivity());

        HOST_TASK_MEMBER = String.format(HOST_TASK_MEMBER, mProjectObject.getId());
        refresh();

        adapter = new MyPagerAdapter(getChildFragmentManager());
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                loadData(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // 必须添加，否则回收恢复的时候，TaskListFragment 的 actionmenu 会显示几个出来
        setHasOptionsMenu(true);
        initFilterViews();
    }

    @Override
    protected void initFilterViews() {
        super.initFilterViews();
        toolBarTitle = (TextView) getActivity().findViewById(R.id.toolbarProjectTitle);
        if (toolBarTitle != null) {
            toolBarTitle.setOnClickListener(v -> {
                meActionFilter();
            });
            toolBarTitle.setText("全部任务");
        }


        loadData(0);
    }

    @Override
    protected boolean isProjectInner() {
        return true;
    }

    private void loadData(int index) {

        mTaskProjectCountModel = new TaskProjectCountModel();

        if (index == 0) {
            //全部成员
            //「全部任务」数量 - 进行中，已完成的 「我创建的」数量 = create
            getNetwork(String.format(urlProjectTaskCount, mProjectObject.getId()), urlProjectTaskCount);
            getNetwork(String.format(urlALL_Count, mProjectObject.getId()), urlALL_Count);
            getNetwork(String.format(urlALL_WATCH_Count, mProjectObject.getId(), account.id), urlALL_WATCH_Count);

            loadAllLabels();
        } else {
            TaskObject.Members members = mMembersAll.get(index);

            //某个成员
            getNetwork(String.format(urlSome_Count, mProjectObject.getId(), members.user_id), urlSome_Count);
            getNetwork(String.format(urlSome_Label, mProjectObject.getId(), members.user_id), urlSome_Label);
        }
    }

    private void loadAllLabels() {
        int cur = tabs.getCurrentPosition();
        if (cur != 0) {
            TaskObject.Members members = mMembersAll.get(cur);
            getNetwork(String.format(urlSome_Label, mProjectObject.getId(), members.user_id), urlSome_Label);
        } else {
            if (statusIndex == 0) {
                getNetwork(String.format(urlALL_Label, mProjectObject.owner_user_name, mProjectObject.name), urlALL_Label);
            } else {
                getNetwork(String.format(urlProjectTaskLabels, mProjectObject.getId()) + getRole(), urlProjectTaskLabels);
            }
        }
    }

    private void refresh() {
        getNetwork(HOST_TASK_MEMBER, HOST_TASK_MEMBER);
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(HOST_MEMBERS)) {
            hideDialogLoading();
            if (code == 0) {
                ArrayList<TaskObject.Members> usersInfo = new ArrayList<>();

                JSONArray jsonArray = respanse.getJSONObject("data").getJSONArray("list");

                for (int i = 0; i < jsonArray.length(); ++i) {
                    TaskObject.Members userInfo = new TaskObject.Members(jsonArray.getJSONObject(i));
                    if (mMemberTask.memberHasTask(userInfo.user_id)) { // 只显示有任务的
                        if (userInfo.user.global_key.equals(MyApp.sUserObject.global_key)) {
                            usersInfo.add(0, userInfo);
                        } else {
                            usersInfo.add(userInfo);
                        }
                    }

                    mMembersAllAll.add(userInfo);
                }

                mUsersInfo = usersInfo;
                mMembersAll = new ArrayList<>();
                mMembersAll.add(new TaskObject.Members());
                mMembersAll.addAll(mUsersInfo);

                adapter.notifyDataSetChanged();

                final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                        .getDisplayMetrics());
                pager.setPageMargin(pageMargin);

                tabs.setViewPager(pager);
                tabs.setVisibility(View.VISIBLE);
                actionDivideLine.setVisibility(View.VISIBLE);
            } else {
                showErrorMsg(code, respanse);

                BlankViewDisplay.setBlank(mMembersAllAll.size(), this, false, blankLayout, onClickRetry);
            }

        } else if (tag.equals(HOST_TASK_MEMBER)) {
            if (code == 0) {
                mMemberTask.addItems(respanse.getJSONArray("data"));

                String getMembers = String.format(HOST_MEMBERS, mProjectObject.getId());
                getNetwork(getMembers, HOST_MEMBERS);

            } else {
                hideDialogLoading();
                showErrorMsg(code, respanse);
                BlankViewDisplay.setBlank(mMembersAllAll.size(), this, false, blankLayout, onClickRetry);
            }
        } else if (tag.equals(urlProjectTaskCount)) {
            showLoading(false);
            if (code == 0) {
                TaskProjectCountModel projectTaskCountModel = JSONUtils.getData(respanse.getString("data"), TaskProjectCountModel.class);
                mTaskProjectCountModel.creatorDone = projectTaskCountModel.creatorDone;
                mTaskProjectCountModel.creatorProcessing = projectTaskCountModel.creatorProcessing;
                mTaskProjectCountModel.watcherDone = projectTaskCountModel.watcherDone;
                mTaskProjectCountModel.watcherProcessing = projectTaskCountModel.watcherProcessing;
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(urlALL_Count)) {
            showLoading(false);
            if (code == 0) {
                ProjectTaskCountModel projectTaskCountModel = JSONUtils.getData(respanse.getString("data"), ProjectTaskCountModel.class);
                mTaskProjectCountModel.owner = projectTaskCountModel.done + projectTaskCountModel.processing;
                mTaskProjectCountModel.ownerDone = projectTaskCountModel.done;
                mTaskProjectCountModel.ownerProcessing = projectTaskCountModel.processing;
                mTaskProjectCountModel.creator = projectTaskCountModel.create;
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(urlALL_WATCH_Count)) {
            showLoading(false);
            if (code == 0) {
                mTaskProjectCountModel.watcher = JSONUtils.getJSONLong("totalRow", respanse.getString("data"));
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(urlProjectTaskLabels)) {
            showLoading(false);
            if (code == 0) {
                taskLabelModels = JSONUtils.getList(respanse.getString("data"), TaskLabelModel.class);
                Collections.sort(taskLabelModels, new PinyinComparator());
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(urlALL_Label)) {
            showLoading(false);
            if (code == 0) {
                taskLabelModels = JSONUtils.getList(respanse.getString("data"), TaskLabelModel.class);
                Collections.sort(taskLabelModels, new PinyinComparator());
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(urlSome_Count)) {
            showLoading(false);
            if (code == 0) {
                ProjectTaskUserCountModel item = JSONUtils.getData(respanse.getString("data"), ProjectTaskUserCountModel.class);

                mTaskProjectCountModel.owner = item.memberDone + item.memberProcessing;
                mTaskProjectCountModel.ownerDone = item.memberDone;
                mTaskProjectCountModel.ownerProcessing = item.memberProcessing;

                mTaskProjectCountModel.creatorDone = item.creatorDone;
                mTaskProjectCountModel.creator = item.creatorDone + item.creatorProcessing;
                mTaskProjectCountModel.creatorProcessing = item.creatorProcessing;

                mTaskProjectCountModel.watcher = item.watcherDone + item.watcherProcessing;
                mTaskProjectCountModel.watcherDone = item.watcherDone;
                mTaskProjectCountModel.watcherProcessing = item.watcherProcessing;
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(urlSome_Label)) {
            showLoading(false);
            if (code == 0) {
                taskLabelModels = JSONUtils.getList(respanse.getString("data"), TaskLabelModel.class);
                Collections.sort(taskLabelModels, new PinyinComparator());
            } else {
                showErrorMsg(code, respanse);
            }
        }
        setDrawerData();
    }

    @OnActivityResult(ListModify.RESULT_EDIT_LIST)
    void onResultEditList(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            taskListParentUpdate();
            String globarKey = data.getStringExtra(TaskAddActivity.RESULT_GLOBARKEY);

            TaskObject.Members modifyMember = null;
            for (int i = 0; i < mMembersAllAll.size(); ++i) {
                if (mMembersAllAll.get(i).user.global_key.equals(globarKey)) {
                    modifyMember = mMembersAllAll.get(i);
                    break;
                }
            }

            if (modifyMember != null) {
                if (!mMembersAll.contains(modifyMember)) {
                    mMembersAll.add(modifyMember);
                    adapter.notifyDataSetChanged();
                    tabs.setViewPager(pager);
                    tabs.setVisibility(View.VISIBLE);
                    actionDivideLine.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void taskListParentUpdate() {
        List<WeakReference<Fragment>> fragmentArray = adapter.getFragments();
        for (WeakReference<Fragment> ref : fragmentArray) {
            Fragment item = ref.get();
            if (item instanceof TaskListUpdate) {
                ((TaskListUpdate) item).taskListUpdate(true);
            }
        }
    }

    @Click
    public final void floatButton() {
        TaskObject.Members member = adapter.getItemData(pager.getCurrentItem());

//        Intent intent = new Intent(getActivity(), TaskAddActivity_.class);
        TaskObject.SingleTask task = new TaskObject.SingleTask();
        task.project = mProjectObject;
        task.project_id = mProjectObject.getId();
        task.owner = AccountInfo.loadAccount(getActivity());
        task.owner_id = task.owner.id;

        TaskAddActivity_.intent(this)
                .mSingleTask(task)
                .mUserOwner(member.user)
                .canPickProject(false)
                .startForResult(ListModify.RESULT_EDIT_LIST);
    }

    private static class MemberTaskCount {

        private ArrayList<Count> mData = new ArrayList<>();

        public void addItems(JSONArray jsonArray) throws JSONException {
            for (int i = 0; i < jsonArray.length(); ++i) {
                Count count = new Count(jsonArray.getJSONObject(i));
                mData.add(count);
            }
        }

        public boolean memberHasTask(int id) {
            for (Count item : mData) {
                if (item.user == id) {
                    return true;
                }
            }

            return false;
        }

        static class Count {
            public int done;
            public int processing;
            public int user;

            public Count(JSONObject json) {
                done = json.optInt("done");
                processing = json.optInt("processing");
                user = json.optInt("user");
            }
        }
    }

    public class MyPagerAdapter extends SaveFragmentPagerAdapter implements MyPagerSlidingTabStrip.IconTabProvider {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "";
        }

        @Override
        public int getCount() {
            return mMembersAll.size();
        }

        @Override
        public Fragment getItem(int position) {
            TaskListFragment_ fragment = new TaskListFragment_();
            Bundle bundle = new Bundle();
            bundle.putSerializable("mMembers", mMembersAll.get(position));
            bundle.putSerializable("mProjectObject", mProjectObject);
            bundle.putSerializable("mMembersArray", mUsersInfo);
            bundle.putSerializable("mMemberPos", position);
            bundle.putBoolean("mShowAdd", true);

            bundle.putString("mMeAction", mMeActions[statusIndex]);
            if (mFilterModel != null) {
                bundle.putString("mStatus", mFilterModel.status + "");
                bundle.putString("mLabel", mFilterModel.label);
                bundle.putString("mKeyword", mFilterModel.keyword);
            } else {
                bundle.putString("mStatus", "");
                bundle.putString("mLabel", "");
                bundle.putString("mKeyword", "");
            }
            fragment.setParent(ProjectTaskFragment.this);

            fragment.setArguments(bundle);

            saveFragment(fragment);

            return fragment;
        }

        public TaskObject.Members getItemData(int postion) {
            return mMembersAll.get(postion);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public String getPageIconUrl(int position) {
            return mMembersAll.get(position).user.avatar;
        }
    }

    @OptionsItem
    protected final void action_filter() {
        actionFilter();
    }

    @Override
    protected void sureFilter() {
        super.sureFilter();
        loadAllLabels();
    }
}
