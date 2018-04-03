package com.zl.pdfdemo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.shockwave.pdfium.PdfDocument;
import com.zl.android_pdf_view.PDFView;
import com.zl.android_pdf_view.listener.OnLoadCompleteListener;
import com.zl.android_pdf_view.listener.OnPageChangeListener;
import com.zl.android_pdf_view.listener.OnPageErrorListener;
import com.zl.android_pdf_view.scroll.DefaultScrollHandle;
import com.zl.android_pdf_view.util.FitPolicy;
import com.zl.pdfdemo.utils.SPUtil;
import com.zl.pdfdemo.view.SelfDialog;

import java.util.List;

import static com.zl.pdfdemo.view.SelfDialog.*;

public class MainActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener {
    private PDFView pdfView;
    private TextView pageTv, pageTv1;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int PERMISSION_CODE = 42042;
    public static final int REQUEST_CODE = 42;

    public static final String SAMPLE_FILE = "kotlin-docs.pdf";
    public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    Integer pageNumber = 0;
    String pdfFileName;
    Uri uri;
    private int myPage;
    private int p;
    private SelfDialog selfDialog;
    private int pageCount;//pdf文件的总页数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pdfView = findViewById(R.id.pdfView);
        pageTv = findViewById(R.id.pageTv);
        pageTv1 = findViewById(R.id.pageTv1);
        myPage = (int) SPUtil.get(MainActivity.this, "page", 0);
        afterViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //填充选项菜单（读取XML文件、解析、加载到Menu组件上）
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pickFile:
                int permissionCheck = ContextCompat.checkSelfPermission(this,
                        READ_EXTERNAL_STORAGE);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{READ_EXTERNAL_STORAGE},
                            PERMISSION_CODE
                    );
                } else {
                    launchPicker();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            uri = intent.getData();
            displayFromUri(uri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchPicker();
            }
        }
    }

    void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            //alert user that file manager not working
            Toast.makeText(this, R.string.toast_pick_file_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void afterViews() {
        pdfView.setBackgroundColor(Color.LTGRAY);
        if (uri != null) {
            displayFromUri(uri);
        } else {
            displayFromAsset(SAMPLE_FILE);
        }
        setTitle(pdfFileName);
    }

    private void displayFromAsset(String assetFileName) {
        pdfFileName = assetFileName;

        pdfView.fromAsset(SAMPLE_FILE)
//                .pages(0, 2, 3, 4, 5); // 把0 , 2 , 3 , 4 , 5 过滤掉
                //是否允许翻页，默认是允许翻页
                .enableSwipe(true)
                //pdf文档翻页是否是垂直翻页，默认是左右滑动翻页
                .swipeHorizontal(true)
                //
                .enableDoubletap(false)
                //设置默认显示第0页
                .defaultPage(myPage)
                //允许在当前页面上绘制一些内容，通常在屏幕中间可见。
//                .onDraw(onDrawListener)
//                // 允许在每一页上单独绘制一个页面。只调用可见页面
//                .onDrawAll(onDrawListener)
                //设置加载监听
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        pageTv.setText(nbPages + "");
                        pageTv1.setText(myPage + "/");
                    }
                })
                //设置翻页监听
                .onPageChange(this)
                //设置页面滑动监听
//                .onPageScroll(onPageScrollListener)
//                .onError(onErrorListener)
                // 首次提交文档后调用。
//                .onRender(onRenderListener)
                // 渲染风格（就像注释，颜色或表单）
                .enableAnnotationRendering(true)
                .password(null)
                .scrollHandle(new DefaultScrollHandle(this))
                // 改善低分辨率屏幕上的渲染
                .enableAntialiasing(true)
                // 页面间的间距。定义间距颜色，设置背景视图
                .spacing(10)
                .onPageError(this)
                .pageFitPolicy(FitPolicy.BOTH)
                .load();
    }

    private void displayFromUri(Uri uri) {
        pdfFileName = getFileName(uri);

        pdfView.fromUri(uri)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .spacing(10) // in dp
                .onPageError(this)
                .load();
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }


    @Override
    public void onPageError(int page, Throwable t) {
        Log.e(TAG, "Cannot load page " + page);
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    @Override
    public void onPageChanged(int page, final int pageCount) {
        this.pageCount = pageCount;
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));

        Snackbar snackBar = Snackbar.make(pdfView, "it is snackbar!", Snackbar.LENGTH_SHORT);
        //设置SnackBar背景颜色
        snackBar.getView().setBackgroundResource(R.color.bg_snackbar);
        //设置按钮文字颜色
        snackBar.setActionTextColor(Color.DKGRAY);
        //设置点击事件
        snackBar.setAction("点击", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selfDialog = new SelfDialog(MainActivity.this);
                selfDialog.setTitle("跳转");
                selfDialog.setJumpOnclickListener(pageCount,new onJumpOnclickListener() {
                    @Override
                    public void onJumpClick(int page) {
                        pdfView.jumpTo(page-1);
                        selfDialog.dismiss();
                    }
                });
                selfDialog.setNoOnclickListener("取消", new onNoOnclickListener() {
                    @Override
                    public void onNoClick() {
                        selfDialog.dismiss();
                    }
                });
                selfDialog.show();

            }
        });
        //设置回调
        snackBar.setCallback(new Snackbar.Callback() {

            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
//                Toast.makeText(MainActivity.this, "Snackbar dismiss", Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onShown(Snackbar snackbar) {
                super.onShown(snackbar);
//                Toast.makeText(MainActivity.this, "Snackbar show", Toast.LENGTH_SHORT).show();
            }
        }).show();

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SPUtil.put(this, "page", p);
    }

}
