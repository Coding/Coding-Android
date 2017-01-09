package net.coding.program.project;


import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.RequestParams;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerviewViewHolder;
import com.marshalchen.ultimaterecyclerview.quickAdapter.easyRegularAdapter;
import com.readystatesoftware.viewbadger.BadgeView;

import net.coding.program.MyApp;
import net.coding.program.R;
import net.coding.program.common.Global;
import net.coding.program.common.ImageLoadTool;
import net.coding.program.common.TextWatcherAt;
import net.coding.program.common.ui.BaseFragment;
import net.coding.program.common.ui.shadow.RecyclerViewSpace;
import net.coding.program.common.util.PermissionUtil;
import net.coding.program.message.MessageListActivity_;
import net.coding.program.model.AccountInfo;
import net.coding.program.model.ProjectObject;
import net.coding.program.model.UserObject;
import net.coding.program.project.init.InitProUtils;
import net.coding.program.project.init.create.ProjectCreateActivity_;
import net.coding.program.task.add.TaskAddActivity_;
import net.coding.program.user.UsersListActivity;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@EFragment(R.layout.fragment_enterprise_project)
@OptionsMenu(R.menu.enterprise_main_project)
public class EnterpriseProjectFragment extends BaseFragment {

    final String host = Global.HOST_API + "/projects?pageSize=100&type=all&sort=hot";
    private final String URL_PIN_DELETE = Global.HOST_API + "/user/projects/pin?ids=%d";
    private final String URL_PIN_SET = Global.HOST_API + "/user/projects/pin";

    private final int RESULT_SELECT_USER = 2001;

    int msectionId = 0;

    @ViewById
    UltimateRecyclerView listView;

    ArrayList<ProjectObject> listData = new ArrayList<>();

    ProjectAdapter projectAdapter;

    @AfterViews
    void initEnterpriseProjectFramgent() {
        setToolbar("我的项目");

        RecyclerView.LayoutManager manager = new LinearLayoutManager(getActivity());
        listView.setLayoutManager(manager);
        listView.addItemDecoration(new RecyclerViewSpace(getActivity()));
        listView.setEmptyView(R.layout.fragment_enterprise_project_empty,
                R.layout.fragment_enterprise_project_empty);

        listView.setDefaultOnRefreshListener(() -> onRefresh());
        listView.mSwipeRefreshLayout.setColorSchemeResources(R.color.green);
        listView.setOnLoadMoreListener((itemsCount, maxLastVisiblePosition) -> loadMore());

//       上拉加载更多
//        mFootUpdate.initToRecycler(listView, mInflater, this);
//        mFootUpdate.showLoading();
//        listView.setLoadMoreView(mFootUpdate.getView());

        listData = AccountInfo.loadProjects(getActivity());
        projectAdapter = new ProjectAdapter(listData);
        listView.setAdapter(projectAdapter);

        projectActionUtil = new ProjectActionUtil(getActivity().getBaseContext());
        projectActionUtil.setListener(pos -> {
            final int projectId = listData.get(pos).getId();
            if (listData.get(pos).isPin()) {
                String pinDeleteUrl = String.format(URL_PIN_DELETE, projectId);
                deleteNetwork(pinDeleteUrl, URL_PIN_DELETE, -1, projectId);
            } else {
                RequestParams params = new RequestParams();
                params.put("ids", projectId);
                postNetwork(URL_PIN_SET, params, URL_PIN_SET, -1, projectId);
            }

            projectActionUtil.close();
        });
        onRefresh();
    }

    private void onRefresh() {
        getNetwork(host, host);
    }

    private View.OnClickListener clickItem = v -> {
        Object object = v.getTag();
        if (object instanceof ProjectObject) {
            ProjectObject item = (ProjectObject) object;
            ProjectHomeActivity_.intent(this)
                    .mProjectObject(item)
                    .startForResult(InitProUtils.REQUEST_PRO_UPDATE);
        }
    };

