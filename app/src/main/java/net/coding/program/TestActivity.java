package net.coding.program;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import net.coding.program.common.Global;
import net.coding.program.common.htmltext.URLSpanNoUnderline;
import net.coding.program.login.auth.QRScanActivity;
import net.coding.program.project.detail.PickLabelColorActivity_;


public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

    }

    public void click1(View v) {
//        test1();
//        PopCaptchaDialog.pop(this);

        String url = Global.HOST + "/user/tasks?owner=22";
        URLSpanNoUnderline.openActivityByUri(this, url, true);
    }

    public void click2(View v) {
        Intent intent = new Intent(this, QRScanActivity.class);
//        intent.putExtra(QRScanActivity.EXTRA_OPEN_URL, "true");
        startActivity(intent);
    }

    public void click3(View v) {
//        EntranceActivity_.intent(this).start();
//        MainActivity_.intent(this).start();
    }

    int mColor = 0xffff5722;

    void test1() {
        PickLabelColorActivity_.intent(this)
                .generateColor(mColor)
                .startForResult(1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            int color = data.getIntExtra("resultData", 0);
            Toast.makeText(TestActivity.this, "color" + color, Toast.LENGTH_SHORT).show();
            mColor = color;
        }
    }


    public void clickMain() {
        Intent intent = new Intent(TestActivity.this, EntranceActivity_.class);
        startActivity(intent);
    }
}
