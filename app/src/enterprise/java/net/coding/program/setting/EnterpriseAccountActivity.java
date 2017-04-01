package net.coding.program.setting;

import android.app.Activity;
import android.os.Build;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import net.coding.program.R;
import net.coding.program.common.Global;
import net.coding.program.common.ImageLoadTool;
import net.coding.program.common.base.MyJsonResponse;
import net.coding.program.common.network.MyAsyncHttpClient;
import net.coding.program.common.ui.BackActivity;
import net.coding.program.common.widget.CircleImageView;
import net.coding.program.model.EnterpriseAccount;
import net.coding.program.model.EnterpriseInfo;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.json.JSONObject;

@EActivity(R.layout.activity_enterprise_account)
@OptionsMenu(R.menu.enterprise_account_menu)
public class EnterpriseAccountActivity extends BackActivity {

    private static final int SETTING_REQUEST_CODE = 1000;

    @ViewById
    TextView accountState;
    @ViewById
    CircleImageView companyIcon;
    @ViewById
    TextView companyName;
    @ViewById
    Toolbar toolbar;

    @ColorRes(R.color.font_red)
    int fontRed;

    @ColorRes(R.color.font_orange)
    int fontOragne;

    @AfterViews
    void initEnterpriseAccountActivity() {
        initToolbar();
        updateUI();

        String host = String.format("%s/enterprise/%s", Global.HOST_API, EnterpriseInfo.instance().getGlobalkey());
        MyAsyncHttpClient.get(this, host, new MyJsonResponse(this) {
            @Override
            public void onMySuccess(JSONObject response) {
                super.onMySuccess(response);

                EnterpriseAccount account = new EnterpriseAccount(response.optJSONObject("data"));
                Spanned countString;

                if (account.trial) { // 处于试用期
                    int fontColor = account.remaindays > 5 ? fontOragne : fontRed;
                    countString = Global.createColorHtml("试用期剩余 ", String.valueOf(account.remaindays), " 天", fontColor);
                } else {
                    if (account.payed) {
                        if (account.remaindays > 5) { // 付费期且未到期
                            countString = Global.createColorHtml("账户余额：", account.balance, "元", fontOragne);
                        } else if (account.remaindays > 0) {
                            countString = Global.createColorHtml("", "您的余额不足，请尽快订购", "", fontRed);
                        } else { // 付费期已到期
                            String tip;
                            if (account.suspendedAt > 0) { // 已暂停
                                tip = String.format("您的服务已暂停 %s 天，请订购后使用", account.suspendedToToday());
                            } else { // 处于超时使用阶段
                                tip = String.format("您的服务已超时使用 %s 天，请订购后使用", account.estimateDate());
                            }
                            countString = Global.createColorHtml("", tip, "", fontRed);
                        }
                    } else { // 未付费而且试用期已过
                        countString = Global.createColorHtml("", "您的试用期已结束，请订购后使用", "", fontRed);
                    }
                }

                accountState.setText(countString);
            }

            @Override
            public void onMyFailure(JSONObject response) {
                super.onMyFailure(response);
            }
        });
    }

    private void updateUI() {
        ImageLoader.getInstance().displayImage(EnterpriseInfo.instance().getAvatar(), companyIcon, ImageLoadTool.options);
        companyName.setText(EnterpriseInfo.instance().getName());
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        if (Build.VERSION.SDK_INT >= 21) {
            getSupportActionBar().setElevation(0);
        }
    }

    @Click
    void itemProject() {
        ManageProjectListActivity_.intent(this).start();
    }

    @Click
    void itemMember() {
        ManageMemberActivity_.intent(this).start();
    }

    @OptionsItem
    void action_setting() {
        EnterpriseSettingActivity_.intent(this).startForResult(SETTING_REQUEST_CODE);
    }

    @OnActivityResult(SETTING_REQUEST_CODE)
    void OnSettingResult(int result) {
        if (result == Activity.RESULT_OK) {
            updateUI();
        }
    }
}