    @OnActivityResult(InitProUtils.REQUEST_PRO_UPDATE)
    void onResultRefresh() {
        onRefresh();
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(host)) {
            if (code == 0) {
                listData.clear();
                JSONArray array = respanse.getJSONObject("data").getJSONArray("list");
                int pinCount = 0;
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject item = array.getJSONObject(i);
                    ProjectObject oneData = new ProjectObject(item);
                    if (oneData.isPin()) {
                        listData.add(pinCount++, oneData);
                    } else {
                        listData.add(oneData);
                    }
                }

                msectionId = 0;
                for (ProjectObject item : listData) {
                    if (!item.isPin()) {
                        break;
                    }
                    ++msectionId;
                }


                AccountInfo.saveProjects(getActivity(), listData);
                projectAdapter.notifyDataSetChanged();

            } else {
                projectAdapter.notifyDataSetChanged();
            }
        } else if (tag.equals(URL_PIN_SET)) {
            if (code == 0) {
                int id = (int) data;
                updatePin(id, true);
            } else {
                showErrorMsg(code, respanse);
            }

        } else if (tag.equals(URL_PIN_DELETE)) {
            if (code == 0) {
                int id = (int) data;
                updatePin(id, false);
            } else {
                showErrorMsg(code, respanse);
            }
        }
    }

    private void updatePin(int id, boolean pin) {
        setPin(id, pin);
    }

    public void setPin(int id, boolean pin) {
        for (int i = 0; i < listData.size(); ++i) {
            if (listData.get(i).getId() == id) {
                listData.get(i).setPin(pin);

                ProjectObject item = listData.remove(i);
                if (pin) {
                    ++msectionId;
                    listData.add(0, item);
                } else {
                    --msectionId;
                    listData.add(msectionId, item);
                }
                projectAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void loadMore() {
    }

    @OptionsItem
    void actionCreateProject() {
        ProjectCreateActivity_.intent(this).start();
    }

    @OptionsItem
    void actionCreateTask() {
        TaskAddActivity_.intent(this).mUserOwner(MyApp.sUserObject).start();
    }

    @OptionsItem
    void actionSendMessage() {
        TextWatcherAt.startActivityAt(getActivity(), this, RESULT_SELECT_USER);
    }

    @OnActivityResult(RESULT_SELECT_USER)
    void onSelectUser(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            UserObject user = (UserObject) data.getSerializableExtra(UsersListActivity.RESULT_EXTRA_USESR);
            if (user != null) {
                MessageListActivity_.intent(this).mUserObject(user).start();
            }
        }
    }
    @OptionsItem
    void action2fa() {
        if (!PermissionUtil.checkCamera(getActivity())) {
            return;
        }

        Global.start2FAActivity(getActivity());
    }

    protected class ProjectAdapter extends easyRegularAdapter<ProjectObject, ProjectHolder> {

        public ProjectAdapter(List<ProjectObject> list) {
            super(list);
        }

        @Override
        protected int getNormalLayoutResId() {
            return R.layout.enterprise_project_all_list_item;
        }

        @Override
        protected ProjectHolder newViewHolder(View view) {
            view.setOnClickListener(clickItem);
            return new ProjectHolder(view);
        }

        @Override
        protected void withBindHolder(ProjectHolder holder, ProjectObject data, int position) {
            ProjectObject item = getItem(position);

            holder.privatePin.setVisibility(item.isPin() ? View.VISIBLE : View.INVISIBLE);
            String ownerName = item.owner_user_name;

            holder.name2.setVisibility(View.VISIBLE);
            holder.name2.setText(item.name);
            holder.name.setVisibility(View.INVISIBLE);

            holder.desc.setText(item.getDescription());
            setClickEvent(holder.fLayoutAction, position);
            int count = item.un_read_activities_count;
            BadgeView badge = holder.badge;
            Global.setBadgeView(badge, count);

            holder.fLayoutAction.setVisibility(View.VISIBLE);
            holder.rootLayout.setTag(item);
            iconfromNetwork(holder.image, item.icon, ImageLoadTool.optionsRounded2);
        }

        @Override
        public ProjectHolder newHeaderHolder(View view) {
            return new ProjectHolder(view, true);
        }

        @Override
        public ProjectHolder newFooterHolder(View view) {
            return new ProjectHolder(view, true);
        }
    }

    private ProjectActionUtil projectActionUtil;

    private void setClickEvent(final View fLayoutAction, final int position) {
        fLayoutAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                projectActionUtil.show(fLayoutAction, position);
                if (!listData.get(position).isPin()) {
                    projectActionUtil.getTxtSetting().setText("设为常用");
                    projectActionUtil.getTxtSetting().setTextColor(getActivity().getResources().getColor(R.color.white));
                    projectActionUtil.getTxtSetting().setBackgroundColor(getActivity().getResources().getColor(R.color.color_3BBD79));
                } else {
                    projectActionUtil.getTxtSetting().setText("取消常用");
                    projectActionUtil.getTxtSetting().setTextColor(getActivity().getResources().getColor(R.color.color_3BBD79));
                    projectActionUtil.getTxtSetting().setBackgroundColor(getActivity().getResources().getColor(R.color.color_E5E5E5));
                }
            }
        });
    }

    class ProjectHolder extends UltimateRecyclerviewViewHolder {

        TextView name;
        ImageView image;
        TextView name2;
        TextView desc;
        BadgeView badge;
        ImageView privatePin;
        View fLayoutAction;
        View rootLayout;

        ProjectHolder(View view, boolean isHeader) {
            super(view);
        }

        public ProjectHolder(View view) {
            super(view);
            rootLayout = view;
            name = (TextView) view.findViewById(R.id.name);
            image = (ImageView) view.findViewById(R.id.icon);
            badge = (BadgeView) view.findViewById(R.id.badge);
            fLayoutAction = view.findViewById(R.id.flayoutAction);
            desc = (TextView) view.findViewById(R.id.txtDesc);
            privatePin = (ImageView) view.findViewById(R.id.privatePin);
            name2 = (TextView) view.findViewById(R.id.name2);
        }
    }
}
